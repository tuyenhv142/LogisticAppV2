package com.example.qr_code_project.activity.packaged;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.ApiService;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PackageActivity extends AppCompatActivity {

    private EditText codePackageEt, titlePackageEt, itemPackagesEt
            , totalProductPackageEt, totalRQProductPackageEt;
    private RecyclerView productPackagesRv;
    private Button submitPackageBtn,resetPackageBtn;

    private int totalRealQuantity;
    private int deliveryId;
    private QRcodeManager qrcodeManager;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private ArrayList<ProductModal> productArrayList;
    private ProductAdapter productAdapter;
    private final Map<Integer, Object> productMap = new HashMap<>();
    private ApiService apiService;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_package);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Package product");
        setSupportActionBar(toolbar);

        initUI();

        // Hiển thị nút quay lại
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        //Set event when click submit button
        submitPackageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = codePackageEt.getText().toString();
                if(deliveryId == 0){
                    Toast.makeText(PackageActivity.this,"Real quantity is  null"
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
        apiService.submitPackage(code, deliveryId, new ApiService.ApiResponseListener() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(PackageActivity.this, response, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(PackageActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PackageActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void resetData() {
        codePackageEt.setText("");
        titlePackageEt.setText("");
        itemPackagesEt.setText("");
        totalProductPackageEt.setText("");
        totalRQProductPackageEt.setText("0");

        productArrayList.clear();
        productMap.clear();
        totalRealQuantity = 0;

        // Update RecyclerView
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }

        // Hide submit button
        submitPackageBtn.setVisibility(View.GONE);

        showError("Please press scan!");
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
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
            Toast.makeText(this, "Scan value is empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        loadingDialog.show();

        String url = ApiConstants.getFindOneCodeDeliveryUrl(scanValue);
        StringRequest findInbound = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
                this::handleError
        ) {
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

        requestQueue.add(findInbound);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    populateContent(content);
                }
            } else {
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("package", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        boolean isAction = content.optBoolean("isPack", false);

        if (isAction) {
            Toast.makeText(this, "This delivery has been packaged !", Toast.LENGTH_SHORT).show();
            return;
        }
        deliveryId = content.optInt("id",0);
        codePackageEt.setText(content.optString("code", "N/A"));
        titlePackageEt.setText(content.optString("title", "N/A"));
        itemPackagesEt.setText(String.valueOf(content.optInt("totalProduct", 0)));
        totalProductPackageEt.setText(String.valueOf(content.optInt("totalQuantity", 0)));

        JSONArray products = content.optJSONArray("products");
        productArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(products).length(); i++) {
            JSONObject object = products.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            int quantity = object.optInt("quantityDelivery");
//            int location = object.optString("dataItem");
            String code = object.optString("code", "N/A");
            String image = object.optString("image", "");

            productArrayList.add(new ProductModal(id, title, quantity, null, code, image));
        }

        if (productAdapter == null) {
            productAdapter = new ProductAdapter(this, productArrayList, productMap,
                    (product, updatedMap) -> {
                        Intent intent = new Intent(PackageActivity.this, ConfirmPackageActivity.class);
                        intent.putExtra("product", product);
                        intent.putExtra("realQuantitiesMap", new HashMap<>((Map) updatedMap));
                        confirmProductLauncher.launch(intent);
                    });

            productPackagesRv.setAdapter(productAdapter);
        } else {
            productAdapter.notifyDataSetChanged();
        }
    }

    private void handleError(Exception error) {
        Log.e("Package", "API Error", error);
        loadingDialog.dismiss();
        showError( "An error occurred. Please try again.");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void initUI() {
        codePackageEt = findViewById(R.id.codePackageEt);
        titlePackageEt = findViewById(R.id.titlePackageEt);
        itemPackagesEt = findViewById(R.id.itemPackagesEt);
        totalProductPackageEt = findViewById(R.id.totalProductPackageEt);
        productPackagesRv = findViewById(R.id.productPackagesRv);
        totalRQProductPackageEt = findViewById(R.id.totalRQProductPackageEt);
        submitPackageBtn = findViewById(R.id.submitPackageBtn);
        submitPackageBtn.setVisibility(View.GONE);
        resetPackageBtn = findViewById(R.id.resetPackageBtn);

        qrcodeManager = new QRcodeManager(this);
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);

        productPackagesRv.setLayoutManager(new LinearLayoutManager(this));
        productArrayList = new ArrayList<>();
        apiService = new ApiService(this);
        loadingDialog = new LoadingDialog(this);

        if(productMap.isEmpty()){
            totalRQProductPackageEt.setText("0");
        }
    }
}