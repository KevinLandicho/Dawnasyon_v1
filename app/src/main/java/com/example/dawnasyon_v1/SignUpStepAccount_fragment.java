package com.example.dawnasyon_v1;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
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
import androidx.core.content.ContextCompat;

public class SignUpStepAccount_fragment extends BaseFragment {

    private EditText etPassword, etConfirm;
    private CheckBox cbTerms;
    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible = false;

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

        // ⭐ 1. SETUP SHOW/HIDE PASSWORD TOGGLES
        setupPasswordToggle(etPassword);
        setupPasswordToggle(etConfirm);

        // ⭐ 2. TERMS & CONDITIONS LOGIC
        // Initially disable checkbox if terms haven't been viewed
        if (RegistrationCache.hasViewedTerms) {
            cbTerms.setEnabled(true);
        } else {
            cbTerms.setEnabled(false);
            cbTerms.setOnClickListener(v -> {
                if (!cbTerms.isEnabled()) {
                    Toast.makeText(getContext(), "Please read the Terms and Conditions first.", Toast.LENGTH_SHORT).show();
                    cbTerms.setChecked(false); // Force uncheck just in case
                }
            });
        }

        // Handle clicking the link
        tvTermsLink.setOnClickListener(v -> {
            // Mark as viewed in cache
            RegistrationCache.hasViewedTerms = true;

            // Enable immediately for UI feedback
            cbTerms.setEnabled(true);

            // Open TermsAndConditions_fragment
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

    // ⭐ HELPER METHOD: Adds an Eye Icon to the EditText
    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle(EditText editText) {
        // Set initial icon (Eye Closed)
        // Note: android.R.drawable.ic_menu_view is a generic eye icon available in Android
        // You can replace this with R.drawable.ic_eye_on / ic_eye_off if you have custom icons
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);

        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    // Toggle Logic
                    int selectionStart = editText.getSelectionStart();
                    int selectionEnd = editText.getSelectionEnd();

                    if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                        // Show Password
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        // Change icon to "open eye" state if you have one, or tint it
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);
                    } else {
                        // Hide Password
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        // Change icon back
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_secure, 0);
                    }

                    // Restore cursor position
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
        // Re-check state when returning from Terms fragment
        if (RegistrationCache.hasViewedTerms && cbTerms != null) {
            cbTerms.setEnabled(true);
        }
    }
}