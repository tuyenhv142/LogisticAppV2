package com.example.qr_code_project.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
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
import com.example.qr_code_project.data.helper.LocaleHelper;
import com.example.qr_code_project.data.manager.LanguageManager;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.helper.SSLHelper;
import com.example.qr_code_project.data.manager.TokenManager;
import com.example.qr_code_project.data.service.TokenRepository;
import com.example.qr_code_project.data.ui.LoadingDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
//    private TextView usernameTv;
//    private ImageView imageAccountIv;
    private ImageView menuButton;
    private ConstraintLayout inboundBtn, outboundBtn
            ,packageBtn,swapProductLocationBtn;
    private RequestQueue requestQueue;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;
    private final Map<String, String> languageMap = new HashMap<>();

    private final ActivityResultLauncher<String> resultLauncher
            = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted->{
                if(isGranted){
                    getDeviceToken();
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String language = LanguageManager.getLanguage(this);
        LocaleHelper.setLocale(this, language);
        setContentView(R.layout.activity_main);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        SSLHelper.trustAllCertificates();

        util();

        utilButton();

        firebaseMessaging();

//        loadAccountProfile();

//        loadSwapUnSuccessPlan();

    }

    //check grant notification permission
    private void firebaseMessaging() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)==
                    PackageManager.PERMISSION_GRANTED){
                getDeviceToken();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(this, getString(R.string.notification)
                        , Toast.LENGTH_SHORT).show();
            }else {
                resultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }else {
            getDeviceToken();
        }
    }

    //send device token
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
//        usernameTv = findViewById(R.id.usernameTv);
//        imageAccountIv = findViewById(R.id.imageAccountIv);
        menuButton = findViewById(R.id.menuButtons);
        inboundBtn = findViewById(R.id.inboundBtn);
        outboundBtn = findViewById(R.id.outboundBtn);
        packageBtn = findViewById(R.id.packageBtn);
        swapProductLocationBtn = findViewById(R.id.swapProductLocationBtn);

        languageMap.put("繁體", "tw");
        languageMap.put("English", "en");
        languageMap.put("Việt Nam", "vi");

        requestQueue = Volley.newRequestQueue(this);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
        sharedPreferences = getSharedPreferences("AccountToken"
                , MODE_PRIVATE);
    }

    private void utilButton(){
        menuButton.setOnClickListener(this::showPopupMenu);

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

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.main_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_change_language) {
                showLanguageDialog();
                return true;
            } else if (itemId == R.id.menu_unsuccess_plan) {
//                startActivity(new Intent(MainActivity.this, UnSuccessSwapLocationActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
//                showLogoutConfirmationDialog();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

//    private void showLogoutConfirmationDialog() {
//        new AlertDialog.Builder(MainActivity.this)
//                .setTitle(getString(R.string.confirm_logout))
//                .setMessage(getString(R.string.sure_logout))
//                .setPositiveButton(getString(R.string.logout), (dialog, which) -> {
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.remove("token");
//                    editor.apply();
//
//                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
////                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//                })
//                .setNegativeButton(getString(R.string.canel), (dialog, which) -> dialog.dismiss())
//                .show();
//    }

    private void showLanguageDialog() {
        String[] languages = languageMap.keySet().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_language))
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = languageMap.get(languages[which]);

                    LanguageManager.saveLanguage(MainActivity.this, selectedLanguage);
                    LocaleHelper.setLocale(MainActivity.this, selectedLanguage);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(getString(R.string.canel), (dialog, which) -> dialog.dismiss())
                .show();
    }

//    private void loadAccountProfile(){
//        loadingDialog.show();
//        String url = ApiConstants.ACCOUNT_PROFILE;
//        StringRequest request = new StringRequest(
//                Request.Method.GET,url,
//                this::responseData,
//                this::handleError
//        ) {
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                String token = sharedPreferences.getString("token", null);
//                if (tokenManager.isTokenExpired()) {
//                    tokenManager.clearTokenAndLogout();
//                } else {
//                    headers.put("Authorization", "Bearer " + token);
//                }
//                headers.put("Content-Type", "application/json");
//                return headers;
//            }
//        };
//
//        request.setRetryPolicy(new DefaultRetryPolicy(
//                10 * 1000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//        ));
//
//        requestQueue.add(request);
//    }

