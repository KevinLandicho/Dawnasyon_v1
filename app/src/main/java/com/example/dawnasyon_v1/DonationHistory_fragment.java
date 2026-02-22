package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private List<DonationHistoryItem> historyList = new ArrayList<>();

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

        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

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
        historyList.clear();
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

                // ⭐ PERFECT FIX: Safely gets the description using your newly added getter!
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

            item.setDisplayDescription(item.getDisplayDescription() + "\nStatus: " + (item.getStatus() != null ? item.getStatus() : "Pending"));
            item.setImageResId(R.drawable.ic_profile_avatar);
            historyList.add(item);
        }
        adapter.notifyDataSetChanged();
    }
}