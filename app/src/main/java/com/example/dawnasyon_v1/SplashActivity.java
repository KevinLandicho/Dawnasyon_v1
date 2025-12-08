package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.google.android.material.button.MaterialButton;

// ⭐ CHANGE: Extend BaseActivity if you created it, otherwise keep AppCompatActivity
// If you use BaseActivity, you don't need the attachBaseContext method below.
public class SplashActivity extends BaseActivity {

    private Button btnStart;
    private MaterialButton btnLanguage; // ⭐ The new language button
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // --- 1. MUSIC SETUP ---
        try {
            // Ensure 'intro_sound.mp3' exists in res/raw
            mediaPlayer = MediaPlayer.create(this, R.raw.intro_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- 2. LANGUAGE BUTTON SETUP ---
        btnLanguage = findViewById(R.id.btn_language);
        updateLanguageButtonText(); // Set initial text (EN or TL)

        btnLanguage.setOnClickListener(v -> {
            // Toggle Logic
            String currentLang = LocaleHelper.getLanguage(this);
            String newLang = currentLang.equals("tl") ? "en" : "tl";

            // Save new language
            LocaleHelper.setLocale(this, newLang);

            // Restart this screen to see the change immediately
            recreate();
        });

        // --- 3. START BUTTON SETUP ---
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            stopMusic(); // Stop music before leaving

            // Navigate to Login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close splash so user can't go back
        });
    }

    // Helper to update button text based on current language
    private void updateLanguageButtonText() {
        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("tl")) {
            btnLanguage.setText("TAGALOG");
        } else {
            btnLanguage.setText("ENGLISH");
        }
    }

    // Helper to stop music
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
        stopMusic(); // Ensure music stops if app is closed
    }
}