//    @SuppressLint("NotifyDataSetChanged")
//    private void responseData(String response) {
//        try {
//            JSONObject jsonObject = new JSONObject(response);
//            if (jsonObject.getBoolean("success")) {
//                JSONObject content = jsonObject.optJSONObject("content");
//                if (content != null) {
//                    contentAccount(content);
//                }
//            } else {
//                Toast.makeText(this, jsonObject.optString("error"
//                        , getString(R.string.unknown_error)),Toast.LENGTH_SHORT).show();
//            }
//        } catch (JSONException e) {
//            Log.e("responseValue", "Failed to parse JSON response", e);
//            Toast.makeText(this,getString(R.string.login_fail),Toast.LENGTH_SHORT).show();
//        }finally {
//            if (loadingDialog != null && !isFinishing() && !isDestroyed()) {
//                loadingDialog.dismiss();
//            }
//
//        }
//    }

//    @SuppressLint("NotifyDataSetChanged")
//    private void contentAccount(JSONObject content) throws JSONException {
//
//        String username = content.optString("username", "N/A");
//        String image = content.optString("image", "");
//        String email = content.optString("email", "N/A");
//        String phone = content.optString("phone", "N/A");
//        String address = content.optString("address", "N/A");
//
////        usernameTv.setText(email);
//        Log.d("img",image);
//
////        try {
////            if (!image.isEmpty()) {
////                Picasso.get()
////                        .load(image)
////                        .placeholder(R.drawable.ic_user)
////                        .error(R.drawable.ic_user)
////                        .fit()
////                        .centerCrop()
////                        .into(imageAccountIv);
////            } else {
////                imageAccountIv.setImageResource(R.drawable.ic_user);
////            }
////        } catch (Exception e) {
////            Log.e("PicassoError", "Error loading image", e);
////        }
//
//    }

//    private void handleError(Exception error) {
//        String errorMsg = getString(R.string.error_parse);
//        if (error instanceof com.android.volley.TimeoutError) {
//            errorMsg = getString(R.string.error_timeout);
//        } else if (error instanceof com.android.volley.NoConnectionError) {
//            errorMsg = getString(R.string.error_no_connection);
//        }
//        Log.e("API Error", error.getMessage(), error);
//        loadingDialog.dismiss();
//        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
//    }
//
//    private void loadSwapUnSuccessPlan(){
//        String url = ApiConstants.SWAP_LOCATION_CLAIM;
//        loadingDialog.show();
//        StringRequest request = new StringRequest(
//                Request.Method.GET,url,
//                this::responseUnSuccess,
//                this::handleError
//        ) {
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
//
//        request.setRetryPolicy(new DefaultRetryPolicy(
//                10 * 1000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//        ));
//
//        requestQueue.add(request);
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private void responseUnSuccess(String response) {
//        try {
//            JSONObject jsonObject = new JSONObject(response);
//            if (jsonObject.getBoolean("success")) {
//                JSONObject content = jsonObject.optJSONObject("content");
//                if (content != null) {
//                    contentUnSuccess(content);
//                }
//            }
//        } catch (JSONException e) {
//            Log.e("responseValue", "Failed to parse JSON response", e);
//            Toast.makeText(this,getString(R.string.login_fail),Toast.LENGTH_SHORT).show();
//        }finally {
//            loadingDialog.dismiss();
//        }
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private void contentUnSuccess(JSONObject content) throws JSONException {
//        JSONArray swapLocations = content.optJSONArray("data");
//
//        if (swapLocations != null && swapLocations.length() > 0) {
//            showUnSuccessPlanDialog(swapLocations.length());
//        }
//    }
//
//    //send warning have swap plan not done
//    private void showUnSuccessPlanDialog(int quantity) {
//        new AlertDialog.Builder(this)
//                .setTitle(getString(R.string.dialog_title))
//                .setMessage(quantity+" "+getString(R.string.dialog_mes_unsucess))
//                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
//                    Intent intent = new Intent(MainActivity.this,
//                            UnSuccessSwapLocationActivity.class);
//                    startActivity(intent);
//                })
//                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
//                .setCancelable(false)
//                .show();
//    }

}