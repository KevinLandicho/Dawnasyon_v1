package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import kotlin.Unit; // Required to work with AuthHelper (Kotlin)

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignin;
    private TextView btnSignup, btnForgot;

    // --- SECURITY: Rate Limiting Variables ---
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30000;
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. CHECK SESSION
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup);
        btnForgot = findViewById(R.id.btnForgot);

        // --- Sign In Button ---
        btnSignin.setOnClickListener(v -> {
            hideKeyboard();
            if (isLockedOut) {
                Toast.makeText(LoginActivity.this, "Too many attempts. Please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateInputs(email, password)) {
                performSupabaseLogin(email, password);
            }
        });

        btnSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
        btnForgot.setOnClickListener(v -> Toast.makeText(this, "Forgot Password feature coming soon!", Toast.LENGTH_SHORT).show());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void performSupabaseLogin(String email, String password) {
        btnSignin.setEnabled(false);
        btnSignin.setText("Verifying...");

        // 1. CALL LOGIN
        AuthHelper.loginUser(email, password, new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                // ⭐ 2. LOGIN SUCCESS -> NOW FETCH PROFILE DATA
                fetchProfileAndSave(email);
            }

            @Override
            public void onError(String message) {
                handleLoginFailure(message);
            }
        });
    }

    // ⭐ THIS IS THE NEW PART THAT SAVES THE FACE DATA
    private void fetchProfileAndSave(String email) {
        // We call the Kotlin function from Java
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null) {
                // Get SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // A. Save Basic Info
                editor.putBoolean("isLoggedIn", true);
                editor.putString("email", email);
                editor.putString("user_id", profile.getId()); // Helper must have getId()

                // B. ⭐ SAVE FACE EMBEDDING FOR OFFLINE VERIFICATION ⭐
                String faceData = profile.getFace_embedding(); // Ensure Profile.kt has this field
                if (faceData != null && !faceData.isEmpty()) {
                    editor.putString("face_embedding", faceData);

                    // Reset the "Last Verified" timer so they aren't asked immediately after login
                    editor.putLong("last_verified_timestamp", System.currentTimeMillis());
                }

                editor.apply();

                // C. Navigate to Dashboard
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    loginAttempts = 0;
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            } else {
                // Fallback: Login worked, but Profile fetch failed
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Welcome! (Could not load profile details)", Toast.LENGTH_SHORT).show();
                    // Still let them in, just without face data
                    saveLoginSession(email);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            return Unit.INSTANCE; // Required when Java calls a Kotlin function
        });
    }

    private void handleLoginFailure(String errorMessage) {
        loginAttempts++;
        int remaining = MAX_LOGIN_ATTEMPTS - loginAttempts;

        btnSignin.setEnabled(true);
        btnSignin.setText("Sign In");

        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            initiateLockout();
        } else {
            Toast.makeText(LoginActivity.this, "Error: " + errorMessage + "\nAttempts left: " + remaining, Toast.LENGTH_LONG).show();
        }
    }

    private void initiateLockout() {
        isLockedOut = true;
        btnSignin.setEnabled(false);
        btnSignin.setAlpha(0.5f);
        Toast.makeText(this, "Maximum login attempts reached. Locked for 30s.", Toast.LENGTH_LONG).show();

        new CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                btnSignin.setText("Locked (" + millisUntilFinished / 1000 + "s)");
            }
            public void onFinish() {
                isLockedOut = false;
                loginAttempts = 0;
                btnSignin.setEnabled(true);
                btnSignin.setAlpha(1.0f);
                btnSignin.setText("Sign In");
            }
        }.start();
    }

    // Backup method for basic save
    private void saveLoginSession(String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("email", email);
        editor.apply();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}