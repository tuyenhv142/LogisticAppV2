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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfirmSwapLocationActivity extends AppCompatActivity {

    private TextView codeLocation2,codeLocation1, codeLocation1New,codeLocation2New;
    private TextView productCode1Tv,productQuantity1Tv,productName1Tv;
    private TextView productCode2Tv,productQuantity2Tv,productName2Tv;
    private ImageView checkLocation1Iv,checkLocation2Iv;
    private EditText nameProductLocation1,quantityProductLocation1,codeProductLocation1;
    private EditText nameProductLocation2,quantityProductLocation2,codeProductLocation2;
    private Button confirmSwapBtn;
    private String product1Location,product2Location;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private QRcodeManager qrCodeManager;
    private String scannedProductLocation1 = "";
    private String scannedLocation1New = "";
    private String scannedProductLocation2= "";
    private String scannedLocation2New= "";
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;
    private int pendingRequests = 0;
    private AlertDialog productDialog;
    private boolean isLocation1Confirmed = false;
    private boolean isLocation2Confirmed = false;

    private int statusId;
    private int swapId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_swap_location);

        util();

        setupQRManager();

        fetchProductLocation(product1Location,true);

        fetchProductLocation(product2Location,false);

        fetchPlanStatus(swapId);

        confirmSwapBtn.setOnClickListener(v -> onConfirmSwap(swapId));
    }

    private void onConfirmSwap(int swapId) {
        loadingDialog.show();
        String url = ApiConstants.SWAP_LOCATION_SUBMIT;
        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");

                        if (isSuccess) {
                            loadingDialog.dismiss();
                            Toast.makeText(ConfirmSwapLocationActivity.this,
                                    getString(R.string.success_response), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ConfirmSwapLocationActivity.this,
                                    SwapLocationActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        loadingDialog.dismiss();
                        Toast.makeText(ConfirmSwapLocationActivity.this, e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                this::handleError) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("id_statuswarehourse", statusId);
                    params.put("title", "done");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("ConfirmSwapLocationActivity", "Request Body: " + params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
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

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
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

    private void updateProductLocation1ScanStatus() {
        productCode1Tv.setTextColor(Color.BLACK);
        productQuantity1Tv.setTextColor(Color.BLACK);
        productName1Tv.setTextColor(Color.BLACK);
        codeProductLocation1.setTextColor(Color.BLACK);
        quantityProductLocation1.setTextColor(Color.BLACK);
        nameProductLocation1.setTextColor(Color.BLACK);
        codeLocation1New.setTextColor(Color.BLACK);
        checkLocation1Iv.setImageTintList(ColorStateList.valueOf(Color.GREEN));
        isLocation1Confirmed = true;
    }

    private void updateProductLocation2ScanStatus() {
        codeLocation2New.setTextColor(Color.BLACK);
        productCode2Tv.setTextColor(Color.BLACK);
        productQuantity2Tv.setTextColor(Color.BLACK);
        codeProductLocation2.setTextColor(Color.BLACK);
        quantityProductLocation2.setTextColor(Color.BLACK);
        productName2Tv.setTextColor(Color.BLACK);
        nameProductLocation2.setTextColor(Color.BLACK);
        checkLocation2Iv.setImageTintList(ColorStateList.valueOf(Color.GREEN));
        isLocation2Confirmed = true;
    }

    private void handleScannedData(String qrCodeText) {
        if ( !scannedProductLocation1.equals("SKIP")) {
            if (scannedProductLocation1.isEmpty()){
                scannedProductLocation1 = qrCodeText;
                if (scannedProductLocation1.equals(codeProductLocation1.getText().toString().trim())) {
                    String productCode = codeProductLocation1.getText().toString().trim();
                    String productName = nameProductLocation1.getText().toString().trim();
                    String productQuantity = quantityProductLocation1.getText().toString().trim();
                    showProductDialog(product2Location, productCode, productName, productQuantity);
                } else {
                    Toast.makeText(this, R.string.invalid_product, Toast.LENGTH_SHORT).show();
                    scannedProductLocation1 = "";
                }
            }else if (scannedLocation1New.isEmpty() ) {
                scannedLocation1New = qrCodeText;
                if (scannedLocation1New.equals(product2Location)) {
                    updateProductLocation1ScanStatus();
                    Toast.makeText(this, R.string.location_correct, Toast.LENGTH_SHORT).show();
                    if (productDialog != null && productDialog.isShowing()) {
                        productDialog.dismiss();
                    }
                } else {
                    Toast.makeText(this, R.string.scan_again_location, Toast.LENGTH_SHORT).show();
                    scannedLocation1New = "";
                }

            }else if (scannedProductLocation2.isEmpty()) {
                scannedProductLocation2 = qrCodeText;
                if (scannedProductLocation2.equals(codeProductLocation2.getText().toString().trim())) {
                    String productCode = codeProductLocation2.getText().toString().trim();
                    String productName = nameProductLocation2.getText().toString().trim();
                    String productQuantity = quantityProductLocation2.getText().toString().trim();
                    showProductDialog(product1Location, productCode, productName, productQuantity);
                } else {
                    Toast.makeText(this, R.string.invalid_product, Toast.LENGTH_SHORT).show();
                    scannedProductLocation2 = "";
                }
            } else if (scannedLocation2New.isEmpty()) {
                scannedLocation2New = qrCodeText;
                if (scannedLocation2New.equals(product1Location)) {
                    Toast.makeText(this, R.string.location_correct, Toast.LENGTH_SHORT).show();
                    updateProductLocation2ScanStatus();
                    if (productDialog != null && productDialog.isShowing()) {
                        productDialog.dismiss();
                    }
                    confirmSwapBtn.setEnabled(true);
                    if (qrCodeManager != null) {
                        qrCodeManager.unregister();
                        qrCodeManager = null;
                    }
                } else {
                    Toast.makeText(this, R.string.scan_again_location, Toast.LENGTH_SHORT).show();
                    scannedLocation2New = "";
                }
            }
        } else if (scannedProductLocation2.isEmpty()) {
            scannedProductLocation2 = qrCodeText;
            if (scannedProductLocation2.equals(codeProductLocation2.getText().toString().trim())) {
                String productCode = codeProductLocation2.getText().toString().trim();
                String productName = nameProductLocation2.getText().toString().trim();
                String productQuantity = quantityProductLocation2.getText().toString().trim();
                showProductDialog(product1Location, productCode, productName, productQuantity);
            } else {
                Toast.makeText(this, R.string.invalid_product, Toast.LENGTH_SHORT).show();
                scannedProductLocation2 = "";
            }
        } else if (scannedLocation2New.isEmpty()) {
            scannedLocation2New = qrCodeText;
            if (scannedLocation2New.equals(product1Location)) {
                Toast.makeText(this, R.string.location_correct, Toast.LENGTH_SHORT).show();
                updateProductLocation2ScanStatus();
                if (productDialog != null && productDialog.isShowing()) {
                    productDialog.dismiss();
                }
                confirmSwapBtn.setEnabled(true);
                if (qrCodeManager != null) {
                    qrCodeManager.unregister();
                    qrCodeManager = null;
                }
            } else {
                Toast.makeText(this, R.string.scan_again_location, Toast.LENGTH_SHORT).show();
                scannedLocation2New = "";
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }

    @SuppressLint("SetTextI18n")
    private void util(){
        codeLocation1New = findViewById(R.id.codeLocation1New);
        codeLocation2New = findViewById(R.id.codeLocation2New);
//        locationNewBarcodeStatus1 = findViewById(R.id.locationNewBarcodeStatus1);
//        locationNewBarcodeStatus2 = findViewById(R.id.locationNewBarcodeStatus2);
        nameProductLocation1 = findViewById(R.id.nameProductLocation1);
        quantityProductLocation1 = findViewById(R.id.quantityProductLocation1);
        codeProductLocation1 = findViewById(R.id.codeProductLocation1);
        codeLocation2 = findViewById(R.id.codeLocation2);
        nameProductLocation2 = findViewById(R.id.nameProductLocation2);
        quantityProductLocation2 = findViewById(R.id.quantityProductLocation2);
        codeProductLocation2 = findViewById(R.id.codeProductLocation2);
        codeLocation1 = findViewById(R.id.codeLocation1);
        confirmSwapBtn = findViewById(R.id.confirmSwapBtn);

        productCode1Tv = findViewById(R.id.productCode1Tv);
        productQuantity1Tv = findViewById(R.id.productQuantity1Tv);
        productCode2Tv = findViewById(R.id.productCode2Tv);
        productQuantity2Tv = findViewById(R.id.productQuantity2Tv);
        productName2Tv = findViewById(R.id.productName2Tv);
        productName1Tv = findViewById(R.id.productName1Tv);
        checkLocation1Iv = findViewById(R.id.checkLocation1Iv);
        checkLocation2Iv = findViewById(R.id.checkLocation2Iv);

        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);

        product1Location = sharedPreferences.getString("product1_location", "N/A");
        product2Location = sharedPreferences.getString("product2_location", "N/A");
        swapId = sharedPreferences.getInt("swap_location_id", 0);
//        nameLocationOld1Tv.setText(product1Location);
//        nameLocationOld2Tv.setText(product2Location);
        codeLocation2.setText(product2Location);
        codeLocation1.setText(product1Location);
        codeLocation1New.setText(product2Location);
        codeLocation2New.setText(product1Location);
        requestQueue = Volley.newRequestQueue(this);
        confirmSwapBtn.setEnabled(false);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
    }

    private void fetchProductLocation(String code, boolean isOldLocation) {
        String url = ApiConstants.getFindCodeLocationProductUrl(code);
        Log.d("url_getFindCodeLocationProductUrl",url);

        loadingDialog.show();
        pendingRequests++;

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
                }else{
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
        checkAndDismissLoading();
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONArray content = jsonObject.optJSONArray("content");
                if (content != null && content.length()>0) {
                    for (int i = 0; i < 1; i++) {
                        JSONObject product = content.getJSONObject(i);

                        if (isOldLocation) {
                            nameProductLocation1.setText(product.optString("title", "N/A"));
                            quantityProductLocation1.setText(String.valueOf(product.optInt("quantity", 0)));
                            codeProductLocation1.setText(product.optString("code", "N/A"));
                        } else {
                            nameProductLocation2.setText(product.optString("title", "N/A"));
                            quantityProductLocation2.setText(String.valueOf(product.optInt("quantity", 0)));
                            codeProductLocation2.setText(product.optString("code", "N/A"));
                        }
                    }
                }else{
                    if (isOldLocation) {
                        nameProductLocation1.setText("null");
                        quantityProductLocation1.setText("null");
                        codeProductLocation1.setText("null");
                        scannedProductLocation1 = "SKIP";
                        updateProductLocation1ScanStatus();
                    } else {
                        nameProductLocation2.setText("null");
                        quantityProductLocation2.setText("null");
                        codeProductLocation2.setText("null");
                        scannedProductLocation2 = "SKIP";
                        updateProductLocation2ScanStatus();
                    }
                }
            } else {
                Log.d("error", "Unknown error");
                Toast.makeText(this,getString(R.string.failed_parse_response),Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.response_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndDismissLoading() {
        pendingRequests--;
        if (pendingRequests <= 0) {
            loadingDialog.dismiss();
        }
    }


    private void handleError(Exception error) {
        checkAndDismissLoading();
        String errorMsg = getString(R.string.error_parse);
        if (error instanceof TimeoutError) {
            errorMsg = getString(R.string.error_timeout);
        } else if (error instanceof NoConnectionError) {
            errorMsg = getString(R.string.error_no_connection);
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void fetchPlanStatus(int code) {
        loadingDialog.show();
        String url = ApiConstants.getFindByPlanUrl(code);
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
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

    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    statusId = content.optInt("id_status",0);
                }
            } else {
                Log.d("error", "Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.response_fail), Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

}