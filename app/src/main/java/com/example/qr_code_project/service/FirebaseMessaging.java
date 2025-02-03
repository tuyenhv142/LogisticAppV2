package com.example.qr_code_project.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.qr_code_project.repository.TokenRepository;
import com.google.firebase.messaging.FirebaseMessagingService;

public class FirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "New Token: " + token);

        // Gửi token này lên server
        TokenRepository.sendTokenToServer(getApplicationContext(), token);
    }
}
