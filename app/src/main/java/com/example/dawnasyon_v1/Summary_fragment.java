package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import android.graphics.Typeface;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Summary_fragment extends Fragment {

    private static final String ARG_DONATION_ITEMS = "donation_items";
    private static final String TAG = "SummaryFragment";
    // --- ItemForSummary class (Define this class here) ---
    public static class ItemForSummary implements Serializable {
        public String name;
        public String quantityUnit;

        public ItemForSummary(String name, String quantityUnit) {
            this.name = name;
            this.quantityUnit = quantityUnit;
        }
    }
    // ---------------------------------------------------

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
        // Assume R.layout.summary_details is your layout file for this fragment
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

        // 🟢 FIX: Implement click listener to generate reference and navigate
        btnApplyToDonate.setOnClickListener(v -> {
            String refNumber = generateReferenceNumber();
            launchReferenceFragment(refNumber);
        });
    }

    // --- HELPER METHOD TO SET FONT (Using Dongle as requested) ---

    private void addItemRowToSummary(LinearLayout container, String name, String quantityUnit) {
        TextView textView = new TextView(getContext());
        textView.setText(name + " (" + quantityUnit + ")");
        textView.setTextSize(16);

        try {
            // Load the custom Dongle font
            Typeface dongleTypeface = ResourcesCompat.getFont(getContext(), R.font.dongle); // Assuming you used this name
            textView.setTypeface(dongleTypeface);

            // Optionally set text color
            textView.setTextColor(container.getContext().getResources().getColor(android.R.color.black));

        } catch (Exception e) {
            Log.e("SummaryFragment", "Dongle font not found, using default.", e);
            textView.setTypeface(Typeface.MONOSPACE);
        }

        textView.setPadding(0, 10, 0, 10);
        container.addView(textView);
    }


    // --- NEW METHOD 1: REFERENCE NUMBER GENERATION ---

    /**
     * Generates a unique reference number based on the current date and a random 5-digit ID.
     * Format: DYYYYMMDD-XXXXX (e.g., D20251117-09876)
     * @return The generated reference number string.
     */
    private String generateReferenceNumber() {
        // Get current date in YYYYMMDD format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String datePart = sdf.format(new Date());

        // Generate a random 5-digit number
        Random random = new Random();
        // Generates a number between 10000 and 99999 (inclusive)
        int randomId = random.nextInt(90000) + 10000;

        // Combine parts
        return "D" + datePart + "-" + randomId;
    }

    // --- NEW METHOD 2: NAVIGATION TO REFERENCE FRAGMENT ---

    /**
     * Navigates to the Reference_fragment, passing the generated reference number.
     * @param referenceNumber The transaction ID to display.
     */
    private void launchReferenceFragment(String referenceNumber) {
        if (getActivity() != null) {
            // NOTE: You need to create Reference_fragment.java and its newInstance method!
            try {
                Fragment referenceFragment = Reference_fragment.newInstance(referenceNumber);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, referenceFragment) // Replace R.id.fragment_container with your actual host ID
                        .addToBackStack(null) // Prevent going back to Summary on pressing Back
                        .commit();
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                Log.e(TAG, "Reference_fragment is missing or its newInstance method is incorrect.", e);
                Toast.makeText(getContext(), "Error: Reference screen missing.", Toast.LENGTH_LONG).show();
            }
        }
    }
}