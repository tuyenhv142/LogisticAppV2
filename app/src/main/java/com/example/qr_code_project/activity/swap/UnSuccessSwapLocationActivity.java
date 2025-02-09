package com.example.qr_code_project.activity.swap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.adapter.SwapLocationAdapter;
import com.example.qr_code_project.data.modal.SwapModal;
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

public class UnSuccessSwapLocationActivity extends AppCompatActivity implements SwapLocationAdapter.OnSwapClickListener {

    private LoadingDialog loadingDialog;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private ArrayList<SwapModal> swapArrayList;
    private SwapLocationAdapter swapLocationAdapter;
    private RecyclerView unSuccessSwapLocationsRv;
    private TokenManager tokenManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_un_success_swap_location);

        util();

        loadSwapPlan();
    }

    private void util(){
        tokenManager = new TokenManager(this);
        loadingDialog = new LoadingDialog(this);
        sharedPreferences = getSharedPreferences("AccountToken",MODE_PRIVATE);
        unSuccessSwapLocationsRv = findViewById(R.id.unSuccessSwapLocationsRv);
        unSuccessSwapLocationsRv.setLayoutManager(new LinearLayoutManager(this));
        requestQueue = Volley.newRequestQueue(this);
        swapArrayList = new ArrayList<>();
    }

    private void loadSwapPlan(){
        String url = ApiConstants.SWAP_LOCATION_CLAIM;
        loadingDialog.show();
        StringRequest request = new StringRequest(
                Request.Method.GET,url,
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
                Toast.makeText(this,jsonObject.optString("error"
                        , getString(R.string.unknown_error)),Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e("responseValue", "Failed to parse JSON response", e);
            Toast.makeText(this,getString(R.string.login_fail),Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        JSONArray swapLocations = content.optJSONArray("data");
        swapArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(swapLocations).length(); i++) {
            JSONObject object = swapLocations.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            String locationOldCode = object.optString("localtionOldCode","N/A");
            String locationNewCode = object.optString("localtionNewCode", "N/A");
            String warehouseOld = object.optString("warehouseOld", "N/A");
            String areaOld = object.optString("areaOld", "N/A");
            String floorOld = object.optString("floorOld","N/A");
            String warehouse = object.optString("warehouse", "N/A");
            String area = object.optString("area", "N/A");
            String floor = object.optString("floor", "N/A");

            swapArrayList.add(new SwapModal(floor, area, warehouse
                    , floorOld, areaOld, warehouseOld
                    ,locationNewCode,locationOldCode,title,id));
        }

        if (swapLocationAdapter == null) {
            swapLocationAdapter = new SwapLocationAdapter(this
                    , swapArrayList,this );

            unSuccessSwapLocationsRv.setAdapter(swapLocationAdapter);
        } else {
            swapLocationAdapter.notifyDataSetChanged();
        }
    }


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

    @Override
    public void onSwapItemClick(int swapId) {
        Intent intent = new Intent(this, DetailSwapLocationActivity.class);
        intent.putExtra("swapId", swapId);
        startActivity(intent);
    }
}