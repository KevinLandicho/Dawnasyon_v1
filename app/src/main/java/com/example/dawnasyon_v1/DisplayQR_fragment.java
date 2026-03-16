package com.example.dawnasyon_v1;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;

import java.io.OutputStream;

public class DisplayQR_fragment extends BaseFragment {

    private ImageView imgQrCode;
    private String currentUserId = null;

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

        // 2. Fetch & Display QR Code (Fast Cached Version)
        loadRealQrCode();

        // 3. Save to Gallery Button
        btnSave.setOnClickListener(v -> saveImageToGallery());

        // 4. TAP: View Full Screen
        imgQrCode.setOnClickListener(v -> showFullScreenQR());

        applyTagalogTranslation(view);
    }

    // ⭐ FAST GLIDE LOAD
    private void loadRealQrCode() {
        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        SupabaseJavaHelper.fetchUserProfile(getContext(), new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (!isAdded()) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                if (profile != null) {
                    String qrUrl = profile.getQr_code_url();
                    currentUserId = profile.getId();

                    if (qrUrl != null && !qrUrl.isEmpty()) {
                        // Native, lightning-fast Glide caching
                        Glide.with(DisplayQR_fragment.this)
                                .load(qrUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.ic_loading_qr) // Shows while downloading
                                .error(R.drawable.ic_loading_qr) // Failsafe
                                .into(imgQrCode);
                    } else {
                        // Instantly generates if no URL exists
                        generateLocalQr(currentUserId);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                Toast.makeText(getContext(), "Could not load QR Profile", Toast.LENGTH_SHORT).show();
            }
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

    private void showFullScreenQR() {
        Drawable drawable = imgQrCode.getDrawable();
        if (drawable == null) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullScreenImage = new ImageView(requireContext());
        fullScreenImage.setImageDrawable(drawable);
        fullScreenImage.setBackgroundColor(android.graphics.Color.WHITE);
        fullScreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        fullScreenImage.setOnClickListener(view -> dialog.dismiss());

        dialog.setContentView(fullScreenImage);
        dialog.show();
        Toast.makeText(getContext(), "Tap the QR code to close", Toast.LENGTH_SHORT).show();
    }

    private void saveImageToGallery() {
        imgQrCode.setDrawingCacheEnabled(true);
        Bitmap bitmap = null;

        try {
            Drawable drawable = imgQrCode.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            }
        } catch (Exception e) {
            // Ignored
        }

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
                if (imageUri != null) {
                    fos = requireContext().getContentResolver().openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    if (fos != null) fos.close();
                    Toast.makeText(getContext(), "QR Code saved to Gallery!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Saving not supported on this Android version", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}