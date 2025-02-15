package com.example.qr_code_project.activity.swap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DetailSwapLocationActivity extends AppCompatActivity {

    private EditText productName1,productQuantity1,productCode1;
    private EditText productName2,productQuantity2,productCode2;
    private TextView locationOld1,productCodeTv, productQuantityTv,productNameTv;
    private TextView locationNewCode2,productCodeNewTv, productQuantityNewTv,productNameNewTv;
    private ImageView checkedNewImg,checkedOldImg;
    private Button continueConfirmProductBtn;
    private String scannedLocation1 = "";
    private String scannedProductLocation1 = "";
    private String scannedLocation2= "";
    private String scannedProductLocation2 = "";
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private QRcodeManager qrCodeManager;
    private boolean isConfirmed = false;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;
    private AlertDialog productDialog;
    private boolean isLocation1Confirmed = false;
    private boolean isLocation2Confirmed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_swap_location);

        util();

        Integer swapId = getSwapIdFromIntent();
        if (swapId == null) return;

        loadSwapLocation(swapId);
        setupQRManager();

        continueConfirmProductBtn.setOnClickListener(v -> onConfirmSwap(swapId));

    }

    private @Nullable Integer getSwapIdFromIntent() {
        int swapId = getIntent().getIntExtra("swapId", 0);

        if (swapId == 0) {
            Toast.makeText(this, getString(R.string.swapId_empty), Toast.LENGTH_SHORT).show();
            return null;
        }
        return swapId;
    }

    private void onConfirmSwap(int swapId) {

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save swapped product data in SharedPreferences
        editor.putString("product1_location", locationNewCode2.getText().toString().trim());
        editor.putString("product2_location", locationOld1.getText().toString().trim());
        editor.putInt("swap_location_id", swapId);

        editor.apply(); // Commit changes

        Toast.makeText(this, getString(R.string.swap_confirmed), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(DetailSwapLocationActivity.this, ConfirmSwapLocationActivity.class);
        startActivity(intent);
        finish();
    }

    //Get data Inbound from Api
    private void loadSwapLocation(int swapId) {
        loadingDialog.show();
        String url = ApiConstants.getFindOneCodeSwapLocationUrl(swapId);
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::parseResponseLocation,
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (!tokenManager.isTokenExpired()) {
                    headers.put("Authorization", "Bearer " + token);
                }else {
                    tokenManager.clearTokenAndLogout();
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    //Data from Api
    private void parseResponseLocation(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    String codeLocationOld = content.optString("localtionOldCode", "N/A");
                    String codeLocationNew = content.optString("localtionNewCode", "N/A");
                    locationOld1.setText(codeLocationOld);
                    locationNewCode2.setText(codeLocationNew);
                    fetchProductLocation(codeLocationOld,true);
                    fetchProductLocation(codeLocationNew,false);
                }
            } else {
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("SwapLocation", "Failed to parse JSON response", e);
            showError(getString(R.string.login_fail));
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void util(){
        locationOld1 = findViewById(R.id.locationOld1);
        continueConfirmProductBtn = findViewById(R.id.continueConfirmProductBtn);
        productName1 = findViewById(R.id.productName1);
        productQuantity1 = findViewById(R.id.productQuantity1);
        locationNewCode2 = findViewById(R.id.locationNewCode2);
        productName2 = findViewById(R.id.productName2);
        productQuantity2 = findViewById(R.id.productQuantity2);
        productCode2 = findViewById(R.id.productCode2);
        productCode1 = findViewById(R.id.productCode1);

        productCodeTv = findViewById(R.id.productCodeTv);
        productNameTv = findViewById(R.id.productNameTv);
        productQuantityTv = findViewById(R.id.productQuantityTv);
        checkedOldImg = findViewById(R.id.checkedOldImg);

        productCodeNewTv = findViewById(R.id.productCodeNewTv);
        productNameNewTv = findViewById(R.id.productNameNewTv);
        productQuantityNewTv = findViewById(R.id.productQuantityNewTv);
        checkedNewImg = findViewById(R.id.checkedNewImg);

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
        continueConfirmProductBtn.setEnabled(false);
    }

    //Show error
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showProductDialog(String location, String productCode, String productName, String quantity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_product_dialog, null);
        builder.setView(dialogView);

        TextView tvProductLocation = dialogView.findViewById(R.id.tvProductLocation);
        TextView tvProductCode = dialogView.findViewById(R.id.tvProductCode);
        TextView tvProductName = dialogView.findViewById(R.id.tvProductName);
        TextView tvProductQuantity = dialogView.findViewById(R.id.tvProductQuantity);

        tvProductLocation.setText(location);
        tvProductCode.setText("產品代碼: " + productCode);
        tvProductName.setText("產品名稱: " + productName);
        tvProductQuantity.setText("產品數量: " + quantity);

        productDialog = builder.create();
        Objects.requireNonNull(productDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        productDialog.setCancelable(false);
        productDialog.show();
    }

    private void handleScannedData(String qrCodeText) {
        if (isConfirmed) return;

        if (scannedLocation1.isEmpty()) {
            scannedLocation1 = qrCodeText;
            if (scannedLocation1.equals(locationOld1.getText().toString().trim())) {
                String productName = productName1.getText().toString().trim();
                String productQuantity = productQuantity1.getText().toString().trim();
                String productCode = productCode1.getText().toString().trim();
                showProductDialog(scannedLocation1, productCode, productName, productQuantity);
                Toast.makeText(this, R.string.location_correct, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.scan_again_location, Toast.LENGTH_SHORT).show();
                scannedLocation1 = "";
            }
        } else if (scannedProductLocation1.isEmpty()) {
            scannedProductLocation1 = qrCodeText;
            if (scannedProductLocation1.equals(productCode1.getText().toString().trim())) {
                updateProductLocation1ScanStatus();
                Toast.makeText(this, R.string.product_correct, Toast.LENGTH_SHORT).show();
                if (productDialog != null && productDialog.isShowing()) {
                    productDialog.dismiss();
                }
            } else {
                Toast.makeText(this, R.string.invalid_product, Toast.LENGTH_SHORT).show();
                scannedProductLocation1 = "";
            }
        } else if (scannedLocation2.isEmpty()) {
            scannedLocation2 = qrCodeText;
            if (scannedLocation2.equals(locationNewCode2.getText().toString().trim())) {
                String productName = productName2.getText().toString().trim();
                String productQuantity = productQuantity2.getText().toString().trim();
                String productCode = productCode2.getText().toString().trim();
                showProductDialog(scannedLocation2, productCode, productName, productQuantity);
                Toast.makeText(this, R.string.location_correct, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.scan_again_location, Toast.LENGTH_SHORT).show();
                scannedLocation2 = "";
            }
        } else if (scannedProductLocation2.isEmpty()) {
            scannedProductLocation2 = qrCodeText;
            if (scannedProductLocation2.equals(productCode2.getText().toString().trim())) {
                updateProductLocation2ScanStatus();
                Toast.makeText(this, R.string.product_correct, Toast.LENGTH_SHORT).show();
                if (productDialog != null && productDialog.isShowing()) {
                    productDialog.dismiss();
                }
                // Check if position 1 is scanned and continue
                if (!scannedProductLocation1.isEmpty() && !scannedProductLocation2.isEmpty()) {
//                    continueConfirmProductBtn.setEnabled(true);
                    if (qrCodeManager != null) {
                        qrCodeManager.unregister();
                        qrCodeManager = null;
                    }
                }
            } else {
                Toast.makeText(this, R.string.invalid_product, Toast.LENGTH_SHORT).show();
                scannedProductLocation2 = "";
            }
        }

        // Ensure that continue button only gets enabled when both positions are scanned
//        if (!scannedProductLocation1.isEmpty() && !scannedProductLocation2.isEmpty()) {
//            continueConfirmProductBtn.setEnabled(true);
//        }
    }


    private void updateProductLocation1ScanStatus() {
        locationOld1.setTextColor(Color.BLACK);
        productNameTv.setTextColor(Color.BLACK);
        productName1.setTextColor(Color.BLACK);
        productCodeTv.setTextColor(Color.BLACK);
        productCode1.setTextColor(Color.BLACK);
        productQuantityTv.setTextColor(Color.BLACK);
        productQuantity1.setTextColor(Color.BLACK);
        checkedOldImg.setImageTintList(ColorStateList.valueOf(Color.GREEN));

        isLocation1Confirmed = true;
        checkIfBothLocationsConfirmed();
    }

    private void updateProductLocation2ScanStatus() {
        locationNewCode2.setTextColor(Color.BLACK);
        productNameNewTv.setTextColor(Color.BLACK);
        productName2.setTextColor(Color.BLACK);
        productCodeNewTv.setTextColor(Color.BLACK);
        productCode2.setTextColor(Color.BLACK);
        productQuantityNewTv.setTextColor(Color.BLACK);
        productQuantity2.setTextColor(Color.BLACK);
        checkedNewImg.setImageTintList(ColorStateList.valueOf(Color.GREEN));

        isLocation2Confirmed = true;
        checkIfBothLocationsConfirmed();
    }

    private void checkIfBothLocationsConfirmed() {
        if (isLocation1Confirmed && isLocation2Confirmed) {
            continueConfirmProductBtn.setEnabled(true);
        }
    }

    private void fetchProductLocation(String code, boolean isOldLocation) {
        String url = ApiConstants.getFindCodeLocationProductUrl(code);
        loadingDialog.show();
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> parseResponseProductLocation(response, isOldLocation),
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (!tokenManager.isTokenExpired()) {
                    headers.put("Authorization", "Bearer " + token);
                }else {
                    tokenManager.clearTokenAndLogout();
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    @SuppressLint("SetTextI18n")
    private void parseResponseProductLocation(String response, boolean isOldLocation) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONArray content = jsonObject.optJSONArray("content");
                if (content != null && content.length() > 0) {
                    for (int i = 0; i < content.length(); i++) {
                        JSONObject product = content.getJSONObject(i);

                        if (isOldLocation) {
                            productName1.setText(product.optString("title", "N/A"));
                            productQuantity1.setText(String.valueOf(product.optInt("quantity", 0)));
                            productCode1.setText(product.optString("code", "N/A"));
                        } else {
                            productName2.setText(product.optString("title", "N/A"));
                            productQuantity2.setText(String.valueOf(product.optInt("quantity", 0)));
                            productCode2.setText(product.optString("code", "N/A"));
                        }
                    }
                }else {
                    Toast.makeText(this,getString(R.string.data_null),Toast.LENGTH_SHORT).show();
                    if (!isOldLocation){
                        productName2.setText("null");
                        productQuantity2.setText("null");
                        productCode2.setText("null");
                        scannedLocation2 = "SKIP";
                        updateProductLocation2ScanStatus();
//                        continueConfirmProductBtn.setEnabled(true);
//                        if (qrCodeManager != null) {
//                            qrCodeManager.unregister();
//                            qrCodeManager = null;
//                        }
                    }
//                    else {
//                        updateProductLocation2ScanStatus(true, getString(R.string.skip_code));
//                    }
                }
            } else {
                Log.d("error", "Unknown error");
                Toast.makeText(this,getString(R.string.failed_parse_response),Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    //Show error process Api
    private void handleError(Exception error) {
        String errorMsg = getString(R.string.error_parse);
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = getString(R.string.error_timeout);
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = getString(R.string.error_no_connection);
        }
        loadingDialog.dismiss();
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

}