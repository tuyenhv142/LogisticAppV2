package com.example.qr_code_project.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.inbound.InboundActivity;
import com.example.qr_code_project.activity.login.LoginActivity;
import com.example.qr_code_project.activity.outbound.OutboundActivity;
import com.example.qr_code_project.activity.packaged.PackageActivity;
import com.example.qr_code_project.activity.swap.SwapLocationActivity;
import com.example.qr_code_project.activity.swap.UnSuccessSwapLocationActivity;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.SSLHelper;
import com.example.qr_code_project.service.TokenRepository;
import com.example.qr_code_project.ui.LoadingDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private TextView usernameTv;
    private ImageView imageAccountIv,logoutBtn,planUnSuccessBtn;
    private ConstraintLayout inboundBtn, outboundBtn
            ,packageBtn,swapProductLocationBtn;
    private RequestQueue requestQueue;
    private LoadingDialog loadingDialog;

    private final ActivityResultLauncher<String> resultLauncher
            = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted->{
                if(isGranted){
                    getDeviceToken();
                }else{

                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SSLHelper.trustAllCertificates();

        util();

        utilButton();

        firebaseMessaging();

        loadAccountProfile();
    }

    private void firebaseMessaging() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)==
                    PackageManager.PERMISSION_GRANTED){
                getDeviceToken();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(this, "You need to grant notification permission to receive messages."
                        , Toast.LENGTH_SHORT).show();
            }else {
                resultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }else {
            getDeviceToken();
        }
    }

    private void getDeviceToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        Log.d("FCM", token);
                        TokenRepository.sendTokenToServer(getApplicationContext(), token);
                    }
                });
    }

    private void util(){
        usernameTv = findViewById(R.id.usernameTv);
        imageAccountIv = findViewById(R.id.imageAccountIv);
        planUnSuccessBtn = findViewById(R.id.planUnSuccessBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        inboundBtn = findViewById(R.id.inboundBtn);
        outboundBtn = findViewById(R.id.outboundBtn);
        packageBtn = findViewById(R.id.packageBtn);
        swapProductLocationBtn = findViewById(R.id.swapProductLocationBtn);

        requestQueue = Volley.newRequestQueue(this);
        loadingDialog = new LoadingDialog(this);
        sharedPreferences = getSharedPreferences("AccountToken"
                , MODE_PRIVATE);
    }

    private void utilButton(){
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);

            }
        });

//        unSuccessSwapLocationsRv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, UnSuccessSwapLocationActivity.class);
//                startActivity(intent);
//            }
//        });

        planUnSuccessBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UnSuccessSwapLocationActivity.class);
            startActivity(intent);
        });

        outboundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OutboundActivity.class);
                startActivity(intent);
            }
        });

        inboundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InboundActivity.class);
                startActivity(intent);

            }
        });

        packageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PackageActivity.class);
            startActivity(intent);
        });

        swapProductLocationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SwapLocationActivity.class);
            startActivity(intent);
        });
    }

    private void loadAccountProfile(){
        loadingDialog.show();
        String url = ApiConstants.ACCOUNT_PROFILE;
        StringRequest request = new StringRequest(
                Request.Method.GET,url,
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
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("responseValue", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }finally {
            if (loadingDialog != null && !isFinishing() && !isDestroyed()) {
                loadingDialog.dismiss();
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {

        String username = content.optString("username", "N/A");
        String image = content.optString("image", "");
        String email = content.optString("email", "N/A");
        String phone = content.optString("phone", "N/A");
        String address = content.optString("address", "N/A");

        usernameTv.setText(email);
        Log.d("img",image);

        try {
            if (!image.isEmpty()) {
                Picasso.get()
                        .load(image)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .fit()
                        .centerCrop()
                        .into(imageAccountIv);
            } else {
                imageAccountIv.setImageResource(R.drawable.ic_user);
            }
        } catch (Exception e) {
            Log.e("PicassoError", "Error loading image", e);
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
        loadingDialog.dismiss();
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

}