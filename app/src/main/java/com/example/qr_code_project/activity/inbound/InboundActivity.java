package com.example.qr_code_project.activity.inbound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.activity.swap.UnSuccessSwapLocationActivity;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.data.adapter.ProductAdapter;
import com.example.qr_code_project.data.modal.ProductModal;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.helper.SSLHelper;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InboundActivity extends AppCompatActivity {

    private static final String TAG = "InboundActivity";

    // Create UI Components
    private EditText codeEt;
//            titleEt;
    private TextView itemsEt, totalEt, totalRealQuantityEt;
    private RecyclerView productsRv;
    private Button submitBtn,resetBtn;

    // API and Data Management
    private int totalRealQuantity;
    private QRcodeManager qrcodeManager;
    private RequestQueue requestQueue;
//    private SharedPreferences sharedPreferences;
    private ArrayList<ProductModal> productArrayList;
    private ProductAdapter productAdapter;
    private final Map<Integer, Object> productMap = new HashMap<>();
    private LoadingDialog loadingDialog;
    private boolean isSubmit = false;
//    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inbound);

        // Initialize UI components
        initUI();

        SSLHelper.trustAllCertificates();

        //Set event when click submit button
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = codeEt.getText().toString();
                if(totalRealQuantity == 0){
                    Toast.makeText(InboundActivity.this,getString(R.string.real_quantity_null)
                            ,Toast.LENGTH_SHORT).show();
                    return;
                }
                submit(code,totalRealQuantity);
            }
        });

        //Set event when click reset button
        resetBtn.setOnClickListener(v -> resetData());

        if(isSubmit){
            qrcodeManager.unregister();
        }
    }
    //Scan service
    @Override
    protected void onResume() {
        super.onResume();
        qrcodeManager.setListener(this::loadInbound);
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrcodeManager.setListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrcodeManager != null) {
            qrcodeManager.unregister();
        }
    }

    //Get data from ConfirmInboundActivity
    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> confirmProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    updateRealQuantities(result.getData());
                }
            });

    //Update total real quantity
    @SuppressLint("NotifyDataSetChanged")
    private void updateRealQuantities(Intent data) {
        Map<Integer, Object> updatedQuantities =
                (Map<Integer, Object>) data.getSerializableExtra("productMap");

        if (updatedQuantities != null) {
            productMap.putAll(updatedQuantities);
            productAdapter.notifyDataSetChanged();

            totalRealQuantity = 0;
            for (Map.Entry<Integer, Object> entry : productMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> info = (Map<String, Object>) entry.getValue();
                    Object value = info.get("actualQuantity");

                    if (value instanceof Number) {
                        totalRealQuantity += ((Number) value).intValue();
                    } else {
                        Log.e("CombineExportActivity"
                                , "actualQuantity is not a number: " + value);
                    }
                }
            }
            totalRealQuantityEt.setText(String.valueOf(totalRealQuantity));
            checkAllProductsConfirmed();
        }
    }

    //Check product commit for submit data
    private void checkAllProductsConfirmed() {
        if(productMap.size() == productArrayList.size()){
            submitBtn.setVisibility( View.VISIBLE);
            resetBtn.setVisibility(View.GONE);
            isSubmit = true;
        }else {
            submitBtn.setVisibility( View.GONE);
            isSubmit = false;
        }
    }

    //Reset data for scan again
    private void resetData() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title))
                .setMessage(getString(R.string.reset_data))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    resetConfirm();
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();

    }

    @SuppressLint("NotifyDataSetChanged")
    private void resetConfirm() {
        codeEt.setText("");
//        titleEt.setText("");
        itemsEt.setText("");
        totalEt.setText("");
        totalRealQuantityEt.setText("0");

        productArrayList.clear();
        productMap.clear();
        totalRealQuantity = 0;

        // Update RecyclerView
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }
        // Create scan
        qrcodeManager = new QRcodeManager(this);
        qrcodeManager.setListener(this::loadInbound);

        // Hide submit button
        submitBtn.setVisibility(View.GONE);

        Toast.makeText(this, getString(R.string.data_reset), Toast.LENGTH_SHORT).show();
    }

    //Submit data
    private void submit(String code, int quantity) {
        loadingDialog.show();
        String url = ApiConstants.INBOUND_SUBMIT;
        Log.d(TAG,url);
        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");

                        if (isSuccess) {
                            Toast.makeText(InboundActivity.this,
                                    getString(R.string.success_response), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(InboundActivity.this,
                                    MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(InboundActivity.this, getString(R.string.failed_parse_response),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                this::handleError)
        {
//            @Override
//            public byte[] getBody() {
//                JSONObject params = new JSONObject();
//                try {
//                    params.put("code", code);
//                    params.put("quantity", quantity);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                Log.d(TAG, "Request Body: " + params.toString());
//                return params.toString().getBytes();
//            }

            @Override
            public byte[] getBody() {
                JSONArray jsonArray = new JSONArray();
                JSONObject params = new JSONObject();
                try {
                    params.put("code", code);
                    params.put("quantity", quantity);
                    jsonArray.put(params);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Request Body: " + jsonArray.toString());
                return jsonArray.toString().getBytes();
            }


            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (token != null) {
//                    headers.put("Authorization", "Bearer " + token);
//                }
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

    //UI components
    private void initUI() {
        codeEt = findViewById(R.id.codeEt);
//        titleEt = findViewById(R.id.titleEt);
        itemsEt = findViewById(R.id.itemsEt);
        totalEt = findViewById(R.id.totalEt);
        productsRv = findViewById(R.id.productsRv);
        totalRealQuantityEt = findViewById(R.id.totalRealQuantityEt);
        submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setVisibility(View.GONE);
        resetBtn = findViewById(R.id.resetBtn);

        qrcodeManager = new QRcodeManager(this);
        loadingDialog = new LoadingDialog(this);
        requestQueue = Volley.newRequestQueue(this);
//        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        productsRv.setLayoutManager(new LinearLayoutManager(this));
        productArrayList = new ArrayList<>();
//        tokenManager = new TokenManager(this);
        if(productMap.isEmpty()){
            totalRealQuantityEt.setText("0");
        }
    }

    //Get data Inbound from Api
    private void loadInbound(String scanValue) {
        if (scanValue == null || scanValue.isEmpty()) {
            Toast.makeText(this, getString(R.string.not_value), Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();

//        if (productMap.size() == productArrayList.size() && !productArrayList.isEmpty()) {
//            qrcodeManager.unregister();
//        }

        String url = ApiConstants.getFindOneCodeInboundUrl(scanValue);
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
                this::handleError
        ) ;
//        {
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (tokenManager.isTokenExpired()) {
//                    tokenManager.clearTokenAndLogout();
//                }else {
//                    headers.put("Authorization", "Bearer " + token);
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

    //Process data from Api
    @SuppressLint("NotifyDataSetChanged")
    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                assert content != null;
                JSONObject data = content.optJSONObject("data");
                if (data != null) {
                    populateContent(data);
                    if (!productArrayList.isEmpty()) {
                        qrcodeManager.unregister();
                    }
                }
            } else {
                showError(getString(R.string.not_data));
                if (qrcodeManager != null) {
                    qrcodeManager.setListener(this::loadInbound);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON response", e);
            showError(getString(R.string.fail_load_data));
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        boolean isAction = content.optBoolean("isAction",false);

        if (isAction){
            Toast.makeText(this,"The Import has been import!",Toast.LENGTH_SHORT).show();
            return;
        }

        codeEt.setText(content.optString("code", "N/A"));
//        titleEt.setText(content.optString("tite", "N/A"));
        itemsEt.setText(String.valueOf(content.optInt("quantity", 0)));
        totalEt.setText(String.valueOf(content.optInt("quantityProduct", 0)));
        JSONArray products = content.optJSONArray("locationDataInbounds");
        productArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(products).length(); i++) {
            JSONObject object = products.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            int quantity = object.optInt("quantity");
            String location = object.optString("code",null);
            String code = object.optString("title", "N/A");
            String image = object.optString("category_image", "N/A");

            productArrayList.add(new ProductModal(id, title, quantity, location, code, image));
        }

        if (productAdapter == null) {
            productAdapter = new ProductAdapter(this, productArrayList, productMap,
                    (product, updatedMap) -> {
                        Intent intent = new Intent(InboundActivity.this,
                                ConfirmInboundActivity.class);
                        intent.putExtra("product", product);
                        intent.putExtra("productMap", new HashMap<>((Map) updatedMap));
                        confirmProductLauncher.launch(intent);
                    });

            productsRv.setAdapter(productAdapter);
        } else {
//            productAdapter.notifyItemRangeRemoved(0, productArrayList.size());
            productAdapter.notifyDataSetChanged();
        }
    }

    //Show error
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //Show error process Api
    private void handleError(Exception error) {
        String errorMsg = getString(R.string.error_parse);
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = getString(R.string.error_timeout);
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = getString(R.string.error_no_connection);
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
        if (qrcodeManager != null) {
            qrcodeManager.setListener(this::loadInbound);
        }
    }

}
