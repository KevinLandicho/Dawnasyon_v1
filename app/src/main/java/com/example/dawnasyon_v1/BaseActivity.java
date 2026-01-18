package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private AlertDialog noInternetDialog;
    private NetworkReceiver networkReceiver;

    // ⭐ EXISTING LANGUAGE LOGIC
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    // ⭐ NEW INTERNET CHECK LOGIC
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNoInternetDialog(); // Prepare the dialog
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver to listen for real-time changes
        networkReceiver = new NetworkReceiver();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // Immediate check when activity opens
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetDialog();
        } else {
            dismissNoInternetDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister to prevent memory leaks
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- DIALOG LOGIC ---

    private void createNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        // Ensure you have created dialog_no_internet.xml
        View dialogView = inflater.inflate(R.layout.dialog_no_internet, null);
        builder.setView(dialogView);

        // ⭐ CRITICAL: PREVENTS CLOSING
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

    // --- BROADCAST RECEIVER ---
    // Listens for system network changes (Wi-Fi on/off)
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