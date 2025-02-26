package com.example.qr_code_project.activity.outbound;

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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.data.adapter.ProductAdapter;
import com.example.qr_code_project.data.modal.ExportModal;
import com.example.qr_code_project.data.modal.ProductModal;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CombineExportActivity extends AppCompatActivity {

    private EditText totalExportCombineEt,totalProductCombineEt,
            totalRealQuantityCombineEt;
    private Button submitOutboundBtn;
    private RecyclerView combinedExportsRv;
    private ProductAdapter productAdapter;
//    private SharedPreferences sharedPreferences;
    private final Map<Integer, Object> productMap = new HashMap<>();
    private ArrayList<ExportModal> deliveryList = new ArrayList<>();
    private ArrayList<ProductModal> productList = new ArrayList<>();
    private final Map<Integer, ArrayList<ProductModal>> deliveryProductsMap = new HashMap<>();
    private LoadingDialog loadingDialog;
    //check all request update confirm
//    private int pendingRequests = 0;
//    private int successfulRequests = 0;
//    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_combine_export);

        util();

        utilButton();

        // Take a list export from Intent
        getDataFromIntent();

    }

    private void getDataFromIntent() {
        ArrayList<ExportModal> exportList = getIntent().getParcelableArrayListExtra("exportList");

        if (exportList != null && !exportList.isEmpty()) {
            deliveryList.addAll(exportList);
            totalExportCombineEt.setText(String.valueOf(exportList.size()));

            for (ExportModal export : exportList) {
                loadProductExport(export.getCodeEp());
//                totalProductCombineEt.setText(String.valueOf(productList.size()));
                int totalProduct = exportList.stream().mapToInt(ExportModal::getTotalItem).sum();
                totalProductCombineEt.setText(String.valueOf(totalProduct));
            }
        } else {
            Toast.makeText(this, getString(R.string.export_empty), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void utilButton() {
        submitOutboundBtn.setOnClickListener(v -> {
            // Check all of deliveries
            prepareSubmit();

        });
    }

    private void prepareSubmit() {
        // list deliveries
        ArrayList<Map<String, Object>> deliveriesList = new ArrayList<>();

        for (ExportModal export : deliveryList) {
            ArrayList<Map<String, Object>> productListForDelivery = new ArrayList<>();
            ArrayList<ProductModal> productsForThisDelivery = deliveryProductsMap.get(export.getId());

            if (productsForThisDelivery != null) {
                for (ProductModal product : productsForThisDelivery) {
                    Map<Integer, Object> realQuantityInfo = (Map<Integer, Object>) productMap.get(product.getId());

                    if (realQuantityInfo != null) {
                        Object location = realQuantityInfo.get("location");
                        Object area = realQuantityInfo.get("areaId");

                        if (location != null && area != null) {
                            Map<String, Object> productData = new HashMap<>();
                            productData.put("productDelivenote_id", 0);
                            productData.put("id_product", product.getId());
                            productData.put("quantity", product.getQuantity());
                            productData.put("location", location);
                            productData.put("area", area);

                            productListForDelivery.add(productData);
                        }
                    }
                }
            }

            // a delivery
            Map<String, Object> deliveryData = new HashMap<>();
            deliveryData.put("code", export.getCodeEp());
            deliveryData.put("quantity", export.getId());
//            deliveryData.put("products", productListForDelivery);

            deliveriesList.add(deliveryData);
        }

        // send all of list deliveries
        sendUpdateRequest(deliveriesList);
    }

    private void sendUpdateRequest(ArrayList<Map<String, Object>> requestData) {
        String url = ApiConstants.DELIVERY_UPDATE;
        JSONArray jsonArray = new JSONArray(requestData);

        Log.d("CombineExportActivity", "Sending update request: " + jsonArray.toString());

        loadingDialog.show();

        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String mess = jsonResponse.getString("content");
                        if (jsonResponse.getBoolean("success")) {
//                            JSONArray contentArray = jsonResponse.getJSONArray("content");
//                            Log.d("CombineExportActivity", "Content received: " + contentArray.toString());

                            Toast.makeText(CombineExportActivity.this, getString(R.string.success_response),
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(CombineExportActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("CombineExportActivity", mess);
                            Toast.makeText(CombineExportActivity.this, getString(R.string.update_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("CombineExportActivity", "Error parsing response!");
                    } finally {
                        loadingDialog.dismiss();
                    }
                },
                this::handleError) {
            @Override
            public byte[] getBody() {
                return jsonArray.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (!tokenManager.isTokenExpired()) {
//                    headers.put("Authorization", "Bearer " + token);
//                } else {
//                    tokenManager.clearTokenAndLogout();
//                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                60 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Volley.newRequestQueue(this).add(request);
    }


    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> confirmProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    getDataFromIntent(result);

                }
            });

    private void getDataFromIntent(ActivityResult result) {
        Map<Integer ,Object> returnedMap =
                (HashMap<Integer,Object>) result.getData()
                        .getSerializableExtra("productMap");

        if (returnedMap != null) {
            productMap.putAll(returnedMap);

            if (productAdapter != null) {
                productAdapter.notifyDataSetChanged();

                int totalRealQuantity = 0;
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
                totalRealQuantityCombineEt.setText(String.valueOf(totalRealQuantity));
            }
            checkAllProductsConfirmed();
        }
    }

    private void checkAllProductsConfirmed() {
        if(productMap.size() == productList.size()){
            submitOutboundBtn.setEnabled(true);
        }
    }

    private void loadProductExport(String codeEp) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(codeEp);

        loadingDialog.show();
        @SuppressLint("NotifyDataSetChanged")
        StringRequest request = new StringRequest(Request.Method.GET, url, 
                this::parseResponse,
                this::handleError) ;
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

        Volley.newRequestQueue(this).add(request);
    }

    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                assert content != null;
                JSONObject data = content.optJSONObject("data");
                if (data != null) {
                    populateContent(data);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to load product data!", Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        int delivery = content.optInt("id",0);
        JSONArray products = content.optJSONArray("locationDataInbounds");

        if (products != null) {
            for (int i = 0; i < products.length(); i++) {
                JSONObject object = products.getJSONObject(i);
                int id = object.optInt("id");
                String title = object.optString("title", "N/A");
                int quantity = object.optInt("quantity");
                String code = object.optString("title", "N/A");
                String location = object.optString("code","N/A");
                String image = object.optString("image", "");

                boolean found = false;
                for (ProductModal product : productList) {
                    if (product.getId() == id) {
                        product.setQuantity(product.getQuantity() + quantity);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    ProductModal newProduct = new ProductModal(id, title, quantity,
                            location, code, image);
                    productList.add(newProduct);
                    //deliveryProductsMap.get(delivery).add(newProduct);
                    //Log.d("CombineExportActivity", "Products for delivery " + delivery + ": " + deliveryProductsMap.get(delivery));

                }
                ArrayList<ProductModal> productListForThisDelivery = new ArrayList<>(productList);
                deliveryProductsMap.put(delivery, productListForThisDelivery);
                Log.d("CombineExportActivity", "Mapped products for delivery "
                        + delivery + ": " + productListForThisDelivery);

            }

            if (productAdapter == null) {
                productAdapter = new ProductAdapter(this, productList, productMap,
                        (product, updatedMap) -> {
                            Intent intent = new Intent(
                                    CombineExportActivity.this,
                                    ConfirmOutboundActivity.class);
                            intent.putExtra("product", product);
                            intent.putExtra("productMap",
                                    new HashMap<>((Map) updatedMap));
                            confirmProductLauncher.launch(intent);
                        });

                combinedExportsRv.setAdapter(productAdapter);
            } else {
                productAdapter.notifyDataSetChanged();
            }
        }
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

    private void util(){
        totalExportCombineEt = findViewById(R.id.totalExportCombineEt);
        totalProductCombineEt = findViewById(R.id.totalProductCombineEt);
        totalRealQuantityCombineEt = findViewById(R.id.totalRealQuantityCombineEt);
        combinedExportsRv = findViewById(R.id.productExportRv);
        submitOutboundBtn = findViewById(R.id.submitOutboundBtn);
        submitOutboundBtn.setEnabled(false);

        combinedExportsRv.setLayoutManager(new LinearLayoutManager(this));
//        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        loadingDialog = new LoadingDialog(this);
        productList = new ArrayList<>();
        deliveryList = new ArrayList<>();
//        tokenManager = new TokenManager(this);
    }
}