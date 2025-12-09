package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
// import android.widget.Toast; // Toast is no longer needed here

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ApplyConfirmationDialogFragment extends DialogFragment {

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

        // --- UPDATED PART HERE ---
        btnConfirm.setOnClickListener(v -> {
            // 1. Close the current confirmation dialog
            dismiss();

            // 2. Create and show the new success dialog
            // We use getParentFragmentManager() because we are inside a fragment
            ApplicationSuccessDialogFragment successDialog = new ApplicationSuccessDialogFragment();
            successDialog.show(getParentFragmentManager(), "SuccessDialog");
        });
        // -------------------------

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