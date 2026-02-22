package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AvatarPicker_fragment extends BaseFragment {

    private RecyclerView avatarRecyclerView;
    private AvatarAdapter avatarAdapter;
    private Button btnSelectAvatar;
    private ImageView currentProfilePreview;
    private AlertDialog photoDialog;

    private final int[] avatarResources = {
            R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
            R.drawable.avatar4, R.drawable.avatar5, R.drawable.avatar6,
            R.drawable.avatar7, R.drawable.avatar8, R.drawable.ic_addbutton
    };

    private int selectedAvatarResId = -1;
    private Uri customAvatarUri = null;
    private String customAvatarUrl = null;

    // ⭐ NEW INSTANCE MAKER to receive current avatar
    public static AvatarPicker_fragment newInstance(int resId, String customAvatar) {
        AvatarPicker_fragment fragment = new AvatarPicker_fragment();
        Bundle args = new Bundle();
        args.putInt("CURRENT_RES_ID", resId);
        args.putString("CURRENT_CUSTOM_AVATAR", customAvatar);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleNewCustomAvatar(uri);
            }
    );

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    Uri savedUri = saveBitmapToCache(bitmap);
                    if (savedUri != null) handleNewCustomAvatar(savedUri);
                }
            }
    );

    public AvatarPicker_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarRecyclerView = view.findViewById(R.id.avatar_grid_recyclerview);
        btnSelectAvatar = view.findViewById(R.id.btn_select_avatar);
        currentProfilePreview = view.findViewById(R.id.current_profile_preview);

        // ⭐ INITIALIZE CURRENT AVATAR PREVIEW
        if (getArguments() != null) {
            selectedAvatarResId = getArguments().getInt("CURRENT_RES_ID", -1);
            String customPassed = getArguments().getString("CURRENT_CUSTOM_AVATAR", null);

            if (customPassed != null && !customPassed.isEmpty()) {
                if (customPassed.startsWith("http")) {
                    customAvatarUrl = customPassed;
                    selectedAvatarResId = -1;
                    loadAndCircleCropImage(customAvatarUrl, currentProfilePreview);
                } else {
                    customAvatarUri = Uri.parse(customPassed);
                    selectedAvatarResId = -1;
                    loadLocalUriAndCircleCrop(customAvatarUri, currentProfilePreview);
                }
            } else if (selectedAvatarResId != -1 && currentProfilePreview != null) {
                currentProfilePreview.setImageResource(selectedAvatarResId);
            }
        }

        avatarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        List<Integer> avatarList = new ArrayList<>();
        for (int resId : avatarResources) avatarList.add(resId);

        avatarAdapter = new AvatarAdapter(avatarList, avatarResId -> {
            if (avatarResId == R.drawable.ic_addbutton) {
                showPhotoOptionsDialog();
            } else {
                selectedAvatarResId = avatarResId;
                customAvatarUri = null;
                customAvatarUrl = null;
                if (currentProfilePreview != null) currentProfilePreview.setImageResource(avatarResId);
            }
        });
        avatarRecyclerView.setAdapter(avatarAdapter);

        btnSelectAvatar.setOnClickListener(v -> {
            Bundle result = new Bundle();
            if (customAvatarUri != null) {
                result.putString("selected_avatar_uri", customAvatarUri.toString());
                getParentFragmentManager().setFragmentResult("requestKey_avatar", result);
                getParentFragmentManager().popBackStack();
            } else if (selectedAvatarResId != -1) {
                result.putInt("selected_avatar_id", selectedAvatarResId);
                getParentFragmentManager().setFragmentResult("requestKey_avatar", result);
                getParentFragmentManager().popBackStack();
            } else if (customAvatarUrl != null) {
                getParentFragmentManager().popBackStack(); // Didn't change anything, just leave
            } else {
                Toast.makeText(getContext(), "Please select an avatar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPhotoOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_photo_options, null);
        builder.setView(dialogView);

        LinearLayout btnUpload = dialogView.findViewById(R.id.btn_upload);
        LinearLayout btnCapture = dialogView.findViewById(R.id.btn_capture);
        Button btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        photoDialog = builder.create();
        photoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        photoDialog.show();

        btnUpload.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCapture.setOnClickListener(v -> cameraLauncher.launch(null));
        btnClose.setOnClickListener(v -> photoDialog.dismiss());
    }

    private void handleNewCustomAvatar(Uri uri) {
        customAvatarUri = uri;
        customAvatarUrl = null;
        selectedAvatarResId = -1;

        if (currentProfilePreview != null) {
            loadLocalUriAndCircleCrop(uri, currentProfilePreview);
        }

        if (photoDialog != null && photoDialog.isShowing()) photoDialog.dismiss();
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "custom_avatar.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return Uri.fromFile(imageFile);
        } catch (IOException e) { return null; }
    }

    // =========================================================================
    // ⭐ NATIVE CIRCLE CROPPERS
    // =========================================================================
    private void loadAndCircleCropImage(String urlString, ImageView imageView) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true); connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap rawBitmap = BitmapFactory.decodeStream(input);
                if (rawBitmap != null) {
                    Bitmap circleBitmap = getCircularBitmap(rawBitmap);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && imageView != null) imageView.setImageBitmap(circleBitmap);
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    private void loadLocalUriAndCircleCrop(Uri uri, ImageView imageView) {
        new Thread(() -> {
            try {
                InputStream input = requireContext().getContentResolver().openInputStream(uri);
                Bitmap rawBitmap = BitmapFactory.decodeStream(input);
                if (rawBitmap != null) {
                    Bitmap circleBitmap = getCircularBitmap(rawBitmap);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && imageView != null) imageView.setImageBitmap(circleBitmap);
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int minEdge = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(minEdge, minEdge, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, minEdge, minEdge);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(minEdge / 2f, minEdge / 2f, minEdge / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int dx = (bitmap.getWidth() - minEdge) / 2;
        int dy = (bitmap.getHeight() - minEdge) / 2;
        Rect srcRect = new Rect(dx, dy, dx + minEdge, dy + minEdge);
        canvas.drawBitmap(bitmap, srcRect, rect, paint);
        return output;
    }
}