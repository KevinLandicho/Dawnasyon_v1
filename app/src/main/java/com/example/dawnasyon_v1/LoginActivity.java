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

        // ⭐ 1. CHECK SESSION: Is user already logged in?
        // This runs BEFORE setting the layout. If logged in, we skip to Main immediately.
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // User is already logged in! Skip this screen.
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Close LoginActivity so they can't go back
            return; // Stop running the rest of the code
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Views
        etEmail = findViewById(R.id.editTextText);      // Acts as Username field
        etPassword = findViewById(R.id.editTextTextPassword);
        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup);

        // --- Sign In Button Listener (Secured) ---
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                if (isLockedOut) {
                    Toast.makeText(LoginActivity.this, "Too many attempts. Please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }

                String inputUsername = etEmail.getText().toString().trim();
                String inputPassword = etPassword.getText().toString().trim();

                if (validateInputs(inputUsername, inputPassword)) {
                    performSecureLogin(inputUsername, inputPassword);
                }
            }
        });

        // --- Sign Up Button Listener ---
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

    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            etEmail.setError("Username is required");
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

    private void performSecureLogin(String username, String password) {

        // ⭐ DEFENSE DEMO CREDENTIALS ⭐
        boolean isLoginSuccessful = false;

        if (username.equals("kevinlandicho") && password.equals("admin")) {
            isLoginSuccessful = true;
        }

        if (isLoginSuccessful) {
            // SUCCESS: Reset security counter
            loginAttempts = 0;
            saveLoginSession(username); // ⭐ Save session so they stay logged in

            Toast.makeText(LoginActivity.this, "Login Successful. Welcome Kevin!", Toast.LENGTH_SHORT).show();

            // Navigate to Dashboard
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } else {
            // FAILURE: Increment failed attempts
            loginAttempts++;
            int remaining = MAX_LOGIN_ATTEMPTS - loginAttempts;

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                initiateLockout();
            } else {
                Toast.makeText(LoginActivity.this, "Invalid Credentials. " + remaining + " attempts remaining.", Toast.LENGTH_LONG).show();
                etPassword.setText(""); // Clear password field
            }
        }
    }

    private void initiateLockout() {
        isLockedOut = true;
        btnSignin.setEnabled(false);
        btnSignin.setAlpha(0.5f);

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
                btnSignin.setText("Sign In");
                Toast.makeText(LoginActivity.this, "You can try again now.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void saveLoginSession(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
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