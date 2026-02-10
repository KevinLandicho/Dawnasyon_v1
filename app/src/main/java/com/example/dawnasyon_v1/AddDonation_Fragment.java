package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class AddDonation_Fragment extends BaseFragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public AddDonation_Fragment() {
        // Required empty public constructor
    }

    public static AddDonation_Fragment newInstance(String param1, String param2) {
        AddDonation_Fragment fragment = new AddDonation_Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Reference your button
        Button btnDonate = view.findViewById(R.id.btnDonate);

        // Set click listener
        btnDonate.setOnClickListener(v -> {
            // Create instance of DonateOptions_Fragment
            Fragment donateOptionsFragment = new DonationOptions_fragments();

            // Replace current fragment with DonateOptions_fragment
            if (getActivity() != null) {
                FragmentTransaction transaction = getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();

                transaction.replace(R.id.fragment_container, donateOptionsFragment);
                transaction.addToBackStack(null); // Allows user to go back
                transaction.commit();
            }
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR THIS SCREEN
        applyTagalogTranslation(view);
    }
}