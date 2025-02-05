package com.example.qr_code_project.activity.outbound;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExportDetailActivity extends AppCompatActivity {

    private EditText codeExportEt,titleExportEt,itemsExportEt,totalExportEt;
    private Button returnBtn;
    private RecyclerView productExportRv;
    private ArrayList<ProductModal> productList;
    private ProductAdapter productAdapter;
    private SharedPreferences sharedPreferences;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_export_detail);

        utils();

        productList = new ArrayList<>();


        String codeEp = getIntent().getStringExtra("codeEp");
        loadProductsForOrder(codeEp);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void loadProductsForOrder(String codeEp) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(codeEp);

        @SuppressLint("NotifyDataSetChanged") StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    JSONObject content = jsonObject.optJSONObject("content");

                    assert content != null;
                    String code = content.optString("code","N/A");
                    String titleEx = content.optString("title","N/A");
                    int items = content.optInt("totalProduct",0);
                    int totalProduct = content.optInt("totalQuantity",0);


                    codeExportEt.setText(code);
                    titleExportEt.setText(titleEx);
                    itemsExportEt.setText(String.valueOf(items));
                    totalExportEt.setText(String.valueOf(totalProduct));

                    JSONArray products = content.optJSONArray("products");
                    if (products != null) {
                        for (int i = 0; i < products.length(); i++) {
                            JSONObject object = products.getJSONObject(i);
                            int id = object.optInt("id");
                            String title = object.optString("title", "N/A");
                            int quantity = object.optInt("quantityDelivery");
                            String location = null;
                            String image = object.optString("image", "");

                            productList.add(new ProductModal(id, title, quantity, location, codeEp, image));
                        }
                        productAdapter = new ProductAdapter(this, productList, new HashMap<>(),
                                (product, updatedMap) -> Toast.makeText(this, "Clicked: " + product.getTitle(), Toast.LENGTH_SHORT).show());

                        productExportRv.setAdapter(productAdapter);
                        productAdapter.notifyDataSetChanged();
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }finally {
                loadingDialog.dismiss();
            }


        }, error -> {
            Toast.makeText(this, "Failed to load products!", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
        })
        {
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

        Volley.newRequestQueue(this).add(request);
    }

    private void utils(){
        codeExportEt = findViewById(R.id.codeExportEt);
        titleExportEt = findViewById(R.id.titleExportEt);
        itemsExportEt = findViewById(R.id.itemsExportEt);
        totalExportEt = findViewById(R.id.totalExportEt);
        returnBtn = findViewById(R.id.returnBtn);
        productExportRv = findViewById(R.id.productExportRv);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        productExportRv.setLayoutManager(new LinearLayoutManager(this));
        loadingDialog = new LoadingDialog(this);
    }
}