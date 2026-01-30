package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Color;
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

    // ⭐ FIXED: Now accepts 3 arguments (Title, Status, Date)
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
        // ⭐ FIXED: Using your xml name "fragment_tracker_details_dialog"
        return inflater.inflate(R.layout.fragment_tracker_details_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvHeader = view.findViewById(R.id.tv_header_title);
        TextView btnClose = view.findViewById(R.id.btn_close);

        // Make sure these IDs exist in your XML
        ImageView ivStep1 = view.findViewById(R.id.iv_step1);
        ImageView ivStep2 = view.findViewById(R.id.iv_step2);
        ImageView ivStep3 = view.findViewById(R.id.iv_step3);
        View line1 = view.findViewById(R.id.line1);
        View line2 = view.findViewById(R.id.line2);

        tvHeader.setText(driveTitle);
        btnClose.setOnClickListener(v -> dismiss());

        setupStepper(status, ivStep1, ivStep2, ivStep3, line1, line2);
    }

    private void setupStepper(String status, ImageView iv1, ImageView iv2, ImageView iv3, View l1, View l2) {
        int colorActive = Color.parseColor("#2E7D32"); // Green
        int colorInactive = Color.parseColor("#E0E0E0"); // Gray
        int colorPending = Color.parseColor("#F57C00"); // Orange

        // Reset
        iv1.setColorFilter(colorInactive);
        iv2.setColorFilter(colorInactive);
        iv3.setColorFilter(colorInactive);
        l1.setBackgroundColor(colorInactive);
        l2.setBackgroundColor(colorInactive);

        // Logic
        iv1.setColorFilter(colorActive); // Step 1 always active
        iv1.setImageResource(R.drawable.ic_check_circle);

        if (status.equalsIgnoreCase("Pending")) {
            l1.setBackgroundColor(colorPending);
            iv2.setColorFilter(colorPending);
           // iv2.setImageResource(R.drawable.ic_hourglass);
        }
        else if (status.equalsIgnoreCase("Approved")) {
            l1.setBackgroundColor(colorActive);
            iv2.setColorFilter(colorActive);
            iv2.setImageResource(R.drawable.ic_check_circle);

            l2.setBackgroundColor(colorPending);
            iv3.setColorFilter(colorPending);
           // iv3.setImageResource(R.drawable.ic_qr_code_scanner);
        }
        else if (status.equalsIgnoreCase("Claimed")) {
            l1.setBackgroundColor(colorActive);
            iv2.setColorFilter(colorActive);
            iv2.setImageResource(R.drawable.ic_check_circle);

            l2.setBackgroundColor(colorActive);
            iv3.setColorFilter(colorActive);
            iv3.setImageResource(R.drawable.ic_check_circle);
        }
        else if (status.equalsIgnoreCase("Rejected")) {
            l1.setBackgroundColor(Color.RED);
            iv2.setColorFilter(Color.RED);
          //  iv2.setImageResource(R.drawable.ic_cancel);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}