package com.example.dawnasyon_v1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DonationReceipt_fragment extends BaseFragment {

    private static final String ARG_ITEM = "donation_item";

    public DonationReceipt_fragment() {}

    public static DonationReceipt_fragment newInstance(DonationHistoryItem item) {
        DonationReceipt_fragment fragment = new DonationReceipt_fragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation_receipt, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        TextView tvReceiptNo = view.findViewById(R.id.tv_receipt_no);
        TextView tvDate = view.findViewById(R.id.tv_receipt_date);
        TextView tvDonorName = view.findViewById(R.id.tv_donor_name);

        LinearLayout itemsContainer = view.findViewById(R.id.ll_items_container);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                tvDonorName.setText(profile.getFull_name());
            } else {
                tvDonorName.setText("Valued Donor");
            }
            return null;
        });

        if (getArguments() != null) {
            DonationHistoryItem item = (DonationHistoryItem) getArguments().getSerializable(ARG_ITEM);

            if (item != null) {
                if (item.getReferenceNumber() != null && !item.getReferenceNumber().isEmpty()) {
                    tvReceiptNo.setText(item.getReferenceNumber());
                } else {
                    tvReceiptNo.setText(String.format("ID-%d", item.getDonationId()));
                }

                tvDate.setText(item.getFormattedDate());

                if (itemsContainer != null) {
                    String type = item.getType();

                    if (type != null && type.equalsIgnoreCase("Cash")) {
                        addReceiptRow(itemsContainer, "Cash Donation", "₱" + item.getAmount());

                    } else if (type != null && type.equalsIgnoreCase("Relief Pack")) {
                        // ⭐ NEW: Specific logic for Relief Packs to show contents
                        List<DonationItem> subItems = item.getDonationItems();
                        if (subItems != null && !subItems.isEmpty()) {
                            for (DonationItem sub : subItems) {
                                addReceiptRow(itemsContainer, sub.getItemName(), sub.getQtyString());
                            }
                        } else {
                            addReceiptRow(itemsContainer, "Relief Pack", "Details pending");
                        }

                        // Add the Specific Contents Row
                        String details = (item.getItemDescription() != null && !item.getItemDescription().isEmpty())
                                ? item.getItemDescription()
                                : "Standard Contents";
                        addReceiptRow(itemsContainer, "Contents", details);

                    } else {
                        // Normal In-Kind logic
                        List<DonationItem> subItems = item.getDonationItems();
                        if (subItems != null && !subItems.isEmpty()) {
                            for (DonationItem sub : subItems) {
                                addReceiptRow(itemsContainer, sub.getItemName(), sub.getQtyString());
                            }
                        } else {
                            addReceiptRow(itemsContainer, "In-Kind Donation", "See details");
                        }
                    }

                    loadTrackingTimeline(itemsContainer, item.getDonationId());
                }
            }
        }

        // ⭐ ENABLE AUTO-TRANSLATION FOR THIS SCREEN
        applyTagalogTranslation(view);
    }

    private void loadTrackingTimeline(LinearLayout container, long donationId) {
        View separator = new View(getContext());
        separator.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        separator.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams sepParams = (LinearLayout.LayoutParams) separator.getLayoutParams();
        sepParams.setMargins(0, 30, 0, 30);
        separator.setLayoutParams(sepParams);
        container.addView(separator);

        TextView header = new TextView(getContext());
        header.setText("Donation Journey");
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(Color.parseColor("#E65100"));
        container.addView(header);

        // Translate Header
        TranslationHelper.autoTranslate(getContext(), header, "Donation Journey");

        TextView tvLoading = new TextView(getContext());
        tvLoading.setText("Tracking status...");
        tvLoading.setTextSize(12);
        tvLoading.setTextColor(Color.GRAY);
        container.addView(tvLoading);

        // Translate Loading Text
        TranslationHelper.autoTranslate(getContext(), tvLoading, "Tracking status...");

        SupabaseJavaHelper.fetchDonationTracking(donationId, trackingDTO -> {
            if (!isAdded()) return;
            container.removeView(tvLoading);

            if (trackingDTO != null) {
                addTimelineRow(container, "✅ Received", "Verified by Admin on " + formatDate(trackingDTO.getDonation_date()), true);

                if (trackingDTO.getInventory_status() != null) {
                    String desc = "Status: " + trackingDTO.getInventory_status();
                    if (trackingDTO.getQuantity_on_hand() != null) {
                        desc += " (" + trackingDTO.getQuantity_on_hand() + " items in stock)";
                    }
                    addTimelineRow(container, "📦 Inventory", desc, true);
                }

                if (trackingDTO.getDate_claimed() != null) {
                    String distDesc = "Given to " + (trackingDTO.getBatch_name() != null ? trackingDTO.getBatch_name() : "recipient");
                    distDesc += " on " + formatDate(trackingDTO.getDate_claimed());
                    addTimelineRow(container, "🤝 Distributed", distDesc, true);
                } else {
                    addTimelineRow(container, "⏳ Distribution", "Waiting for distribution...", false);
                }

            } else {
                addTimelineRow(container, "Processing", "Tracking info pending.", false);
            }
        });
    }

    private void addTimelineRow(LinearLayout container, String title, String description, boolean isActive) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 16, 0, 16);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(isActive ? Color.parseColor("#2E7D32") : Color.GRAY);

        TextView tvDesc = new TextView(getContext());
        tvDesc.setText(description);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(Color.DKGRAY);

        // ⭐ TRANSLATE DYNAMIC ROWS
        TranslationHelper.autoTranslate(getContext(), tvTitle, title);
        TranslationHelper.autoTranslate(getContext(), tvDesc, description);

        row.addView(tvTitle);
        row.addView(tvDesc);
        container.addView(row);
    }

    private String formatDate(String rawDate) {
        if (rawDate == null) return "";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (rawDate.contains("T")) input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = input.parse(rawDate);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) { return rawDate; }
    }

    private void addReceiptRow(LinearLayout container, String name, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 12);

        TextView tvName = new TextView(getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#444444"));
        tvName.setTextSize(14);

        TextView tvValue = new TextView(getContext());
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvValue.setText(value);
        tvValue.setTextColor(Color.BLACK);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextSize(14);
        tvValue.setGravity(Gravity.END);

        // ⭐ TRANSLATE RECEIPT ITEMS
        TranslationHelper.autoTranslate(getContext(), tvName, name);
        // Note: tvValue is usually numbers/prices, so we skip translating it to keep format correct

        row.addView(tvName);
        row.addView(tvValue);
        container.addView(row);

        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) line.getLayoutParams();
        params.setMargins(0, 0, 0, 24);
        line.setLayoutParams(params);
        container.addView(line);
    }
}