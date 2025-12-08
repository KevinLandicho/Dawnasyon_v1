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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnSignin;
    private TextView btnSignup;

    // --- SECURITY: Rate Limiting Variables ---
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 30000; // 30 seconds lock
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Views
        etEmail = findViewById(R.id.editTextText);      // This will act as your Username field
        etPassword = findViewById(R.id.editTextTextPassword);
        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup);

        // --- 1. Sign In Button Listener (Secured) ---
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Security Step: Hide Keyboard immediately so user can see messages
                hideKeyboard();

                // Security Step: Check if user is currently locked out
                if (isLockedOut) {
                    Toast.makeText(LoginActivity.this, "Too many attempts. Please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }

                String inputUsername = etEmail.getText().toString().trim();
                String inputPassword = etPassword.getText().toString().trim();

                // Security Step: Validate Inputs (Check for empty fields)
                if (validateInputs(inputUsername, inputPassword)) {
                    performSecureLogin(inputUsername, inputPassword);
                }
            }
        });

        // --- 2. Sign Up Button Listener ---
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        // Handle Edge-to-Edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * SECURITY: Basic Input Validation
     * Ensures fields are not empty before processing.
     */
    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            etEmail.setError("Username is required");
            etEmail.requestFocus();
            return false;
        }

        // Note: Strict Email regex removed to allow "kevinlandicho" username

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * SECURITY: Login Logic with Rate Limiting
     * Handles authentication and brute-force protection.
     */
    private void performSecureLogin(String username, String password) {

        // ⭐ DEFENSE DEMO CREDENTIALS ⭐
        boolean isLoginSuccessful = false;

        // Hardcoded validation for your demo
        if (username.equals("kevinlandicho") && password.equals("admin")) {
            isLoginSuccessful = true;
        }

        if (isLoginSuccessful) {
            // SUCCESS: Reset security counter
            loginAttempts = 0;
            saveLoginSession(username); // Security: Save session

            Toast.makeText(LoginActivity.this, "Login Successful. Welcome Kevin!", Toast.LENGTH_SHORT).show();

            // Navigate to Dashboard
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            // Clear back stack so user cannot press "Back" to return to login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } else {
            // FAILURE: Increment failed attempts
            loginAttempts++;
            int remaining = MAX_LOGIN_ATTEMPTS - loginAttempts;

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                // Too many fails -> Lock the account
                initiateLockout();
            } else {
                // Allow retry
                Toast.makeText(LoginActivity.this, "Invalid Credentials. " + remaining + " attempts remaining.", Toast.LENGTH_LONG).show();
                etPassword.setText(""); // Clear password field for security
            }
        }
    }

    /**
     * SECURITY: Anti-Brute Force Lockout
     * Locks the login capability for 30 seconds after 3 failed tries.
     */
    private void initiateLockout() {
        isLockedOut = true;
        btnSignin.setEnabled(false); // Disable button visually
        btnSignin.setAlpha(0.5f); // Dim button

        Toast.makeText(this, "Maximum login attempts reached. Locked for 30 seconds.", Toast.LENGTH_LONG).show();

        new CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                btnSignin.setText("Locked (" + millisUntilFinished / 1000 + "s)");
            }

            public void onFinish() {
                isLockedOut = false;
                loginAttempts = 0;
                btnSignin.setEnabled(true);
                btnSignin.setAlpha(1.0f);
                btnSignin.setText("Sign In"); // Reset text
                Toast.makeText(LoginActivity.this, "You can try again now.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    /**
     * SECURITY: Session Management placeholder
     * Stores user login state securely.
     */
    private void saveLoginSession(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.apply();
    }

    /**
     * UX/SECURITY: Hide Keyboard
     * Prevents accidental typing after submission.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}