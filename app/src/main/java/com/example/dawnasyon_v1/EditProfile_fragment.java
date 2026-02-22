package com.example.dawnasyon_v1;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class EditProfile_fragment extends BaseFragment {

    private TextInputEditText editName, editProvince, editCity, editBarangay, editStreet, editContact;
    private ImageView profilePic;
    private ImageView btnEditImage;
    private Button btnSaveProfile;

    // State Tracking
    private int currentProfileAvatarResId = R.drawable.avatar1;
    private Uri customAvatarUri = null; // New local photo
    private String currentDbAvatarUrl = null; // Existing DB photo

    private boolean isAvatarUpdated = false;

    public EditProfile_fragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("requestKey_avatar", this, (requestKey, bundle) -> {
            if (bundle.containsKey("selected_avatar_uri")) {
                String uriString = bundle.getString("selected_avatar_uri");
                customAvatarUri = Uri.parse(uriString);
                currentProfileAvatarResId = -1;
                currentDbAvatarUrl = null;
                isAvatarUpdated = true;

                if (profilePic != null) {
                    loadLocalUriAndCircleCrop(customAvatarUri, profilePic);
                }
            } else if (bundle.containsKey("selected_avatar_id")) {
                int selectedId = bundle.getInt("selected_avatar_id");
                currentProfileAvatarResId = selectedId;
                customAvatarUri = null;
                currentDbAvatarUrl = null;
                isAvatarUpdated = true;

                if (profilePic != null) {
                    profilePic.setImageResource(selectedId);
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editName = view.findViewById(R.id.edit_name);
        editProvince = view.findViewById(R.id.edit_province);
        editCity = view.findViewById(R.id.edit_city);
        editBarangay = view.findViewById(R.id.edit_barangay);
        editStreet = view.findViewById(R.id.edit_street);
        editContact = view.findViewById(R.id.edit_contact);

        profilePic = view.findViewById(R.id.profile_pic);
        btnEditImage = view.findViewById(R.id.btn_edit_image);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);

        loadCurrentUserProfile();

        // ⭐ Pass the current avatar data to the Picker
        btnEditImage.setOnClickListener(v -> {
            String customPass = null;
            if (customAvatarUri != null) customPass = customAvatarUri.toString();
            else if (currentDbAvatarUrl != null) customPass = currentDbAvatarUrl;

            AvatarPicker_fragment picker = AvatarPicker_fragment.newInstance(currentProfileAvatarResId, customPass);

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, picker)
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnSaveProfile.setOnClickListener(v -> handleSaveProcess());

        applyTagalogTranslation(view);
    }

    private void loadCurrentUserProfile() {
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                if (editName != null) editName.setText(profile.getFull_name());
                if (editContact != null) editContact.setText(profile.getContact_number());
                if (editProvince != null) editProvince.setText(profile.getProvince());
                if (editCity != null) editCity.setText(profile.getCity());
                if (editBarangay != null) editBarangay.setText(profile.getBarangay());
                if (editStreet != null) editStreet.setText(profile.getStreet());

                applyLockingLogic(profile.getType());

                if (!isAvatarUpdated) {
                    String dbAvatarName = profile.getAvatarName();
                    if (dbAvatarName != null && dbAvatarName.startsWith("http")) {
                        currentDbAvatarUrl = dbAvatarName;
                        currentProfileAvatarResId = -1;
                        if (profilePic != null) loadAndCircleCropImage(dbAvatarName, profilePic);
                    } else {
                        currentProfileAvatarResId = AvatarHelper.getDrawableId(getContext(), dbAvatarName);
                        if (profilePic != null) profilePic.setImageResource(currentProfileAvatarResId);
                    }
                }
            }
            return null;
        });
    }

    private void applyLockingLogic(String userType) {
        boolean isResident = userType != null && userType.equalsIgnoreCase("Resident");
        editName.setEnabled(false);
        editContact.setEnabled(true);
        if (isResident) {
            editProvince.setEnabled(false); editCity.setEnabled(false);
            editBarangay.setEnabled(false); editStreet.setEnabled(false);
        } else {
            editProvince.setEnabled(true); editCity.setEnabled(true);
            editBarangay.setEnabled(true); editStreet.setEnabled(true);
        }
    }

    private void handleSaveProcess() {
        String name = editName.getText().toString().trim();
        String contact = editContact.getText().toString().trim();

        if (name.isEmpty() || contact.isEmpty()) {
            Toast.makeText(getContext(), "Name and Contact are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        if (customAvatarUri != null) {
            byte[] imageBytes = getBytesFromUri(customAvatarUri);
            if (imageBytes != null) {
                SupabaseJavaHelper.uploadApplicationImage(imageBytes, publicUrl -> {
                    if (publicUrl != null) saveUserDataToDatabase(publicUrl);
                    else {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                        Toast.makeText(getContext(), "Failed to upload image.", Toast.LENGTH_SHORT).show();
                    }
                    return null;
                });
            } else {
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                Toast.makeText(getContext(), "Error reading image file.", Toast.LENGTH_SHORT).show();
            }
        } else if (currentDbAvatarUrl != null) {
            saveUserDataToDatabase(currentDbAvatarUrl);
        } else {
            String avatarName = AvatarHelper.getResourceName(getContext(), currentProfileAvatarResId);
            saveUserDataToDatabase(avatarName);
        }
    }

    private void saveUserDataToDatabase(String finalAvatarValue) {
        String name = editName.getText().toString().trim();
        String contact = editContact.getText().toString().trim();
        String province = editProvince.getText().toString().trim();
        String city = editCity.getText().toString().trim();
        String barangay = editBarangay.getText().toString().trim();
        String street = editStreet.getText().toString().trim();

        SupabaseJavaHelper.updateUserProfile(requireContext(), name, contact, province, city, barangay, street, finalAvatarValue, new SupabaseJavaHelper.ProfileUpdateCallback() {
            @Override public void onSuccess() {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                }
            }
            @Override public void onError(String message) {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    Toast.makeText(getContext(), "Update failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private byte[] getBytesFromUri(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
            return byteBuffer.toByteArray();
        } catch (Exception e) { return null; }
    }

    // =========================================================================
    // ⭐ NATIVE CIRCLE CROPPERS
    // =========================================================================
    private void loadAndCircleCropImage(String urlString, ImageView imageView) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap rawBitmap = BitmapFactory.decodeStream(input);
                if (rawBitmap != null) {
                    Bitmap circleBitmap = getCircularBitmap(rawBitmap);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && imageView != null) imageView.setImageBitmap(circleBitmap);
                    });
                }
            } catch (Exception e) { Log.e("Avatar", "Failed to load: " + e.getMessage()); }
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
            } catch (Exception e) { Log.e("Avatar", "Failed local load: " + e.getMessage()); }
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