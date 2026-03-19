package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
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

    // ⭐ TRANSACTION LIMITS
    private static final int MIN_LIMIT_PHYSICAL = 1;
    private static final int MIN_LIMIT_ONLINE = 100; // PayMongo Minimum
    private static final int MAX_LIMIT = 20000;

    private String fTitle, fDescription, fStatus;
    private int fImageRes;

    private EditText etOtherAmount;
    private Button btnConfirmOther;
    private RadioGroup rgPaymentMethod;

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

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToOptions();
            }
        });

        setupHeader(view);

        rgPaymentMethod = view.findViewById(R.id.rg_payment_method);

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
                long amount = Long.parseLong(otherAmountStr);
                goToSummary((int) amount);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid Amount", Toast.LENGTH_SHORT).show();
            }
        });

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBackToOptions());
        }

        applyTagalogTranslation(view);
    }

    private void goBackToOptions() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DonationOptions_fragments())
                .commit();
    }

    private void goToSummary(int amount) {
        // ⭐ Determine the selected method first
        String method = "Online";
        if (rgPaymentMethod != null && rgPaymentMethod.getCheckedRadioButtonId() == R.id.rb_physical) {
            method = "Physical";
        }

        // ⭐ NEW: Strict validation based on selected method
        if ("Online".equals(method) && amount < MIN_LIMIT_ONLINE) {
            Toast.makeText(getContext(), "Minimum of PHP 100.00 is required for Online Payments.", Toast.LENGTH_SHORT).show();
            return;
        } else if ("Physical".equals(method) && amount < MIN_LIMIT_PHYSICAL) {
            Toast.makeText(getContext(), "Please enter a valid amount (Minimum ₱" + MIN_LIMIT_PHYSICAL + ").", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount > MAX_LIMIT) {
            Toast.makeText(getContext(), "Maximum donation is ₱" + MAX_LIMIT + " per transaction.", Toast.LENGTH_SHORT).show();
            return;
        }

        CashSummary_fragment summaryFragment = CashSummary_fragment.newInstance(amount, method);

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