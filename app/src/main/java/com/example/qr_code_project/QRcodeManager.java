package com.example.qr_code_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

public class QRcodeManager extends BroadcastReceiver {
    //Broadcast name
    private final static String ACTION = "android.intent.ACTION_DECODE_DATA";
    //Key
    private final static String RECEIVE_STRING = "barcode_string";

    private final Context context;
    private ScanListener listener;

    public interface ScanListener {
        void scanned(String text);
    }

    public QRcodeManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        initRegister();
    }

    /**
     * Register to receive Broadcast
     */
    private void initRegister() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);

        context.registerReceiver(this, filter);
    }

    /**
     * Unregister to receive Broadcast
     */
    public void unregister() {
        context.unregisterReceiver(this);
    }

    /**
     * Set after scan callback listener
     * @param listener
     */
    public void setListener(ScanListener listener) {
        this.listener = listener;
    }

    /**
     * QR code scan text receive
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null && intent.getAction().equals(ACTION)) {
            String qrCodeText = intent.getStringExtra(RECEIVE_STRING);
            listener.scanned(qrCodeText);
        }
    }
}