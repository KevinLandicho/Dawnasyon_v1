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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
// You might need to adjust imports if BarcodeEncoder is in a different package for your setup
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;

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

        // 2. Fetch & Display QR Code (Offline Ready)
        loadRealQrCode();

        // 3. Save to Gallery Button
        btnSave.setOnClickListener(v -> saveImageToGallery());

        // ⭐ ENABLE AUTO-TRANSLATION (Translates "Save to Gallery", etc.)
        applyTagalogTranslation(view);
    }

    private void loadRealQrCode() {
        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        // ⭐ UPDATED: Use SupabaseJavaHelper to utilize the Offline Cache
        SupabaseJavaHelper.fetchUserProfile(getContext(), new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (!isAdded()) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                if (profile != null) {
                    String qrUrl = profile.getQr_code_url();
                    String userId = profile.getId();

                    if (qrUrl != null && !qrUrl.isEmpty()) {
                        // A. Try to load the official QR Image
                        Glide.with(DisplayQR_fragment.this)
                                .load(qrUrl)
                                .placeholder(R.drawable.ic_qrsample) // Show sample while loading
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        // ⭐ FALLBACK: If Offline and Image fails to load, GENERATE IT LOCALLY
                                        // This ensures the user always has a QR code
                                        generateLocalQr(userId);
                                        return true; // Return true to indicate we handled the error
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        return false;
                                    }
                                })
                                .into(imgQrCode);
                    } else {
                        // B. If no URL in database, generate one immediately
                        generateLocalQr(userId);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                // Even if fetching fails (rare with cache), try to generate if we have a cached user ID elsewhere
                // For now, just show error
                Toast.makeText(getContext(), "Could not load QR Profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateLocalQr(String userId) {
        try {
            // This works 100% Offline because it just encodes text to image
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400);
            imgQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("DisplayQR", "Error generating local QR: " + e.getMessage());
        }
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
            // If drawable is not a bitmap (e.g. vector placeholder), ignore
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