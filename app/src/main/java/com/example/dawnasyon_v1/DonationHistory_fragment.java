package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

public class DonationHistory_fragment extends BaseFragment {

    private RecyclerView rvHistory;
    private DonationHistoryAdapter adapter;

    // ⭐ Master list to hold all data, and historyList to show filtered data
    private List<DonationHistoryItem> fullHistoryList = new ArrayList<>();
    private List<DonationHistoryItem> historyList = new ArrayList<>();

    // Filter Buttons
    private Button btnAll, btnPending, btnApproved, btnDeclined;

    public DonationHistory_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        rvHistory = view.findViewById(R.id.rv_donation_history);

        // Initialize Buttons
        btnAll = view.findViewById(R.id.btn_filter_all);
        btnPending = view.findViewById(R.id.btn_filter_pending);
        btnApproved = view.findViewById(R.id.btn_filter_approved);
        btnDeclined = view.findViewById(R.id.btn_filter_declined);

        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Setup Filter Click Listeners
        btnAll.setOnClickListener(v -> applyFilter("All", btnAll));
        btnPending.setOnClickListener(v -> applyFilter("Pending", btnPending));
        btnApproved.setOnClickListener(v -> applyFilter("Approved", btnApproved));
        btnDeclined.setOnClickListener(v -> applyFilter("Declined", btnDeclined));

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

            adapter = new DonationHistoryAdapter(historyList, item -> {
                DonationReceipt_fragment receiptFragment = DonationReceipt_fragment.newInstance(item);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, receiptFragment)
                        .addToBackStack(null)
                        .commit();
            });

            rvHistory.setAdapter(adapter);
            loadDonationHistory();
        }

        applyTagalogTranslation(view);
    }

    private void loadDonationHistory() {
        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        SupabaseJavaHelper.fetchDonationHistory(new SupabaseJavaHelper.DonationHistoryCallback() {
            @Override
            public void onSuccess(List<DonationHistoryItem> data) {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    processAndDisplay(data);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    Log.e("HistoryFrag", "Error: " + message);
                    Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processAndDisplay(List<DonationHistoryItem> rawList) {
        fullHistoryList.clear();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        for (DonationHistoryItem item : rawList) {
            try {
                if (item.getCreatedAt() != null) {
                    Date date = inputFormat.parse(item.getCreatedAt());
                    item.setFormattedDate(outputFormat.format(date));
                }
            } catch (Exception e) {
                item.setFormattedDate("Unknown Date");
            }

            String type = item.getType();

            if (type != null && type.equalsIgnoreCase("Cash")) {
                item.setDisplayDescription("Cash Donation" + (item.getAmount() != null ? ": ₱" + item.getAmount() : ""));

            } else if (type != null && type.equalsIgnoreCase("Relief Pack")) {
                List<DonationItem> items = item.getDonationItems();
                String qty = "";
                if (items != null && !items.isEmpty()) {
                    qty = items.get(0).getQtyString() + " ";
                }

                String details = (item.getItemDescription() != null && !item.getItemDescription().isEmpty())
                        ? item.getItemDescription()
                        : "Standard Contents";

                item.setDisplayDescription(qty + "Relief Pack(s)\nContents: " + details);

            } else {
                List<DonationItem> items = item.getDonationItems();
                if (items != null && !items.isEmpty()) {
                    DonationItem firstItem = items.get(0);
                    String summary = firstItem.getQtyString() + " " + firstItem.getItemName();
                    if (items.size() > 1) summary += " + " + (items.size() - 1) + " others";
                    item.setDisplayDescription(summary);
                } else {
                    item.setDisplayDescription("In-Kind Donation");
                }
            }

            // Ensure we grab the actual status from Supabase
            String currentStatus = (item.getStatus() != null && !item.getStatus().isEmpty()) ? item.getStatus() : "Pending";

            item.setDisplayDescription(item.getDisplayDescription() + "\nStatus: " + currentStatus);
            item.setImageResId(R.drawable.ic_profile_avatar);

            // Add to master list
            fullHistoryList.add(item);
        }

        // ⭐ Default to showing "All" when data finishes loading
        applyFilter("All", btnAll);
    }

    // ========================================================================
    // ⭐ BULLETPROOF FILTER LOGIC
    // ========================================================================
    private void applyFilter(String targetStatus, Button activeBtn) {
        // 1. Reset all button colors to gray
        resetButtonStyles();

        // 2. Highlight the clicked button to Teal
        activeBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#27869B")));
        activeBtn.setTextColor(Color.WHITE);

        // 3. Filter the actual list with forgiving string matching
        historyList.clear();
        for (DonationHistoryItem item : fullHistoryList) {

            if (targetStatus.equals("All")) {
                historyList.add(item);
            } else {
                // Grab the status and force it to lowercase to avoid case-sensitivity bugs
                String itemStatus = item.getStatus() != null ? item.getStatus().trim().toLowerCase() : "pending";

                // Route to the correct button based on broad keywords
                if (targetStatus.equals("Approved")) {
                    if (itemStatus.contains("approve") || itemStatus.contains("accept") || itemStatus.contains("inventory")) {
                        historyList.add(item);
                    }
                }
                else if (targetStatus.equals("Declined")) {
                    if (itemStatus.contains("decline") || itemStatus.contains("reject") || itemStatus.contains("cancel")) {
                        historyList.add(item);
                    }
                }
                else if (targetStatus.equals("Pending")) {
                    if (itemStatus.contains("pending")) {
                        historyList.add(item);
                    }
                }
            }
        }

        // 4. Tell the RecyclerView to refresh
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void resetButtonStyles() {
        Button[] buttons = {btnAll, btnPending, btnApproved, btnDeclined};
        int inactiveColor = Color.parseColor("#E0E0E0"); // Light Gray
        int inactiveTextColor = Color.BLACK;

        for (Button b : buttons) {
            if (b != null) {
                b.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
                b.setTextColor(inactiveTextColor);
            }
        }
    }
}