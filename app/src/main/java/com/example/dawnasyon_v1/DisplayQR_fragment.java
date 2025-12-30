package com.example.dawnasyon_v1;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

public class DisplayQR_fragment extends BaseFragment {

    private ImageView imgQrCode;

    public DisplayQR_fragment() {
        // Required empty public constructor
    }

    public static DisplayQR_fragment newInstance(int qrResId) {
        DisplayQR_fragment fragment = new DisplayQR_fragment();
        Bundle args = new Bundle();
        args.putInt("qr_res_id", qrResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_display_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgQrCode = view.findViewById(R.id.img_qr_code);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        Button btnSave = view.findViewById(R.id.btn_save_gallery);

        // 1. Back Button
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 2. Fetch & Display QR Code
        loadRealQrCode();

        // 3. Save to Gallery Button
        btnSave.setOnClickListener(v -> saveImageToGallery());
    }

    private void loadRealQrCode() {
        // ⭐ UPDATED: Changed SupabaseRegistrationHelper -> AuthHelper
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                String qrUrl = profile.getQr_code_url();

                if (qrUrl != null && !qrUrl.isEmpty()) {
                    // A. If URL exists in Supabase, load it with Glide
                    Glide.with(this)
                            .load(qrUrl)
                            .placeholder(R.drawable.ic_qrsample)
                            .error(R.drawable.ic_warning)
                            .into(imgQrCode);
                } else {
                    // B. Fallback: If no URL, generate one using ID
                    generateLocalQr(profile.getId());
                }
            } else if (isAdded()) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
            return null;
        });
    }

    private void generateLocalQr(String userId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400);
            imgQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("DisplayQR", "Error generating local QR: " + e.getMessage());
        }
    }

    private void saveImageToGallery() {
        imgQrCode.setDrawingCacheEnabled(true);
        Bitmap bitmap = ((BitmapDrawable) imgQrCode.getDrawable()).getBitmap();

        if (bitmap == null) {
            Toast.makeText(getContext(), "QR Code not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues resolver = new ContentValues();
                resolver.put(MediaStore.Images.Media.DISPLAY_NAME, "Dawnasyon_QR_" + System.currentTimeMillis() + ".jpg");
                resolver.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                resolver.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, resolver);
                fos = requireContext().getContentResolver().openOutputStream(imageUri);
            } else {
                Toast.makeText(getContext(), "Saving not supported on this Android version", Toast.LENGTH_SHORT).show();
                return;
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (fos != null) fos.close();

            Toast.makeText(getContext(), "QR Code saved to Gallery!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}