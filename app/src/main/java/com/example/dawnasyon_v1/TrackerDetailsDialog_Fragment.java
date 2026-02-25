package com.example.dawnasyon_v1;

import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide; // Ensure Glide is in build.gradle

public class TrackerDetailsDialog_Fragment extends DialogFragment {

    private String driveTitle, status, date, proofUrl;

    public static TrackerDetailsDialog_Fragment newInstance(String title, String status, String date, String proofUrl) {
        TrackerDetailsDialog_Fragment fragment = new TrackerDetailsDialog_Fragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("status", status);
        args.putString("date", date);
        args.putString("proof", proofUrl);
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
            proofUrl = getArguments().getString("proof");
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

        // ⭐ Proof Image Views
        LinearLayout layoutProof = view.findViewById(R.id.layout_proof_container);
        ImageView ivProofImage = view.findViewById(R.id.iv_proof_image);

        tvHeader.setText(driveTitle);
        btnClose.setOnClickListener(v -> dismiss());

        setupStepper(status, ivStep1, ivStep2, ivStep3, line1, line2);

        // ⭐ CLICK LISTENERS
        ivStep1.setOnClickListener(v -> {
            layoutProof.setVisibility(View.GONE);
            showStepDetails("Step 1: Application Submitted",
                    "Your application was received on " + date + ". It has been sent to the Barangay Admin.");
        });

        ivStep2.setOnClickListener(v -> {
            layoutProof.setVisibility(View.GONE);
            showStepDetails("Step 2: Admin Approval",
                    "PENDING: The admin is currently reviewing your household record.\n\nAPPROVED: You are eligible! You will receive a QR Code to present at the venue.");
        });

        ivStep3.setOnClickListener(v -> {
            if (status.equalsIgnoreCase("Claimed") && proofUrl != null && !proofUrl.isEmpty()) {
                // ⭐ Show the Proof Image inside the dialog instead of a popup
                layoutProof.setVisibility(View.VISIBLE);
                Glide.with(this).load(proofUrl).into(ivProofImage);
            } else {
                layoutProof.setVisibility(View.GONE);
                showStepDetails("Step 3: Claiming Status",
                        "READY: Present your QR Code at the distribution site.\n\nCLAIMED: You have successfully received your relief pack.");
            }
        });

        // ⭐ MANUAL TRANSLATION
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            boolean isTagalog = prefs.getBoolean("is_tagalog", false);
            if (isTagalog) {
                TranslationHelper.translateViewHierarchy(getContext(), view);
            }
        }
    }

    private void setupStepper(String status, ImageView iv1, ImageView iv2, ImageView iv3, View l1, View l2) {
        int colorGreen = Color.parseColor("#4CAF50"); // Completed
        int colorGray = Color.parseColor("#E0E0E0"); // Inactive
        int colorOrange = Color.parseColor("#FF9800"); // In Progress

        iv1.setColorFilter(colorGray);
        iv2.setColorFilter(colorGray);
        iv3.setColorFilter(colorGray);
        l1.setBackgroundColor(colorGray);
        l2.setBackgroundColor(colorGray);

        iv1.setImageResource(R.drawable.ic_check_circle);
        iv2.setImageResource(R.drawable.ic_circle_outline);
        iv3.setImageResource(R.drawable.ic_circle_outline);

        iv1.setColorFilter(colorGreen);
        iv1.setImageResource(R.drawable.ic_check_circle);

        if (status.equalsIgnoreCase("Pending")) {
            l1.setBackgroundColor(colorOrange);
            iv2.setColorFilter(colorOrange);
            iv2.setImageResource(R.drawable.ic_sync);
        }
        else if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Ready")) {
            l1.setBackgroundColor(colorGreen);
            iv2.setColorFilter(colorGreen);
            iv2.setImageResource(R.drawable.ic_check_circle);

            l2.setBackgroundColor(colorOrange);
            iv3.setColorFilter(colorOrange);
            iv3.setImageResource(R.drawable.ic_circle_outline);
        }
        else if (status.equalsIgnoreCase("Claimed")) {
            l1.setBackgroundColor(colorGreen);
            iv2.setColorFilter(colorGreen);
            iv2.setImageResource(R.drawable.ic_check_circle);

            l2.setBackgroundColor(colorGreen);
            iv3.setColorFilter(colorGreen);
            iv3.setImageResource(R.drawable.ic_check_circle);
        }
        else if (status.equalsIgnoreCase("Rejected") || status.equalsIgnoreCase("Declined")) {
            l1.setBackgroundColor(Color.RED);
            iv2.setColorFilter(Color.RED);
        }
    }

    private void showStepDetails(String title, String message) {
        if (getContext() == null) return;

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();

        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = prefs.getBoolean("is_tagalog", false);

        if (isTagalog && dialog.getWindow() != null) {
            TranslationHelper.translateViewHierarchy(getContext(), dialog.getWindow().getDecorView());
        }
    }
}