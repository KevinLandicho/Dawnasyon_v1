package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    private AlertDialog loadingDialog;
    private ConnectivityManager.NetworkCallback networkCallback;
    private TextView offlineBanner;

    // Track the last time we had internet
    private static long lastOnlineTime = System.currentTimeMillis();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        createLoadingDialog();
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        if (res.getConfiguration().fontScale != 1.0f) {
            Configuration newConfig = new Configuration(res.getConfiguration());
            newConfig.fontScale = 1.0f;
            res.updateConfiguration(newConfig, res.getDisplayMetrics());
        }
        return res;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (offlineBanner == null) {
            setupOfflineBanner();
        }
        registerNetworkCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkCallback();
    }

    // ==========================================================
    // ⭐ SETUP FRIENDLY OFFLINE BANNER
    // ==========================================================
    private void setupOfflineBanner() {
        offlineBanner = new TextView(this);

        // Default text (will be updated dynamically)
        offlineBanner.setText("Offline Mode");

        // ⭐ FRIENDLY COLORS (Amber Background + Dark Text)
        offlineBanner.setTextColor(Color.parseColor("#212121")); // Dark Grey Text
        offlineBanner.setBackgroundColor(Color.parseColor("#FFCA28")); // Amber Yellow

        offlineBanner.setGravity(Gravity.CENTER);
        offlineBanner.setPadding(0, 60, 0, 20); // Top padding for Status Bar
        offlineBanner.setTextSize(13); // Slightly smaller, cleaner font
        offlineBanner.setTypeface(null, android.graphics.Typeface.BOLD);
        offlineBanner.setVisibility(View.GONE);

        // Float above everything
        ViewCompat.setElevation(offlineBanner, 9999f);
        ViewCompat.setTranslationZ(offlineBanner, 9999f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        decorView.addView(offlineBanner, params);

        offlineBanner.bringToFront();
    }

    // ==========================================================
    // ⭐ UPDATE TEXT WITH TIMESTAMP
    // ==========================================================
    private void showOfflineState() {
        runOnUiThread(() -> {
            if (offlineBanner != null) {
                // Format the time: "Jan 30, 4:05 PM"
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                String dateString = sdf.format(new Date(lastOnlineTime));

                offlineBanner.setText("Offline Mode • Last update: " + dateString);

                offlineBanner.setVisibility(View.VISIBLE);
                offlineBanner.bringToFront();
            }
        });
    }

    private void hideOfflineState() {
        runOnUiThread(() -> {
            if (offlineBanner != null && offlineBanner.getVisibility() == View.VISIBLE) {
                offlineBanner.setVisibility(View.GONE);
                // Update the "Last Online" time for next time
                lastOnlineTime = System.currentTimeMillis();
            }
        });
    }

    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkRequest builder = new NetworkRequest.Builder().build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // Update timestamp when internet comes back
                    lastOnlineTime = System.currentTimeMillis();
                    hideOfflineState();
                }

                @Override
                public void onLost(Network network) {
                    showOfflineState();
                }
            };

            cm.registerNetworkCallback(builder, networkCallback);

            if (!isNetworkAvailable()) {
                showOfflineState();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && networkCallback != null) {
                cm.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities actNw = cm.getNetworkCapabilities(network);
        boolean isConnected = actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));

        if (isConnected) {
            lastOnlineTime = System.currentTimeMillis();
        }
        return isConnected;
    }

    // --- Loading Dialog Logic ---
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
}