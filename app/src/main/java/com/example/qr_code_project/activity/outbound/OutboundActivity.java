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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.data.manager.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.adapter.ExportAdapter;
import com.example.qr_code_project.data.modal.ExportModal;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.helper.SSLHelper;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OutboundActivity extends AppCompatActivity {

    private EditText totalExportEt,totalProductEt;
    private Button submitCombineBtn;
    private RecyclerView exportsRv;

    private QRcodeManager qrcodeManager;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private ArrayList<ExportModal> exportList;
    private ExportAdapter exportAdapter;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_outbound);

        SSLHelper.trustAllCertificates();

        utils();

        utilBtn();
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

    private void utilBtn() {
        submitCombineBtn.setOnClickListener(v -> {
            if (exportList.isEmpty()) {
                Toast.makeText(OutboundActivity.this, getString(R.string.combine_toast)
                        , Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(OutboundActivity.this, CombineExportActivity.class);
            intent.putParcelableArrayListExtra("exportList", exportList);
            startActivity(intent);
        });
    }

    private void loadInbound(String scanValue) {
        loadingDialog.show();
        if (scanValue == null || scanValue.isEmpty()) {
            Toast.makeText(this, getString(R.string.scan_value_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        for (ExportModal modal : exportList) {
            if (modal.getCodeEp().equals(scanValue)) {
                Toast.makeText(this, getString(R.string.exists), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                return;
            }
        }

        StringRequest request = getStringRequest(scanValue);
        request.setRetryPolicy(new DefaultRetryPolicy(
                10 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    private @NonNull StringRequest getStringRequest(String scanValue) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(scanValue);
        return new StringRequest(
                Request.Method.GET, url,
                this::responseData,
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (tokenManager.isTokenExpired()) {
                    tokenManager.clearTokenAndLogout();
                }else {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

    }

    @SuppressLint("NotifyDataSetChanged")
    private void responseData(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    content(content);
                }
            } else {
                Toast.makeText(this,getString(R.string.response_fail)
                        ,Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e("responseValue", "Failed to parse JSON response", e);
            Toast.makeText(this,getString(R.string.response_fail),Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
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
    }

    @SuppressLint("NotifyDataSetChanged")
    private void content(JSONObject content) throws JSONException {

        boolean isAction = content.optBoolean("isAction", false);

        if (isAction) {
            Toast.makeText(this, getString(R.string.has_been_pick_up), Toast.LENGTH_SHORT).show();
            return;
        }

        int id = content.optInt("id",0);
        String codeEp = content.optString("code", "N/A");
        int items = content.optInt("totalProduct", 0);
        int totalItem = content.optInt("totalQuantity", 0);

        exportList.add(new ExportModal(codeEp, items, totalItem,id));
        updateTotalValues();

        if (exportAdapter == null) {
            exportAdapter = new ExportAdapter(this, exportList,
                exportModal -> {
                    Intent intent = new Intent(OutboundActivity.this, ExportDetailActivity.class);
                    intent.putExtra("codeEp", exportModal.getCodeEp());
                    startActivity(intent);
                },
                deletedItem -> {
                    exportList.remove(deletedItem);
                    updateTotalValues();
                }
            );
            exportsRv.setAdapter(exportAdapter);
        } else {
            exportAdapter.notifyDataSetChanged();
        }
    }

    private void updateTotalValues() {
        totalExportEt.setText(String.valueOf(exportList.size()));

        int totalProduct = exportList.stream().mapToInt(ExportModal::getTotalItem).sum();
        totalProductEt.setText(String.valueOf(totalProduct));
    }

    private void utils() {
        totalExportEt = findViewById(R.id.totalExportEt);
        totalProductEt = findViewById(R.id.totalProductEt);
//        totalRealQuantityEt = findViewById(R.id.totalRealQuantityEt);
        submitCombineBtn = findViewById(R.id.submitCompineBtn);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        exportsRv = findViewById(R.id.exportsRv);
        exportList = new ArrayList<>();
        exportsRv.setLayoutManager(new LinearLayoutManager(this));
        loadingDialog = new LoadingDialog(this);
        qrcodeManager = new QRcodeManager(this);
        tokenManager = new TokenManager(this);
    }

}