package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
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

        // --- UPDATED: HANDLE CLICKABLE TERMS TEXT ---
        // Instead of complex SpannableString, we just find the separate TextView we made in XML
        TextView tvTermsLink = view.findViewById(R.id.tv_terms_link);

        tvTermsLink.setOnClickListener(v -> {
            // Open TermsAndConditions_fragment
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new TermsAndConditions_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        // ---------------------------------------------

        // 2. Next Button Logic (UNCHANGED)
        btnNext.setOnClickListener(v -> {
            if (!cbTerms.isChecked()) {
                Toast.makeText(getContext(), "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            String password = etPassword.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            // Handle missing email safely
            String email = RegistrationCache.tempEmail;
            if (email == null) email = "user@example.com";

            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            RegistrationCache.tempPassword = password;
            RegistrationCache.tempEmail = email;

            // 3. Initiate Signup
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
}