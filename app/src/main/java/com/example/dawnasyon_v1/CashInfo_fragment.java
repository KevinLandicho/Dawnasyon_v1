package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CashInfo_fragment extends BaseFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_IMAGE = "arg_image";

    private String fTitle, fDescription, fStatus;
    private int fImageRes;

    private EditText etOtherAmount;
    private Button btnConfirmOther;

    public CashInfo_fragment() {}

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fTitle = getArguments().getString(ARG_TITLE);
            fDescription = getArguments().getString(ARG_DESCRIPTION);
            fStatus = getArguments().getString(ARG_STATUS);
            fImageRes = getArguments().getInt(ARG_IMAGE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cash_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ⭐ 1. SYSTEM BACK BUTTON: Forces navigation to DonationOption_fragment
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToOptions();
            }
        });

        setupHeader(view);

        View methodLabel = view.findViewById(R.id.txt_method_label);
        View methodContainer = view.findViewById(R.id.payment_methods_container);
        if (methodLabel != null) methodLabel.setVisibility(View.GONE);
        if (methodContainer != null) methodContainer.setVisibility(View.GONE);

        GridLayout amountGrid = view.findViewById(R.id.amount_grid);
        if (amountGrid != null) setupAmountGrid(amountGrid);

        etOtherAmount = view.findViewById(R.id.et_other_amount);
        btnConfirmOther = view.findViewById(R.id.btn_confirm_other);
        btnConfirmOther.setText("Review");

        btnConfirmOther.setOnClickListener(v -> {
            String otherAmountStr = etOtherAmount.getText().toString().trim();
            if (otherAmountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int amount = Integer.parseInt(otherAmountStr);
                goToSummary(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid Amount", Toast.LENGTH_SHORT).show();
            }
        });

        // ⭐ 2. UI BACK BUTTON: Also goes to DonationOption_fragment
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBackToOptions());
        }
    }

    // ⭐ HELPER: Navigate explicitly to DonationOption_fragment
    private void goBackToOptions() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DonationOptions_fragments())
                .commit();
    }

    private void goToSummary(int amount) {
        if (amount < 100) {
            Toast.makeText(getContext(), "Minimum donation is PHP 100.", Toast.LENGTH_SHORT).show();
            return;
        }

        CashSummary_fragment summaryFragment = CashSummary_fragment.newInstance(amount, "PENDING");

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, summaryFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupHeader(View view) {
        TextView tvTitle = view.findViewById(R.id.detailsTitle);
        TextView tvDesc = view.findViewById(R.id.detailsDescription);
        TextView tvStatus = view.findViewById(R.id.detailsStatus);
        ImageView imgImage = view.findViewById(R.id.detailsImage);

        if (tvTitle != null) tvTitle.setText(fTitle);
        if (tvDesc != null) tvDesc.setText(fDescription);
        if (tvStatus != null) {
            tvStatus.setText(fStatus);
            if ("Critical".equalsIgnoreCase(fStatus)) {
                tvStatus.setBackgroundResource(R.drawable.status_red);
            } else {
                tvStatus.setBackgroundResource(R.drawable.status_green);
            }
        }
        if (imgImage != null) imgImage.setImageResource(fImageRes);
    }

    private void setupAmountGrid(GridLayout grid) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof Button) {
                Button btnAmount = (Button) child;
                btnAmount.setOnClickListener(v -> {
                    String rawText = btnAmount.getText().toString();
                    String cleanText = rawText.replaceAll("[^0-9]", "");
                    try {
                        int amount = Integer.parseInt(cleanText);
                        goToSummary(amount);
                    } catch (NumberFormatException e) {}
                });
            }
        }
    }
}