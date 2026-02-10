package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

public class SplashActivity extends BaseActivity {

    private Button btnStart;
    private MaterialButton btnLanguage;
    private MediaPlayer mediaPlayer;
    private boolean isTagalogEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ⭐ 1. START DOWNLOADING TRANSLATION MODEL IMMEDIATELY
        TranslationHelper.downloadModel(this);

        // --- 2. MUSIC SETUP ---
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.intro_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- 3. UI SETUP ---
        btnLanguage = findViewById(R.id.btn_language);
        btnStart = findViewById(R.id.btnStart);

        // Check current preference
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        isTagalogEnabled = prefs.getBoolean("is_tagalog", false);

        // Initial UI Update
        updateLanguageButtonUI();

        // Apply initial translation if needed
        if (isTagalogEnabled) {
            TranslationHelper.translateViewHierarchy(this, getWindow().getDecorView().getRootView());
        }

        // --- 4. TOGGLE LANGUAGE ---
        btnLanguage.setOnClickListener(v -> {
            // Toggle State
            isTagalogEnabled = !isTagalogEnabled;

            // Save Preference
            prefs.edit().putBoolean("is_tagalog", isTagalogEnabled).apply();

            // ⭐ KEY FIX: Call translateViewHierarchy for BOTH cases.
            // The new TranslationHelper will automatically:
            // - Translate to Tagalog if isTagalogEnabled = true
            // - Restore to English if isTagalogEnabled = false
            TranslationHelper.translateViewHierarchy(this, getWindow().getDecorView().getRootView());

            // Update the Button Text explicitly AFTER translation
            // (so the helper doesn't overwrite "TAGALOG" with a translation)
            updateLanguageButtonUI();

            // Optional: Show feedback
            String msg = isTagalogEnabled ? "Tagalog Mode ON" : "English Mode ON";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // --- 5. START BUTTON ---
        btnStart.setOnClickListener(v -> {
            stopMusic();

            // Navigate to Login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateLanguageButtonUI() {
        if (isTagalogEnabled) {
            btnLanguage.setText("TAGALOG");
            btnLanguage.setIconResource(R.drawable.ic_check_circle);
        } else {
            btnLanguage.setText("ENGLISH");
            btnLanguage.setIcon(null);
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
    }
}