package com.example.dawnasyon_v1;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.OutputStream;

public class DisplayQR_fragment extends BaseFragment {

    private static final String ARG_QR_RES_ID = "qr_res_id";
    private int qrResId;
    private ImageView imgQrCode;

    public DisplayQR_fragment() {}

    public static DisplayQR_fragment newInstance(int qrResId) {
        DisplayQR_fragment fragment = new DisplayQR_fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_QR_RES_ID, qrResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            qrResId = getArguments().getInt(ARG_QR_RES_ID);
        }
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
        Button btnSave = view.findViewById(R.id.btn_save_gallery);
        ImageButton btnBack = view.findViewById(R.id.btn_back);

        // Set the QR Image based on what was passed
        if (qrResId != 0) {
            imgQrCode.setImageResource(qrResId);
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Save to Gallery Logic
        btnSave.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) imgQrCode.getDrawable()).getBitmap();
            saveImageToGallery(bitmap);
        });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "QR_CODE_" + System.currentTimeMillis() + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        // Save to "Pictures/Dawnasyon" folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dawnasyon");
        }

        Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        try {
            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                if (outputStream != null) outputStream.close();

                Toast.makeText(getContext(), "QR Code saved to Gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }
}