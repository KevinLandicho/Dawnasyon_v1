package com.example.dawnasyon_v1;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;

import java.io.OutputStream;

public class Reference_fragment extends BaseFragment {

    private static final String ARG_REFERENCE_NO = "reference_number";
    private String referenceNo;

    public Reference_fragment() {
        // Required empty public constructor
    }

    public static Reference_fragment newInstance(String referenceNumber) {
        Reference_fragment fragment = new Reference_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_REFERENCE_NO, referenceNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            referenceNo = getArguments().getString(ARG_REFERENCE_NO);
        }

        // ====================================================
        // ⭐ NEW: HANDLE PHYSICAL PHONE BACK BUTTON
        // ====================================================
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToDonationPage();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reference, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtReference = view.findViewById(R.id.txtReference);
        CardView cardMain = view.findViewById(R.id.cardMain);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        ImageButton btnSaveImage = view.findViewById(R.id.btn_save_image);

        if (referenceNo != null) {
            txtReference.setText(referenceNo);
        } else {
            txtReference.setText("N/A - ERROR");
        }

        // ⭐ UI BACK BUTTON LOGIC
        btnBack.setOnClickListener(v -> navigateToDonationPage());

        // ⭐ SAVE IMAGE LOGIC
        btnSaveImage.setOnClickListener(v -> {
            btnSaveImage.setVisibility(View.INVISIBLE);
            Bitmap bitmap = getBitmapFromView(cardMain);
            btnSaveImage.setVisibility(View.VISIBLE);
            if (bitmap != null) {
                saveImageToGallery(bitmap);
            }
        });

        applyTagalogTranslation(view);
    }

    // ====================================================
    // ⭐ HELPER: NAVIGATION LOGIC (Used by both buttons)
    // ====================================================
    private void navigateToDonationPage() {
        if (getParentFragmentManager() != null) {
            // 1. Clear the entire backstack history so they can't go back to Summary
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // 2. Replace with the target fragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddDonation_Fragment())
                    .commit();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Dawnasyon_Reference_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dawnasyon");
        }

        try {
            Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(getContext(), "Reference Card saved to Gallery!", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }
}