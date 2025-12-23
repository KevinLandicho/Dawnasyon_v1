package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class CashInfo_fragment extends BaseFragment {

    // Arguments from DonationDetails Fragment
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_IMAGE = "arg_image";

    private String fTitle;
    private String fDescription;
    private String fStatus;
    private int fImageRes;

    private String selectedPaymentMethod = null;
    private String selectedAmount = null;

    private EditText etOtherAmount;
    private Button btnConfirmOther;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cash_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupHeader(view);
        setupPaymentButtons(view);

        GridLayout amountGrid = view.findViewById(R.id.amount_grid);
        if (amountGrid != null) {
            setupAmountGrid(amountGrid);
        }

        // Initialize "Other" amount views
        etOtherAmount = view.findViewById(R.id.et_other_amount);
        btnConfirmOther = view.findViewById(R.id.btn_confirm_other);

        // Logic for "Other" amount confirm button
        btnConfirmOther.setOnClickListener(v -> {
            String otherAmount = etOtherAmount.getText().toString().trim();
            if (otherAmount.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            // Format it nicely
            String formattedAmount = "PHP " + otherAmount;
            selectedAmount = formattedAmount;
            showQrDialog(formattedAmount);
        });

        Button btnBack = view.findViewById(R.id.btnBack);
        Button btnStep3 = view.findViewById(R.id.btnStep3);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Navigate to Upload Proof Screen
        btnStep3.setOnClickListener(v -> {
            if (selectedPaymentMethod == null) {
                Toast.makeText(getContext(), "Please select a payment method first.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedAmount == null) {
                // If they typed in "Other" but didn't click Scan, check if the box has text
                String otherAmount = etOtherAmount.getText().toString().trim();
                if (!otherAmount.isEmpty()) {
                    selectedAmount = "PHP " + otherAmount;
                } else {
                    Toast.makeText(getContext(), "Please select or enter an amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, CashUploadProof_fragment.newInstance(selectedAmount))
                    .addToBackStack(null)
                    .commit();
        });
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

    private void setupPaymentButtons(View view) {
        View gcashLayout = view.findViewById(R.id.gcash_button_layout);
        View mayaLayout = view.findViewById(R.id.maya_button_layout);

        // Get references to the actual buttons
        MaterialButton btnGcash = gcashLayout.findViewById(R.id.btn_payment_method);
        MaterialButton btnMaya = mayaLayout.findViewById(R.id.btn_payment_method);

        // --- Configure GCash (Initial State) ---
        btnGcash.setText("");
        btnGcash.setIconResource(R.drawable.gcash);
        btnGcash.setIconTint(null);
        btnGcash.setBackgroundTintList(null);
        btnGcash.setBackgroundResource(R.drawable.bg_option_unselected);

        // --- Configure Maya (Initial State) ---
        btnMaya.setText("");
        btnMaya.setIconResource(R.drawable.maya);
        btnMaya.setIconTint(null);
        btnMaya.setBackgroundTintList(null);
        btnMaya.setBackgroundResource(R.drawable.bg_option_unselected);

        // Click Listeners
        btnGcash.setOnClickListener(v -> {
            selectedPaymentMethod = "GCash";
            updateButtonVisuals(btnGcash, btnMaya);
            Toast.makeText(getContext(), "GCash Selected", Toast.LENGTH_SHORT).show();
        });

        btnMaya.setOnClickListener(v -> {
            selectedPaymentMethod = "Maya";
            updateButtonVisuals(btnMaya, btnGcash);
            Toast.makeText(getContext(), "Maya Selected", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateButtonVisuals(MaterialButton selected, MaterialButton other) {
        selected.setBackgroundTintList(null);
        selected.setBackgroundResource(R.drawable.bg_option_selected);
        other.setBackgroundTintList(null);
        other.setBackgroundResource(R.drawable.bg_option_unselected);
    }

    private void setupAmountGrid(GridLayout grid) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof Button) {
                Button btnAmount = (Button) child;
                btnAmount.setOnClickListener(v -> {
                    // Clear the "Other" input if they select a preset
                    if (etOtherAmount != null) etOtherAmount.setText("");

                    String amountText = btnAmount.getText().toString();
                    selectedAmount = amountText;
                    showQrDialog(amountText);
                });
            }
        }
    }

    private void showQrDialog(String amountText) {
        if (selectedPaymentMethod == null) {
            Toast.makeText(getContext(), "Please select a payment method (GCash or Maya) first.", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        ImageView imgQrCode = dialogView.findViewById(R.id.img_qr_code);
        TextView tvDialogAmount = dialogView.findViewById(R.id.tv_dialog_amount);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);

        tvDialogAmount.setText(amountText);
        tvDialogTitle.setText("Scan to Donate via " + selectedPaymentMethod);

        int qrDrawableId = getQrDrawableId(selectedPaymentMethod, amountText);
        imgQrCode.setImageResource(qrDrawableId);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private int getQrDrawableId(String paymentMethod, String amountText) {
        // Simple logic to return the QR code.
        // Since it's dynamic, you can just return one default QR code for now
        // or add logic to check ranges if you have multiple images.

        if (paymentMethod.equals("GCash")) {
            return R.drawable.qr_gcash_50; // Replace with a generic GCash QR if you have one
        } else {
            return R.drawable.qr_gcash_50; // Replace with a generic Maya QR
        }
    }
}