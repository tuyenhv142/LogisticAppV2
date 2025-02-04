package com.example.qr_code_project.activity.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.SSLHelper;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEt;
    private EditText passwordEt;
    private Button loginBtn;
    private LoadingDialog loadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        SSLHelper.trustAllCertificates();

        util();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEt.getText().toString();
                String password = passwordEt.getText().toString();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Username or password is empty!"
                            , Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        if (token != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void util(){
        usernameEt = findViewById(R.id.usernameEt);
        passwordEt = findViewById(R.id.passwordEt);
        loginBtn = findViewById(R.id.loginBtn);

        loadingDialog = new LoadingDialog(this);
    }

    private void saveLoginInfo(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("token", token);

        editor.apply();
    }

    private void loginUser(String username, String password) {
        String url = ApiConstants.LOGIN;
        loadingDialog.show();
        StringRequest request = new StringRequest(Request.Method.POST, url,
                this::parseResponse,
                this::handleError)
        {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("username", username);
                    params.put("password", password);
                } catch (JSONException e) {
                    e.getMessage();
                }
                Log.d("Request Body", params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json"); //  Content-Type is JSON
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    String token = jsonObject.getJSONObject("content")
                            .getString("token");

                    saveLoginInfo(token);

                    Toast.makeText(LoginActivity.this, "Login successful!"
                            , Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this
                            , MainActivity.class);
                    startActivity(intent);
                }
            } else {
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("Login_response_error", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(Exception error) {
        Log.e("login_error", "API Error", error);
        Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();

        loadingDialog.dismiss();
    }

}