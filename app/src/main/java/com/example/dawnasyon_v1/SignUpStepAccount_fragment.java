package com.example.dawnasyon_v1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SignUpStepAccount_fragment extends BaseFragment {

    private EditText etPassword, etConfirm;
    private CheckBox cbTerms;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPassword = view.findViewById(R.id.et_pass);
        etConfirm = view.findViewById(R.id.et_confirm);
        cbTerms = view.findViewById(R.id.cb_terms);
        Button btnNext = view.findViewById(R.id.btn_submit);
        Button btnPrevious = view.findViewById(R.id.btn_previous);
        TextView tvTermsLink = view.findViewById(R.id.tv_terms_link);

        // 1. SETUP SHOW/HIDE PASSWORD TOGGLES
        setupPasswordToggle(etPassword);
        setupPasswordToggle(etConfirm);

        // 2. TERMS & CONDITIONS LOGIC
        if (RegistrationCache.hasViewedTerms) {
            cbTerms.setEnabled(true);
        } else {
            cbTerms.setEnabled(false);
            cbTerms.setOnClickListener(v -> {
                if (!cbTerms.isEnabled()) {
                    Toast.makeText(getContext(), "Please read the Terms and Conditions first.", Toast.LENGTH_SHORT).show();
                    cbTerms.setChecked(false);
                }
            });
        }

        tvTermsLink.setOnClickListener(v -> {
            RegistrationCache.hasViewedTerms = true;
            cbTerms.setEnabled(true);
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new TermsAndConditions_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // 3. Next Button Logic
        btnNext.setOnClickListener(v -> {
            if (!RegistrationCache.hasViewedTerms) {
                Toast.makeText(getContext(), "Please read the Terms and Conditions first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbTerms.isChecked()) {
                Toast.makeText(getContext(), "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            String password = etPassword.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();
            String email = RegistrationCache.tempEmail != null ? RegistrationCache.tempEmail : "user@example.com";

            // ⭐ PASSWORD SECURITY CHECKS START HERE ⭐

            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Length (Min 8)
            if (password.length() < 8) {
                Toast.makeText(getContext(), "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Uppercase
            if (!password.matches(".*[A-Z].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Uppercase letter (A-Z).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Lowercase
            if (!password.matches(".*[a-z].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Lowercase letter (a-z).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Number
            if (!password.matches(".*[0-9].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Number (0-9).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Special Character (@, #, $, %, etc.)
            if (!password.matches(".*[@#$%^&+=!._-].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Special Character (e.g., @ # $ %)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if passwords match
            if (!password.equals(confirm)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ SECURITY CHECKS END ⭐

            RegistrationCache.tempPassword = password;
            RegistrationCache.tempEmail = email;

            // 4. Initiate Signup
            btnNext.setEnabled(false);
            btnNext.setText("Sending OTP...");

            String finalEmail = email;
            AuthHelper.initiateSignUp(new AuthHelper.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    if (getContext() == null) return;
                    btnNext.setEnabled(true);
                    btnNext.setText("Next");
                    Toast.makeText(getContext(), "OTP Sent to " + finalEmail, Toast.LENGTH_SHORT).show();

                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container_signup, new SignUpOTP_fragment())
                                .addToBackStack(null)
                                .commit();
                    }
                }

                @Override
                public void onError(String message) {
                    if (getContext() == null) return;
                    btnNext.setEnabled(true);
                    btnNext.setText("Next");
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnPrevious.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle(EditText editText) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);

        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    int selectionStart = editText.getSelectionStart();
                    int selectionEnd = editText.getSelectionEnd();

                    if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);
                    } else {
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_secure, 0);
                    }

                    editText.setSelection(selectionStart, selectionEnd);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (RegistrationCache.hasViewedTerms && cbTerms != null) {
            cbTerms.setEnabled(true);
        }
    }
}