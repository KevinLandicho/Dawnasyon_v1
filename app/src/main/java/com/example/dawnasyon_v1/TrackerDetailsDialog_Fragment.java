package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TrackerDetailsDialog_Fragment extends DialogFragment {

    private String driveTitle, status, date;

    public static TrackerDetailsDialog_Fragment newInstance(String title, String status, String date) {
        TrackerDetailsDialog_Fragment fragment = new TrackerDetailsDialog_Fragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("status", status);
        args.putString("date", date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            driveTitle = getArguments().getString("title");
            status = getArguments().getString("status");
            date = getArguments().getString("date");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.fragment_tracker_details_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvHeader = view.findViewById(R.id.tv_header_title);
        TextView btnClose = view.findViewById(R.id.btn_close);

        ImageView ivStep1 = view.findViewById(R.id.iv_step1);
        ImageView ivStep2 = view.findViewById(R.id.iv_step2);
        ImageView ivStep3 = view.findViewById(R.id.iv_step3);
        View line1 = view.findViewById(R.id.line1);
        View line2 = view.findViewById(R.id.line2);

        tvHeader.setText(driveTitle);
        btnClose.setOnClickListener(v -> dismiss());

        setupStepper(status, ivStep1, ivStep2, ivStep3, line1, line2);

        // ⭐ CLICK LISTENERS (Updated Texts)
        ivStep1.setOnClickListener(v -> showStepDetails("Step 1: Application Submitted",
                "Your application was received on " + date + ". It has been sent to the Barangay Admin."));

        ivStep2.setOnClickListener(v -> showStepDetails("Step 2: Admin Approval",
                "PENDING: The admin is currently reviewing your household record.\n\nAPPROVED: You are eligible! You will receive a QR Code to present at the venue."));

        ivStep3.setOnClickListener(v -> showStepDetails("Step 3: Claiming Status",
                "READY: Present your QR Code at the distribution site.\n\nCLAIMED: You have successfully received your relief pack."));
    }

    private void setupStepper(String status, ImageView iv1, ImageView iv2, ImageView iv3, View l1, View l2) {
        int colorGreen = Color.parseColor("#4CAF50"); // Completed
        int colorGray = Color.parseColor("#E0E0E0"); // Inactive
        int colorOrange = Color.parseColor("#FF9800"); // In Progress

        // 1. Reset everything to Gray first
        iv1.setColorFilter(colorGray);
        iv2.setColorFilter(colorGray);
        iv3.setColorFilter(colorGray);
        l1.setBackgroundColor(colorGray);
        l2.setBackgroundColor(colorGray);

        // Reset Icons
        iv1.setImageResource(R.drawable.ic_check_circle);
        iv2.setImageResource(R.drawable.ic_circle_outline);
        iv3.setImageResource(R.drawable.ic_circle_outline);

        // ---------------------------------------------------------
        // LOGIC FLOW
        // ---------------------------------------------------------

        // STEP 1: APPLIED (Always Green because record exists)
        iv1.setColorFilter(colorGreen);
        iv1.setImageResource(R.drawable.ic_check_circle);

        if (status.equalsIgnoreCase("Pending")) {
            // STEP 2: PENDING APPROVAL
            l1.setBackgroundColor(colorOrange);       // Line turning Orange
            iv2.setColorFilter(colorOrange);          // Step 2 Orange
            iv2.setImageResource(R.drawable.ic_sync); // Hourglass/Sync Icon
        }
        else if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Ready")) {
            // STEP 2: APPROVED (DONE)
            l1.setBackgroundColor(colorGreen);
            iv2.setColorFilter(colorGreen);
            iv2.setImageResource(R.drawable.ic_check_circle);

            // STEP 3: READY TO CLAIM (WAITING)
            l2.setBackgroundColor(colorOrange);       // Line turning Orange
            iv3.setColorFilter(colorOrange);          // Step 3 Orange
            iv3.setImageResource(R.drawable.ic_circle_outline); // Waiting Circle
        }
        else if (status.equalsIgnoreCase("Claimed")) {
            // STEP 3: CLAIMED (ALL DONE)
            l1.setBackgroundColor(colorGreen);
            iv2.setColorFilter(colorGreen);
            iv2.setImageResource(R.drawable.ic_check_circle);

            l2.setBackgroundColor(colorGreen);
            iv3.setColorFilter(colorGreen);
            iv3.setImageResource(R.drawable.ic_check_circle);
        }
        else if (status.equalsIgnoreCase("Rejected") || status.equalsIgnoreCase("Declined")) {
            // SPECIAL CASE: REJECTED
            l1.setBackgroundColor(Color.RED);
            iv2.setColorFilter(Color.RED);
            // iv2.setImageResource(R.drawable.ic_cancel); // Optional if you have cancel icon
        }
    }

    private void showStepDetails(String title, String message) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }
}