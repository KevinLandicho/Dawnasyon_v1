package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Notification_fragment extends BaseFragment {

    private RecyclerView rvNew;
    private RecyclerView rvOld;
    private Button btnSeePrevious;
    private TextView tvHeaderNew, tvHeaderOld;

    public Notification_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        rvNew = view.findViewById(R.id.rv_new);
        rvOld = view.findViewById(R.id.rv_old);
        btnSeePrevious = view.findViewById(R.id.btn_see_previous);
        tvHeaderNew = view.findViewById(R.id.tv_header_new);
        tvHeaderOld = view.findViewById(R.id.tv_header_old);

        rvNew.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOld.setLayoutManager(new LinearLayoutManager(getContext()));

        loadNotifications();
    }

    private void loadNotifications() {
        // Fetch raw data using Helper
        SupabaseJavaHelper.fetchNotifications(new NotificationCallback() {
            @Override
            public void onSuccess(List<? extends NotificationItem> data) {
                if (isAdded()) {
                    processAndDisplay((List<NotificationItem>) data);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Log.e("NotifFrag", "Error: " + message);
                    Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processAndDisplay(List<NotificationItem> rawList) {
        List<NotificationItem> newList = new ArrayList<>();
        List<NotificationItem> oldList = new ArrayList<>();

        // Date Formatters
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        parser.setTimeZone(TimeZone.getTimeZone("UTC")); // Parse Supabase UTC time

        SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String todayStr = dateFormatter.format(new Date());

        for (NotificationItem item : rawList) {
            try {
                if (item.getCreatedAt() != null) {
                    Date date = parser.parse(item.getCreatedAt());

                    // Set formatted time (e.g., "1:28 PM")
                    item.setTime(timeFormatter.format(date));

                    // Check Date Logic
                    String itemDateStr = dateFormatter.format(date);

                    if (itemDateStr.equals(todayStr)) {
                        // If date is TODAY -> New List
                        item.setDateCategory("New");
                        newList.add(item);
                    } else {
                        // If date is Yesterday or older -> Old List
                        item.setDateCategory("Old");
                        oldList.add(item);
                    }
                }

                // Icon Logic (0 = Default, 1 = Alert/Decline)
                if (item.getDbType() != null && item.getDbType().equalsIgnoreCase("Decline")) {
                    item.setType(1);
                } else {
                    item.setType(0);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set Adapters
        rvNew.setAdapter(new NotificationAdapter(newList));
        rvOld.setAdapter(new NotificationAdapter(oldList));

        // Logic to Hide/Show headers if empty
        if (newList.isEmpty()) {
            tvHeaderNew.setVisibility(View.GONE);
        } else {
            tvHeaderNew.setVisibility(View.VISIBLE);
        }

        if (oldList.isEmpty()) {
            tvHeaderOld.setVisibility(View.GONE);
            btnSeePrevious.setVisibility(View.GONE); // Hide button if no old notifications
        } else {
            tvHeaderOld.setVisibility(View.VISIBLE);

            // Logic: Show button only if there are many old notifications (e.g., > 5)
            // Or simply show it if the Old list exists (based on your request)
            if (oldList.size() > 5) {
                btnSeePrevious.setVisibility(View.VISIBLE);
            } else {
                btnSeePrevious.setVisibility(View.GONE);
            }
        }
    }
}