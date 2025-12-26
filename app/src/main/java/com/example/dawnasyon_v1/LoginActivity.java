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

public class LoginActivity extends AppCompatActivity { // Changed to AppCompatActivity for consistency

    private TextInputEditText etEmail, etPassword;
    private Button btnSignin;
    private TextView btnSignup, btnForgot;

    // --- SECURITY: Rate Limiting Variables ---
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 5; // Increased slightly for real world usage
    private static final long LOCKOUT_DURATION_MS = 30000; // 30 seconds lock
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ 1. CHECK SESSION: Is user already logged in?
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Views (Updated to match your XML IDs and Types)
        etEmail = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup); // "Sign up here" link
        btnForgot = findViewById(R.id.btnForgot);

        // --- Sign In Button Listener ---
        btnSignin.setOnClickListener(v -> {
            hideKeyboard();

            // Security Check
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

        // --- Sign Up Link ---
        btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        // --- Forgot Password Link ---
        btnForgot.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Handle Edge-to-Edge
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
        // UI Feedback
        btnSignin.setEnabled(false);
        btnSignin.setText("Verifying...");

        // ⭐ CALL THE KOTLIN HELPER ⭐
        SupabaseRegistrationHelper.loginUser(email, password, new SupabaseRegistrationHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                // SUCCESS LOGIC
                loginAttempts = 0;
                saveLoginSession(email);

                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                // Navigate to Main Dashboard
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                // FAILURE LOGIC
                handleLoginFailure(message);
            }
        });
    }

    private void handleLoginFailure(String errorMessage) {
        loginAttempts++;
        int remaining = MAX_LOGIN_ATTEMPTS - loginAttempts;

        // Reset Button UI
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
                Toast.makeText(LoginActivity.this, "You can try again now.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void saveLoginSession(String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("email", email); // Saved email instead of username
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