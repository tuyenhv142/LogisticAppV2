package com.example.qr_code_project.ui;

import android.app.Activity;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.example.qr_code_project.R;

public class LoadingDialog {
    private AlertDialog progressDialog;
    private Activity activity;

    public LoadingDialog(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_loading, null));
        builder.setCancelable(false);

        progressDialog = builder.create();
        progressDialog.show();
    }

    public void dismiss() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}