package com.example.qr_code_project.activity.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.network.ApiConstants;
import com.example.qr_code_project.data.ui.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NewPasswordActivity extends AppCompatActivity {

    private EditText newPasswordEt,reNewPasswordEt;
    private Button confirmNewPasswordBtn;
    private RequestQueue requestQueue;
    private LoadingDialog loadingDialog;
    private String email,code;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_password);

        email = getIntent().getStringExtra("email");
        code = getIntent().getStringExtra("code");

        util();

        utilBtn();
    }

    private void util(){
        newPasswordEt = findViewById(R.id.newPasswordEt);
        reNewPasswordEt = findViewById(R.id.reNewPasswordEt);
        confirmNewPasswordBtn = findViewById(R.id.confirmNewPasswordBtn);

        loadingDialog = new LoadingDialog(this);
        requestQueue = Volley.newRequestQueue(this);
    }

    private void utilBtn(){
        confirmNewPasswordBtn.setOnClickListener(v -> {
            String password = newPasswordEt.getText().toString();
            String rePassword = reNewPasswordEt.getText().toString();

            if (password.isEmpty() || rePassword.isEmpty()) {
                Toast.makeText(this, "New password cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(rePassword)) {
                Toast.makeText(this, "Password and Re-Password must be the same!", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = ApiConstants.ACCOUNT_UPDATE_PASSWORD;

            loadingDialog.show();
            StringRequest request = new StringRequest(Request.Method.POST,url,
                    this::response,
                    this::handleError)
            {
                @Override
                public byte[] getBody() throws AuthFailureError {
                    JSONObject params = new JSONObject();
                    try {
                        params.put("email", email);
                        params.put("passwordNew", newPasswordEt.getText().toString());
                        params.put("code", code);
                    } catch (JSONException e) {
                        Log.d("LoadOTPActivity", Objects.requireNonNull(e.getMessage()));
                        return null;
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

            request.setRetryPolicy(new DefaultRetryPolicy(
                    10 * 1000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        });
    }

    private void response(String response) {
        try {
            JSONObject object = new JSONObject(response);
            boolean success = object.getBoolean("success");
            if (success) {
                Toast.makeText(this, getString(R.string.success_response), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to update password!", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e("NewPasswordActivity", "JSON Parsing error: " + e.getMessage());
            Toast.makeText(this, getString(R.string.failed_parse_response), Toast.LENGTH_SHORT).show();
        } finally {
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

}