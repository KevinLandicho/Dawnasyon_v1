package com.example.dawnasyon_v1;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DonationOptions_fragments extends Fragment {

    private LinearLayout categoryContainer;

    public DonationOptions_fragments() {
        // Required empty public constructor
    }

    public static DonationOptions_fragments newInstance(String p1, String p2) {
        DonationOptions_fragments fragment = new DonationOptions_fragments();
        Bundle args = new Bundle();
        args.putString("param1", p1);
        args.putString("param2", p2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_donation_options, container, false);

        categoryContainer = view.findViewById(R.id.categoryContainer);

        loadCategories(inflater);

        return view;
    }

    // -----------------------------------------
    // LOAD CATEGORIES
    // -----------------------------------------
    private void loadCategories(LayoutInflater inflater) {

        // NOTE: The title strings here MUST EXACTLY MATCH the keys in
        // PRESET_ITEMS map in Donation_details_fragment (e.g., "FOOD" not "Food").

        addCategory(
                inflater,
                "CASH",
                "Funds for supplies and relief support.",
                "Critical",
                R.drawable.ic_money
        );

        addCategory(
                inflater,
                "FOOD", // Use uppercase "FOOD" to match the map key
                "Rice, noodles, and canned goods for nourishment.",
                "Critical",
                R.drawable.ic_food
        );

        addCategory(
                inflater,
                "HYGIENE KITS", // Use uppercase "HYGIENE KITS" to match the map key
                "Soap, toothpaste, and essentials for cleanliness.",
                "High",
                R.drawable.ic_hygiene
        );

        addCategory(
                inflater,
                "MEDICINE", // Use uppercase "MEDICINE" to match the map key
                "Vitamins and first aid for basic health care.",
                "Critical",
                R.drawable.ic_health
        );

        addCategory(
                inflater,
                "RELIEF PACKS",
                "Packed goods for nourishment and survival.",
                "High",
                R.drawable.ic_packs

        );
    }

    // ------------------------------------------------------
    // INFLATE CARD VIEW AND SET CLICK LISTENER
    // ------------------------------------------------------
    private void addCategory(LayoutInflater inflater, String title, String description,
                             String status, int imageRes) {

        View item = inflater.inflate(R.layout.category_item, categoryContainer, false);

        ImageView imgIcon = item.findViewById(R.id.imgIcon);
        TextView txtTitle = item.findViewById(R.id.txtTitle);
        TextView txtStatus = item.findViewById(R.id.txtStatus);
        TextView txtDescription = item.findViewById(R.id.txtDescription);

        imgIcon.setImageResource(imageRes);
        txtTitle.setText(title);
        txtDescription.setText(description);
        txtStatus.setText(status);

        // Set badge color
        if (status.equalsIgnoreCase("Critical")) {
            txtStatus.setBackgroundResource(R.drawable.status_red);
        } else {
            txtStatus.setBackgroundResource(R.drawable.status_green);
        }

        // Make whole card clickable and navigate to Donation_details_fragment
        item.setOnClickListener(v -> {
            // CRITICAL FIX: Use Donation_details_fragment.newInstance()
            // and pass the category data (title, description, status, imageRes)
            Fragment nextFragment = Donation_details_fragment.newInstance(
                    title,
                    description,
                    status,
                    imageRes
            );

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .commit();
        });

        categoryContainer.addView(item);
    }
}