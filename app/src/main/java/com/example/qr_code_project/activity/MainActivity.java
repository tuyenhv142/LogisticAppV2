package com.example.qr_code_project.activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.inbound.InboundActivity;
import com.example.qr_code_project.activity.login.LoginActivity;
import com.example.qr_code_project.activity.outbound.OutboundActivity;
import com.example.qr_code_project.activity.packaged.PackageActivity;
import com.example.qr_code_project.activity.swap.SwapLocationActivity;
import com.example.qr_code_project.repository.TokenRepository;
import com.example.qr_code_project.ui.LoadingDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private QRcodeManager qrcodeManager;
    private SharedPreferences sharedPreferences;

    private TextView qrCodeTextView = null;
    private TextView usernameTv;
    private Button inboundBtn;
    private Button outboundBtn;
    private Button packageBtn;
    private Button logoutBtn;
    private Button swapProductLocationBtn;
    private LoadingDialog loadingDialog;

    private final ActivityResultLauncher<String> resultLauncher = registerForActivityResult(
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

        util();

        utilButton();

        firebaseMessaging();
    }

    private void firebaseMessaging() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)==
                    PackageManager.PERMISSION_GRANTED){
                getDeviceToken();
                //request have notification
//                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
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
//        qrCodeTextView = findViewById(R.id.qrCodeTextView);
        usernameTv = findViewById(R.id.usernameTv);
        inboundBtn = findViewById(R.id.inboundBtn);
        outboundBtn = findViewById(R.id.outboundBtn);
        packageBtn = findViewById(R.id.packageBtn);
        swapProductLocationBtn = findViewById(R.id.swapProductLocationBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        qrcodeManager = new QRcodeManager(this);
        usernameTv.setText(sharedPreferences.getString("token","N/A"));
        loadingDialog = new LoadingDialog(this);
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

}