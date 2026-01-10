package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ApplyConfirmationDialogFragment extends DialogFragment {

    // 1. Add a variable to hold the action
    private Runnable confirmListener;

    // 2. Method to allow Home_fragment to set what happens on confirm
    public void setOnConfirmListener(Runnable listener) {
        this.confirmListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apply_confirmation_dialog, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }

        Button btnCancel = view.findViewById(R.id.btnCancelApply);
        Button btnConfirm = view.findViewById(R.id.btnConfirmApply);

        btnCancel.setOnClickListener(v -> dismiss());

        // 3. Update the Confirm Button Logic
        btnConfirm.setOnClickListener(v -> {
            if (confirmListener != null) {
                // Run the logic passed from Home_fragment (Database call)
                confirmListener.run();
            }
            // Note: We DO NOT dismiss() here. We wait for Home_fragment
            // to tell us if the application was successful.
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}