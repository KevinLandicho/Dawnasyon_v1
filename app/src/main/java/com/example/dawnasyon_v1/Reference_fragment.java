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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import java.io.OutputStream;

public class Reference_fragment extends BaseFragment {

    private static final String ARG_REFERENCE_NO = "reference_number";
    private String referenceNo;

    public Reference_fragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this fragment.
     * @param referenceNumber The generated reference number to display.
     */
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            // 🟢 Set the generated reference number to the TextView
            txtReference.setText(referenceNo);
        } else {
            // Fallback text if the reference number was not passed
            txtReference.setText("N/A - ERROR");
        }

        // ====================================================
        // ⭐ UPDATED: BACK BUTTON LOGIC
        // ====================================================
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                // 1. Clear the history so they can't reverse back into this reference page
                getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // 2. Go directly to the Donation Page
                // ⚠️ IMPORTANT: If your donation page is named differently, change "Donation_fragment" to your actual class name!
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AddDonation_Fragment())
                        .commit();
            }
        });

        // ====================================================
        // ⭐ SAVE IMAGE LOGIC
        // ====================================================
        btnSaveImage.setOnClickListener(v -> {
            // 1. Temporarily hide the save button so it doesn't appear in the saved image
            btnSaveImage.setVisibility(View.INVISIBLE);

            // 2. Take a snapshot of the CardView
            Bitmap bitmap = getBitmapFromView(cardMain);

            // 3. Make the save button visible again on the screen
            btnSaveImage.setVisibility(View.VISIBLE);

            // 4. Save to gallery
            if (bitmap != null) {
                saveImageToGallery(bitmap);
            }
        });

        // ⭐ ENABLE AUTO-TRANSLATION (Translates "Thank You" and labels)
        applyTagalogTranslation(view);
    }

    // ====================================================
    // ⭐ HELPER: CONVERT VIEW TO BITMAP
    // ====================================================
    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    // ====================================================
    // ⭐ HELPER: SAVE BITMAP TO DEVICE GALLERY
    // ====================================================
    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Dawnasyon_Reference_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        // Use scoped storage for modern Android versions (Android 10+)
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