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

public class LoadOTPActivity extends AppCompatActivity {

    private EditText otpEt;
    private Button submitOtpBtn;
    private String email,otp;
    private LoadingDialog loadingDialog;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_load_otpactivity);
        //get data from intent
        email = getIntent().getStringExtra("email");

        util();

        utilBtn();
    }

    private void util(){
        otpEt = findViewById(R.id.otpEt);
        submitOtpBtn = findViewById(R.id.submitOtpBtn);

        loadingDialog = new LoadingDialog(this);
        requestQueue = Volley.newRequestQueue(this);
    }

    private void utilBtn(){
        submitOtpBtn.setOnClickListener(v -> {
            otp = otpEt.getText().toString().trim();
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "Error: Email is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP sent to your email!", Toast.LENGTH_SHORT).show();
                return;
            }
            loadingDialog.show();
            String url = ApiConstants.ACCOUNT_CHECKCODE;

            StringRequest request = new StringRequest(Request.Method.POST,url,
                    this::response,
                    this::handleError)
            {
                @Override
                public byte[] getBody() throws AuthFailureError {
                    JSONObject params = new JSONObject();
                    try {
                        params.put("email", email);
                        params.put("code", otp);
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

    private void response(String response){
        try {
            JSONObject object = new  JSONObject(response);
            boolean success = object.getBoolean("success");
            if(success){
                Toast.makeText(this,getString(R.string.success_response),Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, NewPasswordActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("code",otp);
                startActivity(intent);
                finish();

            }else{
                Toast.makeText(this,"Code is not correct!",Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.failed_parse_response),Toast.LENGTH_SHORT).show();
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
}