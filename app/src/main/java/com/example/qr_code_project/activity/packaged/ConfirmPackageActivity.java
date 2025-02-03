package com.example.qr_code_project.activity.packaged;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.modal.ProductModal;

import java.util.HashMap;
import java.util.Map;

public class ConfirmPackageActivity extends AppCompatActivity {

    private EditText barcodeProductPackageEt, nameProductPackageEt
            , quantityProductPackageEt, realQuantityProductPackageEt;
    private Button confirmProductPackageBtn;
    private ImageView productBarcodePackageStatusIcon ;
    private TextView productBarcodePackageStatusText ;

    private QRcodeManager qrCodeManager;
    private String scannedProductBarcode = "";

    private ProductModal productModal;
    private Map<Integer, Object> realQuantitiesMap;
    private boolean isConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_package);

        util();

        // Get data from PackageActivity
//        ProductModal productModal = (ProductModal) getIntent().getSerializableExtra("product");
//        Map<Integer, Integer> realQuantitiesMap =
//                (Map<Integer, Integer>) getIntent().getSerializableExtra("realQuantitiesMap");
        Object productObj = getIntent().getSerializableExtra("product");
        if (productObj instanceof ProductModal) {
            productModal = (ProductModal) productObj;
        } else {
            Toast.makeText(this, "Invalid product data!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Object realQuantitiesObj = getIntent().getSerializableExtra("realQuantitiesMap");
        if (realQuantitiesObj instanceof Map) {
            realQuantitiesMap = (Map<Integer, Object>) realQuantitiesObj;
        } else {
            realQuantitiesMap = new HashMap<>();
        }


        // Show product information
        barcodeProductPackageEt.setText(productModal.getCode());
        nameProductPackageEt.setText(productModal.getTitle());
        quantityProductPackageEt.setText(String.valueOf(productModal.getQuantity()));

        // Create QRCodeManager
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);

        // Process when click submit
        Map<Integer, Object> finalRealQuantitiesMap = realQuantitiesMap;
        confirmProductPackageBtn.setOnClickListener(v -> handleConfirmation(productModal, finalRealQuantitiesMap));

    }
    //Check real quantity
    private void handleConfirmation(ProductModal productModal, Map<Integer, Object> realQuantitiesMap) {
        String orderQuantityStr = quantityProductPackageEt.getText().toString().trim();
        String realQuantityStr = realQuantityProductPackageEt.getText().toString().trim();

        if (realQuantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter actual quantity!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int orderQuantity = Integer.parseInt(orderQuantityStr);
            int actualQuantity = Integer.parseInt(realQuantityStr);

            if (actualQuantity != orderQuantity) {
                showConfirmationDialog(orderQuantity, actualQuantity, productModal, realQuantitiesMap);
            } else {
                confirmProduct(productModal, realQuantitiesMap, actualQuantity);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity! Please enter a valid number.", Toast.LENGTH_SHORT).show();
        }

    }

    //Show warning when quantity and real quantity don't same
    private void showConfirmationDialog(int orderQuantity, int actualQuantity,
                                        ProductModal productModal,
                                        Map<Integer, Object> realQuantitiesMap) {
        if (!isFinishing()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("The actual quantity (" + actualQuantity +
                            ") does not match the order quantity (" + orderQuantity +
                            "). Do you still want to confirm?")
                    .setPositiveButton("Yes", (dialog, which) -> confirmProduct(productModal, realQuantitiesMap, actualQuantity))
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                        realQuantityProductPackageEt.setText("");
                    })
                    .show();
        }

    }

    //Confirm product
    private void confirmProduct(ProductModal productModal, Map<Integer, Object> realQuantitiesMap, int actualQuantity) {
        confirmProductPackageBtn.setEnabled(false);

        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("actualQuantity", actualQuantity);
        productInfo.put("code", 0);
        productInfo.put("areaId", 0);
        productInfo.put("location", 0);

        realQuantitiesMap.put(productModal.getId(), productInfo);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("realQuantitiesMap", new HashMap<>(realQuantitiesMap));
        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Product confirmed successfully!", Toast.LENGTH_SHORT).show();
    }

    //Check scan product and warehouse
    private void handleScannedData(String qrCodeText) {

        scannedProductBarcode = qrCodeText;
        if (scannedProductBarcode != null && scannedProductBarcode.equals(barcodeProductPackageEt
                .getText().toString().trim())) {
            updateProductScanStatus(true, "Product barcode is valid.");
            confirmProductPackageBtn.setEnabled(true);
            confirmProductPackageBtn.setText("Confirm Product");
        } else {
            updateProductScanStatus(false, "Invalid product barcode! Please scan again.");
            scannedProductBarcode = "";
        }
    }
    private void updateProductScanStatus(boolean isValid, String message) {
        productBarcodePackageStatusIcon.setVisibility(View.VISIBLE);
        productBarcodePackageStatusIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24 : R.drawable.baseline_cancel_24);
        productBarcodePackageStatusText.setVisibility(View.VISIBLE);
        productBarcodePackageStatusText.setText(message);
        productBarcodePackageStatusText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    //UI component
    private void util(){
        barcodeProductPackageEt = findViewById(R.id.barcodeProductPackageEt);
        nameProductPackageEt = findViewById(R.id.nameProductPackageEt);
        quantityProductPackageEt = findViewById(R.id.quantityProductPackageEt);
        realQuantityProductPackageEt = findViewById(R.id.realQuantityProductPackageEt);
        confirmProductPackageBtn = findViewById(R.id.confirmProductPackageBtn);
        productBarcodePackageStatusIcon = findViewById(R.id.productBarcodePackageStatusIcon);
        productBarcodePackageStatusText = findViewById(R.id.productBarcodePackageStatusText);

        confirmProductPackageBtn.setEnabled(false);
        confirmProductPackageBtn.setText("Scan barcode to enable");

    }
}