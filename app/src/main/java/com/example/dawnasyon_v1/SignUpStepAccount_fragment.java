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
import androidx.fragment.app.Fragment;

public class SignUpStepAccount_fragment extends Fragment {

    // Flag to track if terms were actually opened
    private boolean isTermsOpened = false;

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

        CheckBox cbTerms = view.findViewById(R.id.cb_terms);
        Button btnSubmit = view.findViewById(R.id.btn_submit); // Ensure XML ID matches this
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        EditText etPassword = view.findViewById(R.id.et_pass);
        EditText etConfirm = view.findViewById(R.id.et_confirm);

        // --- 1. SETUP CLICKABLE LINK ---
        setupClickableTerms(cbTerms);

        // Optional: Prevent manual checking if they haven't clicked the link
        cbTerms.setOnClickListener(v -> {
            if (!isTermsOpened && cbTerms.isChecked()) {
                cbTerms.setChecked(false); // Revert check
                Toast.makeText(getContext(), "Please click 'Terms and Conditions' to read them first.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 2. SUBMIT LOGIC ---
        btnSubmit.setOnClickListener(v -> {
            // Check 1: Terms Validation
            if (!cbTerms.isChecked()) {
                Toast.makeText(getContext(), "Please Read the Terms and Conditions first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check 2: Passwords Validation
            String p1 = etPassword.getText().toString();
            String p2 = etConfirm.getText().toString();

            if (p1.isEmpty() || p1.length() < 8) {
                Toast.makeText(getContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p1.equals(p2)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: SAVE DATA TO DATABASE (Temporarily)

            Toast.makeText(getContext(), "Details saved. Proceeding to OTP...", Toast.LENGTH_SHORT).show();

            // ⭐ NAVIGATE TO OTP FRAGMENT ⭐
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpOTP_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
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

                // Open Terms Fragment
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container_signup, new TermsAndConditions_fragment())
                            .addToBackStack(null)
                            .commit();
                }

                // Mark terms as opened and auto-check the box
                isTermsOpened = true;
                checkBox.setChecked(true);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.parseColor("#F5901A")); // Orange Color
            }
        };

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        checkBox.setText(spannableString);
        checkBox.setMovementMethod(LinkMovementMethod.getInstance());
        checkBox.setHighlightColor(Color.TRANSPARENT);
    }
}