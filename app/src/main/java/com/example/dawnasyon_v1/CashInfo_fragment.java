// File: CashInfo_fragment.java
package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// This is a minimal placeholder for the CASH INFO screen
public class CashInfo_fragment extends Fragment {

    // Arguments needed for the header info
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_IMAGE = "arg_image";

    public CashInfo_fragment() {
        // Required empty public constructor
    }

    public static CashInfo_fragment newInstance(String title, String description, String status, int imageRes) {
        CashInfo_fragment fragment = new CashInfo_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_STATUS, status);
        args.putInt(ARG_IMAGE, imageRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // IMPORTANT: Ensure you have R.layout.fragment_cash_info defined
        // or change this to R.layout.donation_details temporarily for testing.
        return inflater.inflate(R.layout.fragment_cash_info, container, false);
    }
}