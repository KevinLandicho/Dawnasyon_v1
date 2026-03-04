package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

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

        // 1. Check if user is already logged in
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

        // FORGOT PASSWORD FLOW
        btnForgot.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email first");
                etEmail.requestFocus();
                return;
            }

            btnForgot.setEnabled(false);
            btnForgot.setText("Sending...");

            SupabaseJavaHelper.sendPasswordResetEmail(email, new SupabaseJavaHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        btnForgot.setEnabled(true);
                        btnForgot.setText("Forgot Password?");
                        TranslationHelper.autoTranslate(LoginActivity.this, btnForgot, "Forgot Password?");
                        showOTPInputDialog(email);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        btnForgot.setEnabled(true);
                        btnForgot.setText("Forgot Password?");
                        TranslationHelper.autoTranslate(LoginActivity.this, btnForgot, "Forgot Password?");
                        Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insetsToApply = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insetsToApply.left, insetsToApply.top, insetsToApply.right, insetsToApply.bottom);
            return insets;
        });

        TranslationHelper.translateViewHierarchy(this, findViewById(android.R.id.content));
    }

    // ⭐ STEP 1: STYLED OTP MODAL
    private void showOTPInputDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Custom Styled Title
        TextView title = new TextView(this);
        title.setText("Verify Gmail Code");
        title.setPadding(0, 40, 0, 0);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#27869B"));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        builder.setCustomTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter 6-digit OTP");
        input.setGravity(Gravity.CENTER);
        input.setBackgroundResource(R.drawable.edittext_border);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("VERIFY", null);
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#27869B"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String otp = input.getText().toString().trim();
            if (otp.length() != 6) {
                input.setError("Please enter 6 digits");
                return;
            }

            AuthHelper.verifyResetOTP(email, otp, new AuthHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        showResetPasswordDialog();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> input.setError("Invalid or Expired Code"));
                }
            });
        });
    }

    // ⭐ STEP 2: UPDATED TO USE dialog_reset_password.xml
    private void showResetPasswordDialog() {
        if (isFinishing() || isDestroyed()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Header Style
        TextView title = new TextView(this);
        title.setText("Set Secure Password");
        title.setPadding(0, 40, 0, 0);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#27869B"));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        builder.setCustomTitle(title);

        // ⭐ Inflate your custom XML with the eye icon support
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        builder.setView(dialogView);

        builder.setPositiveButton("UPDATE", null); // Set null to handle manually
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F5901A"));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();

            // ⭐ COMPLEXITY CHECK
            if (!isValidPassword(newPass)) {
                etNewPassword.setError("Password does not meet requirements.");
                return;
            }

            updateUserPassword(newPass);
            dialog.dismiss();
        });
    }

    // ⭐ Password Complexity Validator
    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern digit = Pattern.compile("[0-9]");
        Pattern specialChar = Pattern.compile("[@#$%^&+=!._-]");

        return upperCase.matcher(password).find() &&
                lowerCase.matcher(password).find() &&
                specialChar.matcher(password).find();
    }

    private void updateUserPassword(String newPass) {
        AuthHelper.updateUserPassword(newPass, new AuthHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Password Updated Successfully!", Toast.LENGTH_LONG).show();
                    getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
                    etPassword.setText("");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed: " + message, Toast.LENGTH_LONG).show());
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
        btnSignin.setText("Verifying...");

        fetchRetries = 0;

        SupabaseJavaHelper.loginUser(email, password, new SupabaseJavaHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    btnSignin.setText("Syncing Profile...");
                    fetchProfileWithRetry(email);
                });
            }
            @Override
            public void onError(String message) {
                handleLoginFailure(message);
            }
        });
    }

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
                btnSignin.setText("Retrying (" + fetchRetries + "/" + MAX_RETRIES + ")...");
                new Handler().postDelayed(() -> fetchProfileWithRetry(email), 2000);
            });
        } else {
            runOnUiThread(() -> {
                btnSignin.setEnabled(true);
                btnSignin.setText("Sign In");
                showErrorDialog("Connection Failed", "Could not download profile. Please check your internet.");
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

        Object faceDataObj = profile.getFace_embedding();
        String faceData = (faceDataObj != null) ? faceDataObj.toString() : null;

        if (profile.getType() != null && profile.getType().equalsIgnoreCase("Resident")) {
            if (faceData == null || faceData.length() < 5) {
                runOnUiThread(() -> {
                    btnSignin.setEnabled(true);
                    btnSignin.setText("Sign In");
                    showErrorDialog("Face Data Missing", "Your account is missing biometric data. Contact admin.");
                });
                return;
            }
        }

        if (faceData != null && !faceData.isEmpty()) {
            editor.putString("face_embedding", faceData);
            editor.putLong("last_verified_timestamp", System.currentTimeMillis());
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
            btnSignin.setText("Sign In");

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) initiateLockout();
            else Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void initiateLockout() {
        isLockedOut = true;
        btnSignin.setEnabled(false);
        btnSignin.setAlpha(0.5f);
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

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}