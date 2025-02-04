package com.example.qr_code_project.activity.outbound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.adapter.ExportAdapter;
import com.example.qr_code_project.modal.ExportModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.SSLHelper;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OutboundActivity extends AppCompatActivity {

    private EditText totalExportEt,totalProductEt,totalRealQuantityEt;
    private Button submitCompineBtn;
    private RecyclerView exportsRv;

    private QRcodeManager qrcodeManager;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private ArrayList<ExportModal> exportList;
    private ExportAdapter exportAdapter;
    private int totalProduct;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_outbound);

        SSLHelper.trustAllCertificates();

        utils();

        qrcodeManager = new QRcodeManager(this);

        submitCompineBtn.setOnClickListener(v -> {
            if (exportList.isEmpty()) {
                Toast.makeText(OutboundActivity.this, "No products to combine!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(OutboundActivity.this, CombineExportActivity.class);
            intent.putParcelableArrayListExtra("exportList", exportList);
            intent.putExtra("totalProduct",totalProduct);
            startActivity(intent);
        });
    }

    private void loadInbound(String scanValue) {
        loadingDialog.show();
        if (scanValue == null || scanValue.isEmpty()) {
            Toast.makeText(this, "Scan value is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ExportModal modal : exportList) {
            if (modal.getCodeEp().equals(scanValue)) {
                Toast.makeText(this, "This code already exists!", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                return;
            }
        }

        StringRequest findInbound = getStringRequest(scanValue);

        requestQueue.add(findInbound);
    }

    private @NonNull StringRequest getStringRequest(String scanValue) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(scanValue);
        return new StringRequest(
                Request.Method.GET, url,
                OutboundActivity.this::parseResponse,
                OutboundActivity.this::handleError
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
            Log.e("responseValue", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(Exception error) {
        String errorMsg = "An error occurred. Please try again.";
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = "Request timed out. Please check your connection.";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = "No internet connection!";
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {

//        boolean isAction = content.optBoolean("isAction", false);
//
//        if (isAction) {
//            Toast.makeText(this, "This delivery has been picked up. !", Toast.LENGTH_SHORT).show();
//            return;
//        }

        int id = content.optInt("id",0);
        String codeEp = content.optString("code", "N/A");
        int items = content.optInt("totalProduct", 0);
        int totalItem = content.optInt("totalQuantity", 0);

        exportList.add(new ExportModal(codeEp, items, totalItem,id));
        int totalExport = exportList.size();
        totalExportEt.setText(String.valueOf(totalExport));

        totalProduct = exportList.stream().mapToInt(ExportModal::getTotalItem).sum();
        totalProductEt.setText(String.valueOf(totalProduct));

        if (exportAdapter == null) {
            exportAdapter = new ExportAdapter(this, exportList, exportModal -> {
                Intent intent = new Intent(OutboundActivity.this, ExportDetailActivity.class);
                intent.putExtra("codeEp", exportModal.getCodeEp());
                startActivity(intent);
            });
            exportsRv.setAdapter(exportAdapter);
        } else {
            exportAdapter.notifyDataSetChanged();
        }
    }


    private void utils() {
        totalExportEt = findViewById(R.id.totalExportEt);
        totalProductEt = findViewById(R.id.totalProductEt);
        totalRealQuantityEt = findViewById(R.id.totalRealQuantityEt);
        submitCompineBtn = findViewById(R.id.submitCompineBtn);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        exportsRv = findViewById(R.id.exportsRv);
        exportList = new ArrayList<>();
        exportsRv.setLayoutManager(new LinearLayoutManager(this));

        loadingDialog = new LoadingDialog(this);

    }


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
        //Unregister to receive Broadcast
        qrcodeManager.unregister();
    }

}