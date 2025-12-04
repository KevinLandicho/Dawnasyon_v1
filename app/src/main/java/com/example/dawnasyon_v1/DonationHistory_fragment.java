package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast; // Added for debug
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DonationHistory_fragment extends Fragment {

    public DonationHistory_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        RecyclerView rvHistory = view.findViewById(R.id.rv_donation_history);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

            // Mock Data
            List<DonationHistoryItem> historyList = new ArrayList<>();
            historyList.add(new DonationHistoryItem("Juan M. Dela Cruz", "09/1/2025", "Assorted Goods", "Completed", R.drawable.ic_profile_avatar));
            historyList.add(new DonationHistoryItem("Juan M. Dela Cruz", "08/15/2025", "Cash Donation", "Verified", R.drawable.ic_profile_avatar));
            historyList.add(new DonationHistoryItem("Juan M. Dela Cruz", "07/20/2025", "Hygiene Kits", "Completed", R.drawable.ic_profile_avatar));

            // ⭐ UPDATED: Pass the click logic here using Lambda ⭐
            DonationHistoryAdapter adapter = new DonationHistoryAdapter(historyList, item -> {

                // DEBUG: Confirm click works
                // Toast.makeText(getContext(), "Opening Receipt...", Toast.LENGTH_SHORT).show();

                // NAVIGATION LOGIC
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DonationReceipt_fragment()) // Ensure ID matches your MainActivity layout
                        .addToBackStack(null)
                        .commit();
            });

            rvHistory.setAdapter(adapter);
        }
    }
}