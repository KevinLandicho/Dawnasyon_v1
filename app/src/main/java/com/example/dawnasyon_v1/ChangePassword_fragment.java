package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChangePassword_fragment extends BaseFragment {

    private Button btnUpdate;
    private EditText etOldPass, etNewPass, etConfirmPass;

    public ChangePassword_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnUpdate = view.findViewById(R.id.btn_update_password);

        etOldPass = view.findViewById(R.id.et_old_password);
        etNewPass = view.findViewById(R.id.et_new_password);
        etConfirmPass = view.findViewById(R.id.et_confirm_new_password);

        // Back Button -> Go back to Profile
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Update Logic
        btnUpdate.setOnClickListener(v -> handlePasswordUpdate());
    }

    private void handlePasswordUpdate() {
        String oldPass = etOldPass.getText().toString().trim();
        String newPass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        // 1. Basic Empty Checks
        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Check Matching
        if (!newPass.equals(confirmPass)) {
            etConfirmPass.setError("Passwords do not match");
            etConfirmPass.requestFocus();
            return;
        }

        // 3. Check Same as Old
        if (oldPass.equals(newPass)) {
            etNewPass.setError("New password cannot be the same as the old one");
            etNewPass.requestFocus();
            return;
        }

        // ⭐ STRICT SECURITY CHECKS START ⭐

        // Check Length (Min 8)
        if (newPass.length() < 8) {
            etNewPass.setError("Must be at least 8 characters");
            etNewPass.requestFocus();
            return;
        }

        // Check Uppercase
        if (!newPass.matches(".*[A-Z].*")) {
            etNewPass.setError("Must contain at least one Uppercase letter (A-Z)");
            etNewPass.requestFocus();
            return;
        }

        // Check Lowercase
        if (!newPass.matches(".*[a-z].*")) {
            etNewPass.setError("Must contain at least one Lowercase letter (a-z)");
            etNewPass.requestFocus();
            return;
        }

        // Check Number
        if (!newPass.matches(".*[0-9].*")) {
            etNewPass.setError("Must contain at least one Number (0-9)");
            etNewPass.requestFocus();
            return;
        }

        // Check Special Character
        if (!newPass.matches(".*[@#$%^&+=!._-].*")) {
            etNewPass.setError("Must contain at least one Special Character (@, #, $, etc.)");
            etNewPass.requestFocus();
            return;
        }

        // ⭐ SECURITY CHECKS END ⭐

        // 4. Disable button to prevent double clicks
        btnUpdate.setEnabled(false);
        btnUpdate.setText("Updating...");

        // 5. Call AuthHelper
        AuthHelper.changePassword(oldPass, newPass, new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("Update Password");

                    Toast.makeText(getContext(), "✅ Password updated successfully!", Toast.LENGTH_LONG).show();
                    getParentFragmentManager().popBackStack(); // Go back to Profile
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("Update Password");

                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}