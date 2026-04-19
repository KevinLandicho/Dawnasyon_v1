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

                    // Added extra checks here in case donors use GCash/PayMongo
                    boolean isMonetary = false;
                    if (type != null) {
                        String t = type.toLowerCase();
                        if (t.contains("cash") || t.contains("paymongo") || t.contains("gcash") || t.contains("bank")) {
                            isMonetary = true;
                        }
                    }

                    if (isMonetary) {
                        addReceiptRow(itemsContainer, type + " Donation", "₱" + item.getAmount());

                    } else if (type != null && type.equalsIgnoreCase("Relief Pack")) {
                        List<DonationItem> subItems = item.getDonationItems();
                        if (subItems != null && !subItems.isEmpty()) {
                            for (DonationItem sub : subItems) {
                                addReceiptRow(itemsContainer, sub.getItemName(), sub.getQtyString());
                            }
                        } else {
                            addReceiptRow(itemsContainer, "Relief Pack", "Details pending");
                        }

                        String details = (item.getItemDescription() != null && !item.getItemDescription().isEmpty())
                                ? item.getItemDescription()
                                : "Standard Contents";
                        addReceiptRow(itemsContainer, "Contents", details);

                    } else {
                        List<DonationItem> subItems = item.getDonationItems();
                        if (subItems != null && !subItems.isEmpty()) {
                            for (DonationItem sub : subItems) {
                                addReceiptRow(itemsContainer, sub.getItemName(), sub.getQtyString());
                            }
                        } else {
                            addReceiptRow(itemsContainer, "In-Kind Donation", "See details");
                        }
                    }

                    loadTrackingTimeline(itemsContainer, item);
                }
            }
        }

        applyTagalogTranslation(view);
    }

    private void loadTrackingTimeline(LinearLayout container, DonationHistoryItem item) {
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
        TranslationHelper.autoTranslate(getContext(), header, "Donation Journey");

        String currentStatus = item.getStatus() != null ? item.getStatus() : "";
        if (currentStatus.equalsIgnoreCase("Declined") || currentStatus.equalsIgnoreCase("Rejected")) {
            addTimelineRow(container, "❌ Declined", "Sorry, this donation was not accepted by the admin at this time.", true);
            return;
        }

        TextView tvLoading = new TextView(getContext());
        tvLoading.setText("Tracking status...");
        tvLoading.setTextSize(12);
        tvLoading.setTextColor(Color.GRAY);
        container.addView(tvLoading);
        TranslationHelper.autoTranslate(getContext(), tvLoading, "Tracking status...");

        SupabaseJavaHelper.fetchDonationTracking(item.getDonationId(), trackingDTO -> {
            if (!isAdded()) return;
            container.removeView(tvLoading);

            if (trackingDTO != null) {
                addTimelineRow(container, "✅ Received", "Verified by Admin on " + formatDate(trackingDTO.getDonation_date()), true);

                // THE BULLETPROOF FIX: Safely detect Cash even if it's GCash, PayMongo, etc.
                boolean isCash = false;
                String type = item.getType() != null ? item.getType().toLowerCase().trim() : "";
                if (type.contains("cash") || type.contains("paymongo") || type.contains("gcash") || type.contains("bank") || type.contains("monetary")) {
                    isCash = true;
                } else if (item.getAmount() > 0 && (item.getDonationItems() == null || item.getDonationItems().isEmpty())) {
                    isCash = true; // Fallback: If it has money but no physical items, it's treasury.
                }

                // Safely extract integers and strings to prevent NullPointerExceptions
                Integer qtyObj = trackingDTO.getQuantity_on_hand();
                int qty = (qtyObj != null) ? qtyObj : 0;
                String invStatus = trackingDTO.getInventory_status();

                // We no longer skip if invStatus is null. If it's cash and qty is 0, we force it to show!
                if (invStatus != null || qtyObj != null || isCash) {
                    String rowTitle = isCash ? "💰 Treasury" : "📦 Inventory";
                    String desc = "";

                    if (isCash) {
                        if (qty <= 0) {
                            desc = "✅ Funds Utilized (Converted to Relief Goods)";
                        } else {
                            desc = "Status: " + (invStatus != null ? invStatus : "Funds Available in Treasury");
                        }
                    } else {
                        desc = "Status: " + (invStatus != null ? invStatus : "Updated");
                        if (qty <= 0) {
                            desc += " (Fully Distributed / Converted)";
                        } else {
                            desc += " (" + qty + " items in stock)";
                        }
                    }
                    addTimelineRow(container, rowTitle, desc, true);
                }

                // ⭐ THE NEW UPDATED WOW FACTOR CODE IS HERE
                if (trackingDTO.getDate_claimed() != null) {
                    String batch = trackingDTO.getBatch_name() != null ? trackingDTO.getBatch_name() : "a Relief Bag";
                    String area = trackingDTO.getDestination_area() != null ? trackingDTO.getDestination_area() : "the community";

                    String distDesc = "Repacked into " + batch + " → Distributed to a family in " + area;
                    distDesc += "\nDate: " + formatDate(trackingDTO.getDate_claimed());

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

        if (title.contains("Declined")) {
            tvTitle.setTextColor(Color.parseColor("#C62828"));
        } else {
            tvTitle.setTextColor(isActive ? Color.parseColor("#2E7D32") : Color.GRAY);
        }

        TextView tvDesc = new TextView(getContext());
        tvDesc.setText(description);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(Color.DKGRAY);

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

        TranslationHelper.autoTranslate(getContext(), tvName, name);

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