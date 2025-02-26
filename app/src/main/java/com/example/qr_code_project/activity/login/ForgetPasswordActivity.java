package com.example.qr_code_project.activity.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText enterEmailEt;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private LoadingDialog loadingDialog;
    private Button btnSendNow;
    private String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);

        util();

//        utilBtn();
    }

    private void util(){
        enterEmailEt = findViewById(R.id.enterEmailEt);
        btnSendNow = findViewById(R.id.btnSendNow);
        sharedPreferences = getSharedPreferences("AccountToken",MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        loadingDialog = new LoadingDialog(this);

    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


//    private void utilBtn(){
//        btnSendNow.setOnClickListener(v -> {
//            email = enterEmailEt.getText().toString().trim();
//            if (email.isEmpty()){
//                Toast.makeText(this,"Please enter your email!",Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (!isValidEmail(email)) {
//                Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            String url = ApiConstants.ACCOUNT_LOAD_OTP;
//
//            loadingDialog.show();
//
//            StringRequest request = new StringRequest(Request.Method.POST,url,
//                    this::response,
//                    this::handleError)
//            {
//                @Override
//                public byte[] getBody() throws AuthFailureError {
//                    JSONObject params = new JSONObject();
//                    try {
//                        params.put("email", email);
//                        params.put("type", "update password");
//                    } catch (JSONException e) {
//                        Log.d("ForgetPasswordActivity", Objects.requireNonNull(e.getMessage()));
//                        return null;
//                    }
//                    Log.d("Request Body", params.toString());
//                    return params.toString().getBytes();
//                }
//
//                @Override
//                public Map<String, String> getHeaders() throws AuthFailureError {
//                    Map<String, String> headers = new HashMap<>();
//                    headers.put("Content-Type", "application/json"); //  Content-Type is JSON
//                    return headers;
//                }
//            };
//
//            request.setRetryPolicy(new DefaultRetryPolicy(
//                    10 * 1000,
//                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//            ));
//
//            requestQueue.add(request);
//        });
//    }

    private void response(String response){
        try {
            JSONObject object = new JSONObject(response);
            boolean success = object.getBoolean("success");
            if (success){
                Toast.makeText(this,getString(R.string.success_response),Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoadOTPActivity.class);
                intent.putExtra("email",email);
                startActivity(intent);
                finish();
            }else{
                Toast.makeText(this,"You have sent too many requests recently, " +
                        "please try again later!",Toast.LENGTH_SHORT).show();
            }

        }catch (JSONException e) {
            Log.e("Login_response_error", "Failed to parse JSON response", e);
            Toast.makeText(this,getString(R.string.login_fail),Toast.LENGTH_SHORT).show();
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