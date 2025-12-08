package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class Notification_fragment extends BaseFragment {

    public Notification_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup "Earlier" List
        RecyclerView rvEarlier = view.findViewById(R.id.rv_earlier);
        rvEarlier.setLayoutManager(new LinearLayoutManager(getContext()));

        List<NotificationItem> earlierList = new ArrayList<>();
        earlierList.add(new NotificationItem("Relief Operation at Pili Street", "Food packs distribution starts at 3 PM.", "1:28 PM", "Earlier", 0));
        earlierList.add(new NotificationItem("Application Decline", "Invalid ID Submitted. Please reapply with a clear photo.", "1:28 PM", "Earlier", 1));
        earlierList.add(new NotificationItem("Account Verified", "Your account has been successfully verified.", "1:28 PM", "Earlier", 0));

        rvEarlier.setAdapter(new NotificationAdapter(earlierList));


        // Setup "Yesterday" List
        RecyclerView rvYesterday = view.findViewById(R.id.rv_yesterday);
        rvYesterday.setLayoutManager(new LinearLayoutManager(getContext()));

        List<NotificationItem> yesterdayList = new ArrayList<>();
        yesterdayList.add(new NotificationItem("Relief Goods Status", "Low stock alert for Hygiene Kits.", "1:28 PM", "Yesterday", 0));
        yesterdayList.add(new NotificationItem("New Donation", "Received 50 packs of rice.", "1:28 PM", "Yesterday", 0));

        rvYesterday.setAdapter(new NotificationAdapter(yesterdayList));
    }
}