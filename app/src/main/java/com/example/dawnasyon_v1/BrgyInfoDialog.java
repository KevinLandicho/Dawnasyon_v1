package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class BrgyInfoDialog extends DialogFragment {

    private TextView tvAddress, tvHours, tvContact, tvEmail;
    private TextView tvPunongBrgy, tvCouncilors, tvSkChair;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Removes the default white background so our rounded corners show!
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.dialog_brgy_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        tvAddress = view.findViewById(R.id.tv_address);
        tvHours = view.findViewById(R.id.tv_hours);
        tvContact = view.findViewById(R.id.tv_contact);
        tvEmail = view.findViewById(R.id.tv_email);
        tvPunongBrgy = view.findViewById(R.id.tv_punong_brgy);
        tvCouncilors = view.findViewById(R.id.tv_councilors);
        tvSkChair = view.findViewById(R.id.tv_sk_chair);
        Button btnClose = view.findViewById(R.id.btn_close_dialog);

        // Close Button
        btnClose.setOnClickListener(v -> dismiss());

        // Fetch Data from Database
        fetchData();
    }

    private void fetchData() {
        SupabaseJavaHelper.fetchBrgyInfo(new SupabaseJavaHelper.BrgyInfoCallback() {
            @Override
            public void onSuccess(@NonNull BrgyInfoDTO info) {
                if (isAdded()) {
                    tvAddress.setText(info.getOffice_address() != null ? info.getOffice_address() : "N/A");
                    tvHours.setText(info.getOffice_hours() != null ? info.getOffice_hours() : "N/A");
                    tvContact.setText(info.getContact_numbers() != null ? info.getContact_numbers() : "N/A");
                    tvEmail.setText(info.getEmail() != null ? info.getEmail() : "N/A");
                    tvPunongBrgy.setText(info.getPunong_barangay() != null ? info.getPunong_barangay() : "N/A");
                    tvSkChair.setText(info.getSk_chairperson() != null ? info.getSk_chairperson() : "N/A");

                    // Format Councilors into a bulleted list
                    if (info.getCouncilors() != null && !info.getCouncilors().isEmpty()) {
                        String[] councilorList = info.getCouncilors().split(",");
                        StringBuilder formattedList = new StringBuilder();
                        for (String c : councilorList) {
                            formattedList.append("•  ").append(c.trim()).append("\n\n");
                        }
                        tvCouncilors.setText(formattedList.toString().trim());
                    } else {
                        tvCouncilors.setText("N/A");
                    }
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the dialog wide enough to look good on screen
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}