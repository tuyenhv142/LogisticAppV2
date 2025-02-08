package com.example.qr_code_project.activity.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.data.ui.LoadingDialog;

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText enterEmailEt;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private LoadingDialog loadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);

        util();
    }

    private void util(){
        enterEmailEt = findViewById(R.id.enterEmailEt);
        sharedPreferences = getSharedPreferences("AccountToken",MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);

    }

}