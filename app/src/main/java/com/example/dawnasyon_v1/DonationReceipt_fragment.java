package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DonationReceipt_fragment extends BaseFragment {

    // You can add arguments here to pass dynamic data later
    // private static final String ARG_RECEIPT_ID = "receipt_id";

    public DonationReceipt_fragment() {
        // Required empty public constructor
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

        // For now, static data is in XML.
        // Later you can populate tv_receipt_no, tv_donor_name etc. using arguments.

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}