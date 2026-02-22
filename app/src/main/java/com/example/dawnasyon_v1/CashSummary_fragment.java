package com.example.dawnasyon_v1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class CashSummary_fragment extends BaseFragment {

    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_REF_ID = "ref_id";

    private int mAmount;
    private String currentLinkId = null;

    private Button btnConfirm;
    private TextView tvRefId;

    public static CashSummary_fragment newInstance(int amount, String refId) {
        CashSummary_fragment fragment = new CashSummary_fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AMOUNT, amount);
        args.putString(ARG_REF_ID, refId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cash_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvAmount = view.findViewById(R.id.tv_summary_amount);
        tvRefId = view.findViewById(R.id.tv_summary_ref);
        btnConfirm = view.findViewById(R.id.btn_confirm_donation);
        Button btnChange = view.findViewById(R.id.btn_change_amount);

        if (getArguments() != null) {
            mAmount = getArguments().getInt(ARG_AMOUNT);
            tvAmount.setText("PHP " + mAmount + ".00");

            String refText = "Will be generated upon confirmation";
            tvRefId.setText(refText);
            // Translate initial text
            TranslationHelper.autoTranslate(getContext(), tvRefId, refText);
        }

        if (savedInstanceState != null) {
            currentLinkId = savedInstanceState.getString("SAVED_LINK_ID");
        }

        btnConfirm.setOnClickListener(v -> {
            if (currentLinkId == null) {
                startPaymentProcess();
            } else {
                verifyPayment();
            }
        });

        btnChange.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC TEXT
        applyTagalogTranslation(view);
    }

    private void startPaymentProcess() {
        btnConfirm.setEnabled(false);

        // ⭐ Translate Dynamic Button State
        String loadingText = "Generating Link...";
        btnConfirm.setText(loadingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, loadingText);

        Toast.makeText(getContext(), "Connecting to PayMongo...", Toast.LENGTH_SHORT).show();

        PayMongoHelper.createDonationLink(mAmount, "Donation", new PayMongoHelper.PaymentListener() {
            @Override
            public void onSuccess(String checkoutUrl, String linkId) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    currentLinkId = linkId;
                    tvRefId.setText(linkId);

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                    startActivity(intent);

                    // ⭐ Translate Next State
                    String verifyText = "Verify Payment";
                    btnConfirm.setText(verifyText);
                    TranslationHelper.autoTranslate(getContext(), btnConfirm, verifyText);

                    btnConfirm.setEnabled(true);

                    Toast.makeText(getContext(), "Please pay in browser, then return here.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);

                    // ⭐ Translate Reset State
                    String resetText = "CONFIRM DONATION";
                    btnConfirm.setText(resetText);
                    TranslationHelper.autoTranslate(getContext(), btnConfirm, resetText);

                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void verifyPayment() {
        if (currentLinkId == null) return;

        btnConfirm.setEnabled(false);

        // ⭐ Translate Verifying State
        String verifyingText = "Verifying...";
        btnConfirm.setText(verifyingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, verifyingText);

        PayMongoHelper.checkPaymentStatus(currentLinkId, status -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (status.equals("paid")) {
                    // PAYMENT SUCCESSFUL -> NOW SAVE TO DATABASE
                    saveToSupabaseAndProceed();
                } else {
                    btnConfirm.setEnabled(true);

                    // ⭐ Translate Reset State
                    String resetText = "Verify Payment";
                    btnConfirm.setText(resetText);
                    TranslationHelper.autoTranslate(getContext(), btnConfirm, resetText);

                    Toast.makeText(getContext(), "Status: " + status.toUpperCase() + ". Please complete payment.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ⭐ NEW: Saves the record to Supabase before navigating
    private void saveToSupabaseAndProceed() {
        if (getActivity() == null) return;

        // ⭐ Translate Saving State
        String savingText = "Saving Record...";
        btnConfirm.setText(savingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, savingText);

        // ⭐ FIXED: Added the `itemDesc` parameter (passed as null for Cash)
        DonationHelper.submitDonation(
                currentLinkId,
                new ArrayList<>(),
                "Cash",
                null, // <-- This is the missing itemDesc parameter!
                (double) mAmount,
                false,
                new DonationHelper.DonationCallback() {
                    @Override
                    public void onSuccess() {
                        // NOW we navigate
                        if (getActivity() == null) return;
                        Toast.makeText(getContext(), "Donation Verified & Saved!", Toast.LENGTH_SHORT).show();

                        Reference_fragment refFragment = Reference_fragment.newInstance(currentLinkId);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, refFragment)
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (getActivity() == null) return;
                        btnConfirm.setEnabled(true);

                        // ⭐ Translate Retry State
                        String retryText = "Retry Save";
                        btnConfirm.setText(retryText);
                        TranslationHelper.autoTranslate(getContext(), btnConfirm, retryText);

                        Toast.makeText(getContext(), "Payment received but failed to save record: " + message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentLinkId != null) {
            PayMongoHelper.checkPaymentStatus(currentLinkId, status -> {
                if (getActivity() != null && status.equals("paid")) {
                    getActivity().runOnUiThread(() -> {
                        // Prevent saving twice if already navigating
                        if (btnConfirm.isEnabled()) {
                            saveToSupabaseAndProceed();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SAVED_LINK_ID", currentLinkId);
    }
}