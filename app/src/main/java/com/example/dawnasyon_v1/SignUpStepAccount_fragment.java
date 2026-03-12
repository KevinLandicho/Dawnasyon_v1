package com.example.dawnasyon_v1;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

public class SignUpStepAccount_fragment extends BaseFragment {

    private TextInputEditText etPassword, etConfirm;
    private CheckBox cbTerms;

    // Requirement text views
    private TextView tvReqUppercase, tvReqLowercase, tvReqNumber, tvReqSpecial, tvReqLength;

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

        tvReqUppercase = view.findViewById(R.id.tv_req_uppercase);
        tvReqLowercase = view.findViewById(R.id.tv_req_lowercase);
        tvReqNumber = view.findViewById(R.id.tv_req_number);
        tvReqSpecial = view.findViewById(R.id.tv_req_special);
        tvReqLength = view.findViewById(R.id.tv_req_length);

        // 1. DYNAMIC PASSWORD REQUIREMENTS CHECKER
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String pass = s.toString();

                // Check Uppercase
                if (pass.matches(".*[A-Z].*")) {
                    tvReqUppercase.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    tvReqUppercase.setTextColor(Color.parseColor("#D32F2F")); // Red
                }

                // Check Lowercase
                if (pass.matches(".*[a-z].*")) {
                    tvReqLowercase.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    tvReqLowercase.setTextColor(Color.parseColor("#D32F2F")); // Red
                }

                // Check Number
                if (pass.matches(".*[0-9].*")) {
                    tvReqNumber.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    tvReqNumber.setTextColor(Color.parseColor("#D32F2F")); // Red
                }

                // Check Special Character
                if (pass.matches(".*[@#$%^&+=!._-].*")) {
                    tvReqSpecial.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    tvReqSpecial.setTextColor(Color.parseColor("#D32F2F")); // Red
                }

                // Check Length
                if (pass.length() >= 8) {
                    tvReqLength.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    tvReqLength.setTextColor(Color.parseColor("#D32F2F")); // Red
                }
            }
        });

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
            cbTerms.setChecked(true);

            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new TermsAndConditions_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // 3. NEXT BUTTON LOGIC
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

            // ⭐ PASSWORD SECURITY CHECKS ⭐
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 8) {
                Toast.makeText(getContext(), "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches(".*[A-Z].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Uppercase letter (A-Z).", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches(".*[a-z].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Lowercase letter (a-z).", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches(".*[0-9].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Number (0-9).", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches(".*[@#$%^&+=!._-].*")) {
                Toast.makeText(getContext(), "Password must contain at least one Special Character (e.g., @ # $ %)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            RegistrationCache.tempPassword = password;
            RegistrationCache.tempEmail = email;

            // 4. INITIATE SIGNUP
            btnNext.setEnabled(false);

            String loadingText = "Sending OTP...";
            btnNext.setText(loadingText);
            TranslationHelper.autoTranslate(getContext(), btnNext, loadingText);

            String finalEmail = email;
            AuthHelper.initiateSignUp(new AuthHelper.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    if (getContext() == null) return;
                    btnNext.setEnabled(true);

                    String nextText = "Next";
                    btnNext.setText(nextText);
                    TranslationHelper.autoTranslate(getContext(), btnNext, nextText);

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

                    String nextText = "Next";
                    btnNext.setText(nextText);
                    TranslationHelper.autoTranslate(getContext(), btnNext, nextText);

                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnPrevious.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
        });

        applyTagalogTranslation(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (RegistrationCache.hasViewedTerms && cbTerms != null) {
            cbTerms.setEnabled(true);
        }
    }
}