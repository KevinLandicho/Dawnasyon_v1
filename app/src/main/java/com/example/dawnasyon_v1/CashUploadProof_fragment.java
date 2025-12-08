package com.example.dawnasyon_v1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary;

public class CashUploadProof_fragment extends BaseFragment {

    private static final String ARG_AMOUNT = "arg_amount"; // Key for the amount
    private String donationAmount; // Variable to hold the amount

    private ImageView imgPreview;
    private Button btnSubmit;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                        imgPreview.setImageBitmap(bitmap);
                        imgPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imgPreview.setPadding(0,0,0,0);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    public CashUploadProof_fragment() {
        // Required empty public constructor
    }

    // ⭐ NEW: Factory method to accept the amount ⭐
    public static CashUploadProof_fragment newInstance(String amount) {
        CashUploadProof_fragment fragment = new CashUploadProof_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            donationAmount = getArguments().getString(ARG_AMOUNT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cash_upload_proof, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPreview = view.findViewById(R.id.img_proof_preview);
        Button btnUpload = view.findViewById(R.id.btn_upload);
        btnSubmit = view.findViewById(R.id.btn_submit);

        btnUpload.setOnClickListener(v -> openGallery());

        btnSubmit.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(getContext(), "Please upload a proof of payment first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ UPDATE: Use the real amount in the summary item ⭐
            ArrayList<ItemForSummary> cashDonationItem = new ArrayList<>();
            // Pass the saved amount (e.g., "PHP 500") instead of "Processing"
            cashDonationItem.add(new ItemForSummary("Cash Donation", donationAmount));

            if (getActivity() != null) {
                Fragment summaryFragment = Summary_fragment.newInstance(cashDonationItem);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, summaryFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }
}