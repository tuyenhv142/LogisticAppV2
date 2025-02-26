package com.example.qr_code_project.activity.swap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

public class SwapLocationActivity extends AppCompatActivity implements SwapLocationAdapter.OnSwapClickListener {

    private RecyclerView swapLocationsRv;
//    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private ArrayList<SwapModal> swapArrayList;
    private SwapLocationAdapter swapLocationAdapter;
    private LoadingDialog loadingDialog;
//    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_swap_location);

        util();

        loadSwapPlan();
    }

    private void util(){
        swapLocationsRv = findViewById(R.id.swapLocationsRv);

        loadingDialog = new LoadingDialog(this);
        requestQueue = Volley.newRequestQueue(this);
//        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        swapLocationsRv.setLayoutManager(new LinearLayoutManager(this));
        swapArrayList = new ArrayList<>();
//        tokenManager = new TokenManager(this);
    }

    private void loadSwapPlan(){
        String url = ApiConstants.SWAP_LOCATION;
        loadingDialog.show();
        StringRequest request = new StringRequest(
                Request.Method.GET,url,
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
                if (content != null) {
                    populateContent(content);
                }
            } else {
                Toast.makeText(this,jsonObject.optString("error",
                        "Unknown error"),Toast.LENGTH_SHORT).show();
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
            int isConfirmation = object.optInt("status",0);

            if (isConfirmation == 1){
                continue;
            }

            int id = object.optInt("id");
//            String title = object.optString("title", "N/A");
            String locationOldCode = object.optString("locationOld","N/A");
            String locationNewCode = object.optString("locationNew", "N/A");
            String areaOld = object.optString("areaOld", "N/A");
            String shelfOld = object.optString("shelfOld", "N/A");
            String lineOld = object.optString("lineOld","N/A");
            String areaNew = object.optString("areaNew", "N/A");
            String lineNew = object.optString("lineNew", "N/A");
            String shelfNew = object.optString("shelfNew", "N/A");
//            String shelfOld = object.optString("shelfOld","N/A");
//            String shelfNew = object.optString("shelf","N/A");

            swapArrayList.add(new SwapModal(areaNew, lineNew, null
                    , areaOld, lineOld, null
                    ,locationNewCode,locationOldCode,null,id,shelfOld,shelfNew));
        }

        if (swapLocationAdapter == null) {
            swapLocationAdapter = new SwapLocationAdapter(this
                    , swapArrayList,this );

            swapLocationsRv.setAdapter(swapLocationAdapter);
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
//        showConfirmationDialog(swapId);
        Intent intent = new Intent(this, DetailSwapLocationActivity.class);
                        intent.putExtra("swapId", swapId);
                        startActivity(intent);
//                        finish();
    }

//    private void showConfirmationDialog(int swapId) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getString(R.string.confirm_task))
//                .setMessage(getString(R.string.accept))
//                .setPositiveButton(getString(R.string.yes), (dialog, which) -> sendConfirmationRequest(swapId))
//                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
//                .show();
//    }

//    private void sendConfirmationRequest(int swapId) {
//        String url = ApiConstants.SWAP_LOCATION_CONFIRM;
//        Log.d("swapID",""+swapId);
//        loadingDialog.show();
//
//        StringRequest request = new StringRequest(Request.Method.PUT, url,
//            response -> {
//                JSONObject jsonObject = null;
//                try {
//                    jsonObject = new JSONObject(response);
////                    if (jsonObject.getBoolean("content")) {
//                    if (jsonObject.getBoolean("success")) {
//                        Toast.makeText(this, getString(R.string.confirm_success), Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(this, DetailSwapLocationActivity.class);
//                        intent.putExtra("swapId", swapId);
//                        startActivity(intent);
//                        finish();
//                    } else {
//                        Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
//                    }
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    loadingDialog.dismiss();
//                }
//            },
//            error -> {
//                Toast.makeText(this, getString(R.string.error_confirm), Toast.LENGTH_SHORT).show();
//                loadingDialog.dismiss();
//            }
//        )
//        {
//            @Override
//            public byte[] getBody() {
//                JSONObject params = new JSONObject();
//                try {
//                    JSONArray idArray = new JSONArray();
//                    idArray.put(swapId);
//                    params.put("id", idArray);
//                    params.put("isConfirmation", true);
//                } catch (JSONException e) {
//                    Log.e("JSONError", "Error while creating JSON body", e);
//                }
//                Log.d("Request Body", params.toString());
//                return params.toString().getBytes();
//            }
//
////            @Override
////            public Map<String, String> getHeaders() {
////                Map<String, String> headers = new HashMap<>();
////                String token = sharedPreferences.getString("token", null);
////                if (!tokenManager.isTokenExpired()) {
////                    headers.put("Authorization", "Bearer " + token);
////                }else {
////                    tokenManager.clearTokenAndLogout();
////                }
////                headers.put("Content-Type", "application/json");
////                return headers;
////            }
//        };
//
//        request.setRetryPolicy(new DefaultRetryPolicy(
//                10 * 1000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//        ));
//
//        Volley.newRequestQueue(this).add(request);
//    }
}