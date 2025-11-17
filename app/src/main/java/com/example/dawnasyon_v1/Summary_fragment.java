package com.example.dawnasyon_v1;

import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

public class Summary_fragment extends Fragment {

    private static final String ARG_DONATION_ITEMS = "donation_items";
    // NOTE: This class is defined here to be the canonical source of the data structure.

    // --- Data Class for Summary Fragment ---
    public static class ItemForSummary implements Serializable {
        public String name;
        public String quantityUnit; // e.g., "10KGS", "30PCS"

        public ItemForSummary(String name, String quantityUnit) {
            this.name = name;
            this.quantityUnit = quantityUnit;
        }
    }

    // This is the newInstance method that Donation_details_fragment.java is trying to call.
    public static Summary_fragment newInstance(ArrayList<ItemForSummary> items) {
        Summary_fragment fragment = new Summary_fragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DONATION_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // You MUST create R.layout.summary_details (as discussed previously)
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout summaryContainer = view.findViewById(R.id.summaryItemsContainer);
        Button btnApplyToDonate = view.findViewById(R.id.btnApplyToDonate);

        // 1. Get the collected data from the arguments
        if (getArguments() != null) {
            ArrayList<ItemForSummary> items = (ArrayList<ItemForSummary>) getArguments().getSerializable(ARG_DONATION_ITEMS);

            if (items != null) {
                for (ItemForSummary item : items) {
                    // 2. Display the items
                    addItemRowToSummary(summaryContainer, item.name, item.quantityUnit);
                }
            }
        }

        // Add dummy logic for the button
        btnApplyToDonate.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Donation Application Submitted!", Toast.LENGTH_SHORT).show();
            // TODO: Add actual navigation/submission logic here
        });
    }

    /**
     * Dynamically adds a row to display one donation item.
     */
    private void addItemRowToSummary(LinearLayout container, String name, String quantityUnit) {
        // You would typically inflate a small layout for the row here, but for simplicity,
        // we'll just add TextViews to show the functionality is working.
        TextView textView = new TextView(getContext());
        textView.setText(name + " (" + quantityUnit + ")");
        textView.setTextSize(16);

        textView.setPadding(0, 10, 0, 10);
        container.addView(textView);
    }
}