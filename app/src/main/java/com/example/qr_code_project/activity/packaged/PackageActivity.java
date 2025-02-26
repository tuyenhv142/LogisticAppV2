package com.example.qr_code_project.activity.packaged;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.data.adapter.ProductAdapter;
import com.example.qr_code_project.data.modal.ProductModal;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PackageActivity extends AppCompatActivity {

    private EditText codePackageEt;// titlePackageEt;
    private TextView itemPackagesEt
            , totalProductPackageEt, totalRQProductPackageEt;
    private RecyclerView productPackagesRv;
    private Button submitPackageBtn;
    private Button resetPackageBtn;

    private int totalRealQuantity;
    private int deliveryId;
    private QRcodeManager qrcodeManager;
    private RequestQueue requestQueue;
//    private SharedPreferences sharedPreferences;
    private ArrayList<ProductModal> productArrayList;
    private ProductAdapter productAdapter;
    private final Map<Integer, Object> productMap = new HashMap<>();
    private LoadingDialog loadingDialog;
//    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_package);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.setTitle("Package product");
//        setSupportActionBar(toolbar);

        initUI();

//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);
//        }
        utilBtn();
    }

    private void utilBtn() {
        submitPackageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = codePackageEt.getText().toString();
                if(deliveryId == 0){
                    Toast.makeText(PackageActivity.this,getString(R.string.real_quantity_null)
                            ,Toast.LENGTH_SHORT).show();
                    return;
                }
                submit(code,deliveryId);
            }
        });

        resetPackageBtn.setOnClickListener(v -> resetData());
    }

    //Submit data
    private void submit(String code, int deliveryId) {
        loadingDialog.show();
        String url = ApiConstants.UpdateOutboundIsPack(code);
        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");
//                        String message = jsonObject.optString("error", "Unknown error");

                        if (isSuccess) {
                            Toast.makeText(PackageActivity.this, getString(R.string.success_response)
                                    , Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PackageActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(PackageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                this::handleError) ;
//        {
//            @Override
//            public byte[] getBody(){
//                JSONObject params = new JSONObject();
//                try {
//                    params.put("code", code);
//                    params.put("id", deliveryId);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                Log.d("PackageActivity", "Request Body: " + params.toString());
//                return params.toString().getBytes();
//            }
//
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (token != null) {
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

//    @Override
//    public boolean onSupportNavigateUp() {
//        finish();
//        return true;
//    }

    @SuppressLint("NotifyDataSetChanged")
    private void resetData() {
        codePackageEt.setText("");
//        titlePackageEt.setText("");
        itemPackagesEt.setText("0");
        totalProductPackageEt.setText("0");
        totalRQProductPackageEt.setText("0");

        productArrayList.clear();
        productMap.clear();
        totalRealQuantity = 0;

        // Update RecyclerView
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }
        qrcodeManager = new QRcodeManager(this);
        qrcodeManager.setListener(this::loadPackage);

        // Hide submit button
        submitPackageBtn.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrcodeManager.setListener(this::loadPackage);
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrcodeManager.setListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qrcodeManager.unregister();
    }

    //Get data from ConfirmPackageActivity
    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> confirmProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                    , result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    updateRealQuantities(result.getData());
                }
            });

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
            totalRQProductPackageEt.setText(String.valueOf(totalRealQuantity));
            checkAllProductsConfirmed();
        }
    }

    private void checkAllProductsConfirmed() {
        submitPackageBtn.setVisibility(productMap.size() == productArrayList.size() ? View.VISIBLE : View.GONE);
    }

    private void loadPackage(String scanValue) {
        if (scanValue == null || scanValue.isEmpty()) {
            Toast.makeText(this, getString(R.string.scan_value_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        loadingDialog.show();

        String url = ApiConstants.getFindOneCodeDeliveryUrl(scanValue);
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
                Toast.makeText(this,jsonObject.optString("error"
                        , "Unknown error"),Toast.LENGTH_SHORT).show();
                if (qrcodeManager != null) {
                    qrcodeManager.setListener(this::loadPackage);
                }
            }
        } catch (JSONException e) {
            Log.e("package", "Failed to parse JSON response", e);
            Toast.makeText(this,getString(R.string.failed_parse_response),Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        boolean isAction = content.optBoolean("isPack", false);

        if (isAction) {
            Toast.makeText(this, getString(R.string.packaged), Toast.LENGTH_SHORT).show();
            return;
        }

        deliveryId = content.optInt("id",0);
        codePackageEt.setText(content.optString("code", "N/A"));
//        titlePackageEt.setText(content.optString("code", "N/A"));
        itemPackagesEt.setText(String.valueOf(content.optInt("quantity", 0)));
        totalProductPackageEt.setText(String.valueOf(content.optInt("quantityProduct", 0)));

        JSONArray products = content.optJSONArray("locationDataInbounds");
        productArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(products).length(); i++) {
            JSONObject object = products.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            int quantity = object.optInt("quantity");
            String location = object.optString("code");
            String code = object.optString("title", "N/A");
            String image = object.optString("image", "");

            productArrayList.add(new ProductModal(id, title, quantity, location, code, image));
        }

        if (productAdapter == null) {
            productAdapter = new ProductAdapter(this, productArrayList, productMap,
                    (product, updatedMap) -> {
                        Intent intent = new Intent(PackageActivity.this
                                , ConfirmPackageActivity.class);
                        intent.putExtra("product", product);
                        intent.putExtra("productMap", new HashMap<>((Map) updatedMap));
                        confirmProductLauncher.launch(intent);
                    });

            productPackagesRv.setAdapter(productAdapter);
        } else {
            productAdapter.notifyDataSetChanged();
        }
    }

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
            qrcodeManager.setListener(this::loadPackage);
        }
    }

    private void initUI() {
        codePackageEt = findViewById(R.id.codePackageEt);
//        titlePackageEt = findViewById(R.id.titlePackageEt);
        itemPackagesEt = findViewById(R.id.itemPackagesEt);
        totalProductPackageEt = findViewById(R.id.totalProductPackageEt);
        productPackagesRv = findViewById(R.id.productPackagesRv);
        totalRQProductPackageEt = findViewById(R.id.totalRQProductPackageEt);
        submitPackageBtn = findViewById(R.id.submitPackageBtn);
        submitPackageBtn.setVisibility(View.GONE);
        resetPackageBtn = findViewById(R.id.resetPackageBtn);

        qrcodeManager = new QRcodeManager(this);
        requestQueue = Volley.newRequestQueue(this);
//        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
//        tokenManager = new TokenManager(this);
        productPackagesRv.setLayoutManager(new LinearLayoutManager(this));
        productArrayList = new ArrayList<>();
        loadingDialog = new LoadingDialog(this);

        if(productMap.isEmpty()){
            totalRQProductPackageEt.setText("0");
        }
    }
}