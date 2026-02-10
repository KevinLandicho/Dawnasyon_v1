package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ApplicationSuccessDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the new success layout
        View view = inflater.inflate(R.layout.fragment_application_success_dialog, container, false);

        // --- Standard code to make corners round ---
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // --- Find the confirm button ---
        Button btnConfirm = view.findViewById(R.id.btnSuccessConfirm);

        // --- Button Logic ---
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just close the dialog
                dismiss();
            }
        });

        // ⭐ MANUAL TRANSLATION CHECK
        // Since DialogFragment doesn't extend BaseFragment, we check manually here.
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            boolean isTagalog = prefs.getBoolean("is_tagalog", false);

            if (isTagalog) {
                TranslationHelper.translateViewHierarchy(getContext(), view);
            }
        }

        return view;
    }

    // Ensure width is correct
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}