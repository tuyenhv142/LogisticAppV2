package com.example.qr_code_project.activity.outbound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.modal.ProductModal;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfirmOutboundActivity extends AppCompatActivity {

    private EditText barcodeOutboundEt, nameOutboundEt, quantityOutboundEt, realQuantityOutboundEt
            , warehouseCodeOutboundEt;
    private Button confirmOutboundBtn;
//    private ImageView productBarcodeStatusOutboundIcon, warehouseBarcodeStatusOutboundIcon;
    private TextView productBarcodeStatusOutboundText, warehouseBarcodeStatusOutboundText;

    private QRcodeManager qrCodeManager;
    private String scannedProductBarcode = "";
    private String scannedWarehouseBarcode = "";
//    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private String code;
    private int areaId;
    private int location;
    private LoadingDialog loadingDialog;
//    private TokenManager tokenManager;

    private boolean isConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_outbound);

        initViews();

        setupQRManager();

        getDateFromIntent();
    }

    private void getDateFromIntent() {
        ProductModal productModal = (ProductModal) getIntent().getSerializableExtra("product");
        if (productModal == null) {
            Toast.makeText(this, getString(R.string.missing), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        barcodeOutboundEt.setText(productModal.getCode());
        nameOutboundEt.setText(productModal.getTitle());
        quantityOutboundEt.setText(String.valueOf(productModal.getQuantity()));
        warehouseCodeOutboundEt.setText(String.valueOf(productModal.getLocation()));

        Map<Integer, Object>  productMap =
                (Map<Integer, Object> ) getIntent().getSerializableExtra("productMap");
        if (productMap == null) {
            productMap = new HashMap<>();
        }

        Map<Integer, Object>  finalProductMap = productMap;

        confirmOutboundBtn.setOnClickListener(v -> handleConfirmation(productModal, finalProductMap));
    }

    // Initialize views and shared preferences
    private void initViews() {
        barcodeOutboundEt = findViewById(R.id.barcodeOutboundEt);
        nameOutboundEt = findViewById(R.id.nameOutboundEt);
        quantityOutboundEt = findViewById(R.id.quantityOutboundEt);
        realQuantityOutboundEt = findViewById(R.id.realQuantityOutboundEt);
        warehouseCodeOutboundEt = findViewById(R.id.warehouseCodeOutboundEt);
        confirmOutboundBtn = findViewById(R.id.confirmOutboundBtn);
        confirmOutboundBtn.setVisibility(View.GONE);
//        productBarcodeStatusOutboundIcon = findViewById(R.id.productBarcodeStatusOutboundIcon);
//        warehouseBarcodeStatusOutboundIcon = findViewById(R.id.warehouseBarcodeStatusOutboundIcon);
        productBarcodeStatusOutboundText = findViewById(R.id.productBarcodeStatusOutboundText);
        warehouseBarcodeStatusOutboundText = findViewById(R.id.warehouseBarcodeStatusOutboundText);

//        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        loadingDialog = new LoadingDialog(this);
//        tokenManager = new TokenManager(this);
    }

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
    }

    private void handleConfirmation(ProductModal productModal, Map<Integer, Object> productMap) {
        String orderQuantityStr = quantityOutboundEt.getText().toString().trim();
        String realQuantityStr = realQuantityOutboundEt.getText().toString().trim();

        if (realQuantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.real_quantity_outbout), Toast.LENGTH_SHORT).show();
            return;
        }

        int orderQuantity = Integer.parseInt(orderQuantityStr);
        int actualQuantity = Integer.parseInt(realQuantityStr);

        if (actualQuantity != orderQuantity) {
            showConfirmationDialog(orderQuantity, actualQuantity, productModal, productMap);
        } else {
            confirmProduct(productModal, productMap, actualQuantity);
        }
    }

    private void handleScannedData(String qrCodeText) {
        if (isConfirmed) return;

        if (scannedProductBarcode.isEmpty()) {
            scannedProductBarcode = qrCodeText;
            if (scannedProductBarcode.equals(barcodeOutboundEt.getText().toString().trim())) {
                updateProductScanStatus(true, getString(R.string.product_barcode_valid));
//                fetchWarehouseLocation(scannedProductBarcode);
            } else {
                updateProductScanStatus(false, getString(R.string.invalid_product_barcode));
                scannedProductBarcode = "";
            }
        } else if (scannedWarehouseBarcode.isEmpty()) {
            scannedWarehouseBarcode = qrCodeText;
            if (scannedWarehouseBarcode.equals(warehouseCodeOutboundEt.getText().toString().trim())) {
                updateWarehouseScanStatus(true, getString(R.string.warehouse_barcode_valid));
                confirmOutboundBtn.setVisibility(View.VISIBLE);
                if (qrCodeManager != null) {
                    qrCodeManager.unregister();
                    qrCodeManager = null;
                }
            } else {
                updateWarehouseScanStatus(false, getString(R.string.invalid_warehouse_barcode));
                scannedWarehouseBarcode = "";
            }
        }
    }

    private void fetchWarehouseLocation(String code) {
        String url = ApiConstants.getFindOneCodeProductUrl(code);
        loadingDialog.show();
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
                this::handleError
        );
