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
import java.util.List;

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

        // Fetch User Name
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
                // --- A. Set Reference Number ---
                if (item.getReferenceNumber() != null && !item.getReferenceNumber().isEmpty()) {
                    tvReceiptNo.setText(item.getReferenceNumber());
                } else {
                    tvReceiptNo.setText(String.format("ID-%d", item.getDonationId()));
                }

                // --- B. Set Date ---
                tvDate.setText(item.getFormattedDate());

                // --- C. Populate Items ---
                if (itemsContainer != null) {
                    // Clear dummy views
                    int childCount = itemsContainer.getChildCount();
                    if (childCount > 2) {
                        itemsContainer.removeViews(2, childCount - 2);
                    }

                    if (item.getType() != null && item.getType().equalsIgnoreCase("Cash")) {
                        // Cash
                        addReceiptRow(itemsContainer, "Cash Donation", "₱" + item.getAmount());
                    } else {
                        // In-Kind: Loop through items
                        List<DonationItem> subItems = item.getDonationItems();

                        if (subItems != null && !subItems.isEmpty()) {
                            for (DonationItem sub : subItems) {
                                addReceiptRow(itemsContainer, sub.getItemName(), sub.getQtyString());
                            }
                        } else {
                            addReceiptRow(itemsContainer, "In-Kind Donation", "See details");
                        }
                    }
                }
            }
        }
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