package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import kotlin.Unit;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignin;
    private TextView btnSignup, btnForgot;

    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30000;
    private boolean isLockedOut = false;

    // Retry System for Slow Internet
    private int fetchRetries = 0;
    private static final int MAX_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ 1. FIX: Check Deep Link Immediately
        checkDeepLink(getIntent());

        // 2. Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Views
        etEmail = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup);
        btnForgot = findViewById(R.id.btnForgot);

        // ⭐ NEW: Initialize Barangay Info Button
        View btnBrgyInfo = findViewById(R.id.btnBrgyInfo);
        if (btnBrgyInfo != null) {
            btnBrgyInfo.setOnClickListener(v -> {
                hideKeyboard();
                BrgyInfoDialog dialog = new BrgyInfoDialog();
                dialog.show(getSupportFragmentManager(), "BrgyInfoDialog");
            });
        }

        // Sign In Button Listener
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

        // Sign Up Button Listener
        btnSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        // Forgot Password Listener
        btnForgot.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email first");
                etEmail.requestFocus();
                Toast.makeText(this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button to prevent spamming
            btnForgot.setEnabled(false);

            // ⭐ TRANSLATE LOADING STATE
            String sendingMsg = "Sending...";
            btnForgot.setText(sendingMsg);
            TranslationHelper.autoTranslate(this, btnForgot, sendingMsg);

            SupabaseJavaHelper.sendPasswordResetEmail(email, new SupabaseJavaHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    btnForgot.setEnabled(true);

                    // ⭐ TRANSLATE RESET STATE
                    String resetMsg = "Forgot Password?";
                    btnForgot.setText(resetMsg);
                    TranslationHelper.autoTranslate(LoginActivity.this, btnForgot, resetMsg);

                    Toast.makeText(LoginActivity.this, "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    btnForgot.setEnabled(true);

                    // ⭐ TRANSLATE RESET STATE
                    String resetMsg = "Forgot Password?";
                    btnForgot.setText(resetMsg);
                    TranslationHelper.autoTranslate(LoginActivity.this, btnForgot, resetMsg);

                    Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        // ⭐ FIXED: Added WindowInsetsCompat.Type.ime() to handle the keyboard pushing the layout up!
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insetsToApply = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insetsToApply.left, insetsToApply.top, insetsToApply.right, insetsToApply.bottom);
            return insets;
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        TranslationHelper.translateViewHierarchy(this, findViewById(android.R.id.content));
    }

    // ⭐ FIX: Handle New Intents (If app was already open in background)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkDeepLink(intent);
    }

    // ⭐ FIX: Logic to Catch the Reset Link and Open Dialog
    private void checkDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        AuthHelper.handleDeepLink(intent, () -> {
            runOnUiThread(this::showResetPasswordDialog);
            return Unit.INSTANCE;
        });
    }

    // ⭐ FIX: Show Popup to Enter New Password
    private void showResetPasswordDialog() {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set New Password");
        builder.setMessage("Enter your new password below:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPass = input.getText().toString().trim();
            if (newPass.length() < 6) {
                Toast.makeText(this, "Password too short! Must be 6+ chars.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserPassword(newPass);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.setCancelable(false);
        builder.show();
    }

    // ⭐ FIX: Update Password Helper Call
    private void updateUserPassword(String newPass) {
        AuthHelper.updateUserPassword(newPass, new AuthHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Password Updated! Please Login.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, "Update Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- NORMAL LOGIN LOGIC ---

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return false; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return false; }
        return true;
    }

    private void performSupabaseLogin(String email, String password) {
        btnSignin.setEnabled(false);

        // ⭐ TRANSLATE LOADING STATE
        String verifyingMsg = "Verifying...";
        btnSignin.setText(verifyingMsg);
        TranslationHelper.autoTranslate(this, btnSignin, verifyingMsg);

        fetchRetries = 0; // Reset retries

        SupabaseJavaHelper.loginUser(email, password, new SupabaseJavaHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                // ⭐ TRANSLATE SYNC STATE
                String syncMsg = "Syncing Profile...";
                btnSignin.setText(syncMsg);
                TranslationHelper.autoTranslate(LoginActivity.this, btnSignin, syncMsg);

                fetchProfileWithRetry(email);
            }
            @Override
            public void onError(String message) {
                handleLoginFailure(message);
            }
        });
    }

    // Retry Logic for Slow Internet
    private void fetchProfileWithRetry(String email) {
        SupabaseJavaHelper.fetchUserProfile(this, new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (profile != null) {
                    processProfileAndSave(profile, email);
                } else {
                    handleFetchError(email, "Empty Profile Data");
                }
            }

            @Override
            public void onError(String message) {
                handleFetchError(email, message);
            }
        });
    }

    private void handleFetchError(String email, String message) {
        if (fetchRetries < MAX_RETRIES) {
            fetchRetries++;
            runOnUiThread(() -> {
                // ⭐ TRANSLATE RETRY STATE
                String retryMsg = "Retrying (" + fetchRetries + "/" + MAX_RETRIES + ")...";
                btnSignin.setText(retryMsg);
                TranslationHelper.autoTranslate(this, btnSignin, retryMsg);

                new Handler().postDelayed(() -> fetchProfileWithRetry(email), 2000); // Wait 2s then retry
            });
        } else {
            runOnUiThread(() -> {
                btnSignin.setEnabled(true);

                // ⭐ TRANSLATE RESET STATE
                String signInMsg = "Sign In";
                btnSignin.setText(signInMsg);
                TranslationHelper.autoTranslate(this, btnSignin, signInMsg);

                showErrorDialog("Connection Failed", "Could not download profile. Please check your internet and try again.\n\nError: " + message);
            });
        }
    }

    private void processProfileAndSave(Profile profile, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("isLoggedIn", true);
        editor.putString("email", email);
        editor.putString("user_id", profile.getId());
        editor.putString("user_type", profile.getType());

        // ⭐ SAFE FACE DATA CHECK (Handles List/Object/String)
        Object faceDataObj = profile.getFace_embedding();
        String faceData = (faceDataObj != null) ? faceDataObj.toString() : null;

        // Check if Resident + No Face Data
        if (profile.getType() != null && profile.getType().equalsIgnoreCase("Resident")) {
            if (faceData == null || faceData.length() < 5) {
                runOnUiThread(() -> {
                    btnSignin.setEnabled(true);

                    // ⭐ TRANSLATE RESET STATE
                    String signInMsg = "Sign In";
                    btnSignin.setText(signInMsg);
                    TranslationHelper.autoTranslate(this, btnSignin, signInMsg);

                    showErrorDialog("Face Data Missing", "Your Resident account is missing face data. Please contact admin.");
                });
                return; // 🛑 BLOCK LOGIN
            }
        }

        if (faceData != null && !faceData.isEmpty()) {
            editor.putString("face_embedding", faceData);
            editor.putLong("last_verified_timestamp", System.currentTimeMillis());
            Log.d("LOGIN", "Face Data Saved: " + faceData);
        }

        editor.apply();

        runOnUiThread(() -> {
            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // --- UI HELPERS ---

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleLoginFailure(String errorMessage) {
        loginAttempts++;
        runOnUiThread(() -> {
            btnSignin.setEnabled(true);

            // ⭐ TRANSLATE RESET STATE
            String signInMsg = "Sign In";
            btnSignin.setText(signInMsg);
            TranslationHelper.autoTranslate(this, btnSignin, signInMsg);

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) initiateLockout();
            else Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void initiateLockout() {
        isLockedOut = true;
        btnSignin.setEnabled(false);
        btnSignin.setAlpha(0.5f);
        Toast.makeText(this, "Locked for 30s.", Toast.LENGTH_LONG).show();
        new CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                btnSignin.setText("Locked (" + millisUntilFinished / 1000 + "s)");
            }
            public void onFinish() {
                isLockedOut = false;
                loginAttempts = 0;
                btnSignin.setEnabled(true);
                btnSignin.setAlpha(1.0f);

                // ⭐ TRANSLATE RESET STATE
                String signInMsg = "Sign In";
                btnSignin.setText(signInMsg);
                TranslationHelper.autoTranslate(LoginActivity.this, btnSignin, signInMsg);
            }
        }.start();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}