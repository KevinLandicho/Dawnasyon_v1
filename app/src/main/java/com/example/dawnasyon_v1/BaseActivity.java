package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    private AlertDialog noInternetDialog;
    private AlertDialog loadingDialog;
    private NetworkReceiver networkReceiver;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // ⭐ FIX 1: Force Light Mode to prevent UI colors breaking
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        createNoInternetDialog();
        createLoadingDialog();
    }

    // ⭐ FIX 2: Force Font Scale to 1.0x to prevent layout rumble
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        if (res.getConfiguration().fontScale != 1.0f) {
            Configuration newConfig = new Configuration(res.getConfiguration());
            newConfig.fontScale = 1.0f; // Reset to standard size
            res.updateConfiguration(newConfig, res.getDisplayMetrics());
        }
        return res;
    }

    @Override
    protected void onResume() {
        super.onResume();
        networkReceiver = new NetworkReceiver();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetDialog();
        } else {
            dismissNoInternetDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- NO INTERNET LOGIC ---
    private void createNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_no_internet, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button btnRetry = dialogView.findViewById(R.id.btn_retry);
        btnRetry.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                dismissNoInternetDialog();
            }
        });
        noInternetDialog = builder.create();
    }

    public void showNoInternetDialog() {
        if (noInternetDialog != null && !noInternetDialog.isShowing() && !isFinishing()) {
            noInternetDialog.show();
        }
    }

    public void dismissNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }
    }

    // --- LOADING DIALOG LOGIC ---
    private void createLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    public void showLoading() {
        if (loadingDialog != null && !loadingDialog.isShowing() && !isFinishing()) {
            loadingDialog.show();
        }
    }

    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // --- BROADCAST RECEIVER ---
    class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                showNoInternetDialog();
            } else {
                dismissNoInternetDialog();
            }
        }
    }
}