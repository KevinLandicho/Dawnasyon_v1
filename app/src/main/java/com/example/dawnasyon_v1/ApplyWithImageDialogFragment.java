package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ApplyWithImageDialogFragment extends DialogFragment {

    private ImageView imgPreview;
    private LinearLayout placeholderLayout;
    private Uri selectedImageUri = null;
    private byte[] imageBytes = null;
    private OnConfirmListener listener;

    public interface OnConfirmListener {
        void onConfirm(byte[] imageBytes);
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    // Image Picker Launcher
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPreview.setImageURI(uri);
                    imgPreview.setVisibility(View.VISIBLE);
                    placeholderLayout.setVisibility(View.GONE);
                    processImage(uri);
                }
            }
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_apply_with_image_dialog, null);

        imgPreview = view.findViewById(R.id.img_preview);
        placeholderLayout = view.findViewById(R.id.layout_upload_placeholder);
        Button btnSelectImage = view.findViewById(R.id.btn_select_image);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        // Select Image
        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/*"));

        // Cancel
        btnCancel.setOnClickListener(v -> dismiss());

        // Confirm
        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm(imageBytes); // Pass bytes (null if no image selected)
            }
            dismiss(); // Don't dismiss immediately if you want to show loading, but for now simple flow
        });

        builder.setView(view);
        return builder.create();
    }

    private void processImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }

            // Compress to JPG
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream); // 70% quality
            imageBytes = stream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }
}