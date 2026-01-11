package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationDetail_fragment extends BaseFragment {

    // Constants for Arguments
    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";
    private static final String ARG_TIME = "time";
    private static final String ARG_TYPE = "type";
    // ✅ New Arguments
    private static final String ARG_SENDER = "sender";
    private static final String ARG_RAW_DATE = "raw_date";

    public NotificationDetail_fragment() {}

    // Factory method to create instance
    public static NotificationDetail_fragment newInstance(NotificationItem item) {
        NotificationDetail_fragment fragment = new NotificationDetail_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_BODY, item.getDescription());
        args.putString(ARG_TIME, item.getTime()); // Formatted time from list
        args.putInt(ARG_TYPE, item.getType());

        // Pass new data
        args.putString(ARG_SENDER, item.getSenderName());
        args.putString(ARG_RAW_DATE, item.getCreatedAt()); // Raw timestamp for date formatting

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        TextView tvSubject = view.findViewById(R.id.tv_subject);
        TextView tvBody = view.findViewById(R.id.tv_body);
        TextView tvTime = view.findViewById(R.id.tv_time);
        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvSenderName = view.findViewById(R.id.tv_sender_name); // ✅ Bind Sender Name
        ImageView imgAttachment = view.findViewById(R.id.img_attachment);

        // Load data from arguments
        if (getArguments() != null) {
            tvSubject.setText(getArguments().getString(ARG_TITLE));
            tvBody.setText(getArguments().getString(ARG_BODY));
            tvTime.setText(getArguments().getString(ARG_TIME));

            // ✅ 1. Set Sender Name (Default to "Barangay Staff" if missing)
            String sender = getArguments().getString(ARG_SENDER);
            if (sender != null && !sender.isEmpty()) {
                tvSenderName.setText(sender);
            } else {
                tvSenderName.setText("Barangay Staff");
            }

            // ✅ 2. Format and Set Date (e.g., "August 23, 2025")
            String rawDate = getArguments().getString(ARG_RAW_DATE);
            if (rawDate != null) {
                tvDate.setText(formatDate(rawDate));
            }

            // Handle Attachment Visibility
            int type = getArguments().getInt(ARG_TYPE);
            if (type == 1) {
                imgAttachment.setVisibility(View.VISIBLE);
            } else {
                imgAttachment.setVisibility(View.GONE);
            }
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    // Helper to format raw DB timestamp into readable Date
    private String formatDate(String rawDate) {
        try {
            // Parse UTC string from Supabase
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(rawDate);

            // Format to "MMMM dd, yyyy" (e.g., August 23, 2025)
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty if error
        }
    }
}