//        {
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (!tokenManager.isTokenExpired()) {
//                    headers.put("Authorization", "Bearer " + token);
//                }else {
//                    tokenManager.clearTokenAndLogout();
//                }
//                headers.put("Content-Type", "application/json");
//                return headers;
//            }
//        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    populateContent(content);
                }
            } else {
                Log.d("error","Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this,getString(R.string.response_fail),Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void populateContent(JSONObject content) throws JSONException {
        int requiredQuantity = Integer.parseInt(quantityOutboundEt.getText().toString().trim());
        JSONArray warehouseArray = content.optJSONArray("listAreaOfproducts");

        if (warehouseArray == null || warehouseArray.length() == 0) {
            Toast.makeText(this, getString(R.string.no_warehouse), Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < warehouseArray.length(); i++) {
            JSONObject warehouse = warehouseArray.getJSONObject(i);
            int warehouseQuantity = warehouse.optInt("quantity", 0); // Lấy số lượng hàng trong kho

            if (warehouseQuantity >= requiredQuantity) {
                code = warehouse.optString("code", "N/A");
                areaId = warehouse.optInt("idShelf", 0);
                location = warehouse.optInt("location", 0);

                warehouseCodeOutboundEt.setText(code);
                return;
            }
        }

        Toast.makeText(this, getString(R.string.no_warehouse), Toast.LENGTH_SHORT).show();
    }


    private void confirmProduct(ProductModal productModal, Map<Integer, Object>  productMap, int actualQuantity) {
        isConfirmed = true;
        confirmOutboundBtn.setEnabled(false);

        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("actualQuantity", actualQuantity);
        productInfo.put("code", code);
        productInfo.put("areaId", areaId);
        productInfo.put("location", location);

        productMap.put(productModal.getId(), productInfo);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("productMap", new HashMap<>(productMap));
        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, getString(R.string.product_confirmed_successfully), Toast.LENGTH_SHORT).show();
    }

    private void showConfirmationDialog(int orderQuantity, int actualQuantity, ProductModal productModal,
                                        Map<Integer, Object>  productMap) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.order_quantity_mismatch))
                .setPositiveButton(getString(R.string.yes), (dialog, which) ->
                        confirmProduct(productModal, productMap, actualQuantity))
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateProductScanStatus(boolean isValid, String message) {
//        productBarcodeStatusOutboundIcon.setVisibility(View.VISIBLE);
//        productBarcodeStatusOutboundIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24
//                : R.drawable.baseline_cancel_24);
        productBarcodeStatusOutboundText.setVisibility(View.VISIBLE);
        productBarcodeStatusOutboundText.setText(message);
        productBarcodeStatusOutboundText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateWarehouseScanStatus(boolean isValid, String message) {
//        warehouseBarcodeStatusOutboundIcon.setVisibility(View.VISIBLE);
//        warehouseBarcodeStatusOutboundIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24
//                : R.drawable.baseline_cancel_24);
        warehouseBarcodeStatusOutboundText.setVisibility(View.VISIBLE);
        warehouseBarcodeStatusOutboundText.setText(message);
        warehouseBarcodeStatusOutboundText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void handleError(Throwable error) {
        String errorMsg = getString(R.string.error_parse);
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = getString(R.string.error_timeout);
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = getString(R.string.error_no_connection);
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }
}
