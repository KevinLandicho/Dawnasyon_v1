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
import androidx.fragment.app.Fragment;

public class ChangePassword_fragment extends Fragment {

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
        Button btnUpdate = view.findViewById(R.id.btn_update_password);

        EditText etOldPass = view.findViewById(R.id.et_old_password);
        EditText etNewPass = view.findViewById(R.id.et_new_password);
        EditText etConfirmPass = view.findViewById(R.id.et_confirm_new_password);

        // Back Button -> Go back to Profile
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Update Logic
        btnUpdate.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Add logic to verify old password and save new one to database

            Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack(); // Go back to Profile
        });
    }
}