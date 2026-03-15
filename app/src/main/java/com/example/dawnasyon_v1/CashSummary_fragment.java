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
    private static final String ARG_METHOD = "method"; // ⭐ Changed to METHOD

    private int mAmount;
    private String mMethod;
    private String currentLinkId = null;

    private Button btnConfirm;
    private TextView tvRefId;
    private TextView tvMethod;

    public static CashSummary_fragment newInstance(int amount, String method) {
        CashSummary_fragment fragment = new CashSummary_fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AMOUNT, amount);
        args.putString(ARG_METHOD, method);
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
        tvMethod = view.findViewById(R.id.tv_summary_method);
        tvRefId = view.findViewById(R.id.tv_summary_ref);
        btnConfirm = view.findViewById(R.id.btn_confirm_donation);
        Button btnChange = view.findViewById(R.id.btn_change_amount);

        if (getArguments() != null) {
            mAmount = getArguments().getInt(ARG_AMOUNT);
            mMethod = getArguments().getString(ARG_METHOD);

            tvAmount.setText("PHP " + mAmount + ".00");

            // ⭐ Setup UI based on Method
            if ("Physical".equals(mMethod)) {
                tvMethod.setText("Physical Drop-off");
                btnConfirm.setText("CONFIRM DROP-OFF");
            } else {
                tvMethod.setText("Online Payment");
            }

            String refText = "Will be generated upon confirmation";
            tvRefId.setText(refText);
            TranslationHelper.autoTranslate(getContext(), tvRefId, refText);
        }

        if (savedInstanceState != null) {
            currentLinkId = savedInstanceState.getString("SAVED_LINK_ID");
        }

        btnConfirm.setOnClickListener(v -> {
            if ("Physical".equals(mMethod)) {
                // ⭐ Bypass PayMongo completely for physical drop-offs
                processPhysicalDonation();
            } else {
                // ⭐ Online Logic
                if (currentLinkId == null) {
                    startPaymentProcess();
                } else {
                    verifyPayment();
                }
            }
        });

        btnChange.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        applyTagalogTranslation(view);
    }

    // ⭐ NEW: Handles Physical Cash Drop-off Logic
    private void processPhysicalDonation() {
        btnConfirm.setEnabled(false);

        String loadingText = "Saving Record...";
        btnConfirm.setText(loadingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, loadingText);

        // Generate a custom physical reference ID
        currentLinkId = "PHY-" + System.currentTimeMillis();
        tvRefId.setText(currentLinkId);

        // Submit to Supabase directly
        DonationHelper.submitDonation(
                currentLinkId,
                new ArrayList<>(),
                "Cash",
                "Physical Drop-off", // Pass this to DB so admin knows it's physical
                (double) mAmount,
                false,
                new DonationHelper.DonationCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        Toast.makeText(getContext(), "Donation Submitted! Please bring cash to Barangay.", Toast.LENGTH_LONG).show();

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

                        String retryText = "Retry Confirmation";
                        btnConfirm.setText(retryText);
                        TranslationHelper.autoTranslate(getContext(), btnConfirm, retryText);

                        Toast.makeText(getContext(), "Failed to save record: " + message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    // --- PAYMONGO ONLINE LOGIC ---

    private void startPaymentProcess() {
        btnConfirm.setEnabled(false);

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

        String verifyingText = "Verifying...";
        btnConfirm.setText(verifyingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, verifyingText);

        PayMongoHelper.checkPaymentStatus(currentLinkId, status -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (status.equals("paid")) {
                    saveToSupabaseAndProceed();
                } else {
                    btnConfirm.setEnabled(true);

                    String resetText = "Verify Payment";
                    btnConfirm.setText(resetText);
                    TranslationHelper.autoTranslate(getContext(), btnConfirm, resetText);

                    Toast.makeText(getContext(), "Status: " + status.toUpperCase() + ". Please complete payment.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveToSupabaseAndProceed() {
        if (getActivity() == null) return;

        String savingText = "Saving Record...";
        btnConfirm.setText(savingText);
        TranslationHelper.autoTranslate(getContext(), btnConfirm, savingText);

        DonationHelper.submitDonation(
                currentLinkId,
                new ArrayList<>(),
                "Cash",
                "Online Payment", // Added descriptor
                (double) mAmount,
                false,
                new DonationHelper.DonationCallback() {
                    @Override
                    public void onSuccess() {
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
        if (currentLinkId != null && "Online".equals(mMethod)) {
            PayMongoHelper.checkPaymentStatus(currentLinkId, status -> {
                if (getActivity() != null && status.equals("paid")) {
                    getActivity().runOnUiThread(() -> {
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