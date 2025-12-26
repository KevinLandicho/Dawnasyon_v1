package com.example.dawnasyon_v1;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SignUpStepAccount_fragment extends BaseFragment {

    private boolean isTermsOpened = false;
    private Button btnSubmit;

    public SignUpStepAccount_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        CheckBox cbTerms = view.findViewById(R.id.cb_terms);
        btnSubmit = view.findViewById(R.id.btn_submit);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        EditText etPassword = view.findViewById(R.id.et_pass);
        EditText etConfirm = view.findViewById(R.id.et_confirm);

        // --- 1. SETUP CLICKABLE LINK ---
        setupClickableTerms(cbTerms);

        cbTerms.setOnClickListener(v -> {
            if (!isTermsOpened && cbTerms.isChecked()) {
                cbTerms.setChecked(false);
                Toast.makeText(getContext(), "Please read the Terms and Conditions first.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 2. SUBMIT LOGIC ---
        btnSubmit.setOnClickListener(v -> {
            // A. Validation
            if (!cbTerms.isChecked()) {
                Toast.makeText(getContext(), "Please agree to the Terms and Conditions.", Toast.LENGTH_SHORT).show();
                return;
            }

            String p1 = etPassword.getText().toString();
            String p2 = etConfirm.getText().toString();

            if (p1.isEmpty() || p1.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p1.equals(p2)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ CRITICAL: Save Password to Cache
            RegistrationCache.tempPassword = p1;

            // ⭐ CRITICAL: Check if we have the email from Step 1
            if (RegistrationCache.tempEmail.isEmpty()) {
                Toast.makeText(getContext(), "Error: Email missing from Step 1. Please restart.", Toast.LENGTH_LONG).show();
                return;
            }

            // B. Disable Button & Show Loading
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Creating Account...");

            // C. ⭐ EXECUTE SUPABASE REGISTRATION ⭐
            // We pass 'requireContext()' so the helper can read the ID image
            SupabaseRegistrationHelper.registerCompleteUser(requireContext(), new SupabaseRegistrationHelper.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                    // Clear Cache for security
                    RegistrationCache.clear();
                    FamilyDataRepository.clearData();

                    // Navigate to OTP or Login
                    if (isAdded() && getParentFragmentManager() != null) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container_signup, new SignUpOTP_fragment())
                                .commit();
                    }
                }

                @Override
                public void onError(String message) {
                    // Re-enable button on failure
                    if (getContext() != null) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                        Toast.makeText(getContext(), "Registration Failed: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        btnPrevious.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupClickableTerms(CheckBox checkBox) {
        String fullText = "I have read and agree to the Terms and Conditions.";
        SpannableString spannableString = new SpannableString(fullText);

        String targetText = "Terms and Conditions";
        int startIndex = fullText.indexOf(targetText);
        int endIndex = startIndex + targetText.length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                widget.cancelPendingInputEvents();

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container_signup, new TermsAndConditions_fragment())
                            .addToBackStack(null)
                            .commit();
                }
                isTermsOpened = true;
                checkBox.setChecked(true);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.parseColor("#F5901A"));
            }
        };

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        checkBox.setText(spannableString);
        checkBox.setMovementMethod(LinkMovementMethod.getInstance());
        checkBox.setHighlightColor(Color.TRANSPARENT);
    }
}