package com.example.dawnasyon_v1;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Summary_fragment extends BaseFragment {

    private static final String ARG_DONATION_ITEMS = "donation_items";
    private static final String TAG = "SummaryFragment";

    public static class ItemForSummary implements Serializable {
        public String name;
        public String quantityUnit; // e.g., "5 kg"

        public ItemForSummary(String name, String quantityUnit) {
            this.name = name;
            this.quantityUnit = quantityUnit;
        }
    }

    private ArrayList<ItemForSummary> itemsToDonate;

    public static Summary_fragment newInstance(ArrayList<ItemForSummary> items) {
        Summary_fragment fragment = new Summary_fragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DONATION_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout summaryContainer = view.findViewById(R.id.summaryItemsContainer);
        Button btnApplyToDonate = view.findViewById(R.id.btnApplyToDonate);
        CheckBox cbAnonymous = view.findViewById(R.id.checkAnonymous);

        // Load Items into UI
        if (getArguments() != null) {
            itemsToDonate = (ArrayList<ItemForSummary>) getArguments().getSerializable(ARG_DONATION_ITEMS);
            if (itemsToDonate != null) {
                for (ItemForSummary item : itemsToDonate) {
                    addItemRowToSummary(summaryContainer, item.name, item.quantityUnit);
                }
            }
        }

        // ⭐ FIXED: Fetch variables OUTSIDE the lambda (click listener) so they are "effectively final"
        final String rawDonationType = Donation_details_fragment.currentDonationType;
        final String finalDonationType = (rawDonationType == null || rawDonationType.isEmpty()) ? "In-Kind" : rawDonationType;

        final String finalItemDesc = Donation_details_fragment.currentItemDescription;

        if (finalDonationType.equalsIgnoreCase("Relief Pack") && finalItemDesc != null && !finalItemDesc.isEmpty()) {
            addItemRowToSummary(summaryContainer, "Contents", finalItemDesc);
        }

        // Donate Button Click
        btnApplyToDonate.setOnClickListener(v -> {
            if (itemsToDonate == null || itemsToDonate.isEmpty()) {
                Toast.makeText(getContext(), "No items to donate.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnApplyToDonate.setEnabled(false);

            String processingText = "Processing...";
            btnApplyToDonate.setText(processingText);
            TranslationHelper.autoTranslate(getContext(), btnApplyToDonate, processingText);

            boolean isAnon = cbAnonymous != null && cbAnonymous.isChecked();
            String refNumber = generateReferenceNumber();

            // ⭐ Call the Helper using the final variables
            DonationHelper.submitDonation(
                    refNumber,
                    itemsToDonate,
                    finalDonationType,
                    finalItemDesc,
                    0.0,
                    isAnon,
                    new DonationHelper.DonationCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() == null) return;
                            btnApplyToDonate.setEnabled(true);

                            String confirmText = "Confirm Donation";
                            btnApplyToDonate.setText(confirmText);
                            TranslationHelper.autoTranslate(getContext(), btnApplyToDonate, confirmText);

                            Toast.makeText(getContext(), "Donation Submitted!", Toast.LENGTH_SHORT).show();

                            launchReferenceFragment(refNumber);
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            if (getActivity() == null) return;
                            btnApplyToDonate.setEnabled(true);

                            String confirmText = "Confirm Donation";
                            btnApplyToDonate.setText(confirmText);
                            TranslationHelper.autoTranslate(getContext(), btnApplyToDonate, confirmText);

                            Toast.makeText(getContext(), "Failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        applyTagalogTranslation(view);
    }

    private void addItemRowToSummary(LinearLayout container, String name, String quantityUnit) {
        TextView textView = new TextView(getContext());
        String fullText = name + ": " + quantityUnit; // Formatted nicely
        textView.setText(fullText);

        TranslationHelper.autoTranslate(getContext(), textView, fullText);

        textView.setTextSize(16);
        try {
            Typeface dongleTypeface = ResourcesCompat.getFont(getContext(), R.font.dongle);
            textView.setTypeface(dongleTypeface);
            textView.setTextColor(getResources().getColor(android.R.color.black));
        } catch (Exception e) {
            textView.setTypeface(Typeface.DEFAULT);
        }
        textView.setPadding(0, 10, 0, 10);
        container.addView(textView);
    }

    private String generateReferenceNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String datePart = sdf.format(new Date());
        Random random = new Random();
        int randomId = random.nextInt(90000) + 10000;
        return "D" + datePart + "-" + randomId;
    }

    private void launchReferenceFragment(String referenceNumber) {
        if (getActivity() != null) {
            try {
                Fragment referenceFragment = Reference_fragment.newInstance(referenceNumber);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, referenceFragment)
                        .addToBackStack(null)
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Navigation Error", e);
            }
        }
    }
}