package com.example.dawnasyon_v1;

import android.os.Bundle;
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

public class EditProfile_fragment extends BaseFragment {

    private TextInputEditText editName, editProvince, editCity, editBarangay, editStreet, editContact;
    private ImageView profilePic;
    private ImageView btnEditImage;
    private Button btnSaveProfile;

    // Default avatar
    private int currentProfileAvatarResId = R.drawable.avatar1;

    // ⭐ NEW FLAG: Tracks if user manually changed the avatar in this session
    private boolean isAvatarUpdated = false;

    public EditProfile_fragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Listen for result from AvatarPicker
        getParentFragmentManager().setFragmentResultListener("requestKey_avatar", this, (requestKey, bundle) -> {
            int selectedId = bundle.getInt("selected_avatar_id");

            Log.d("EditProfile", "Avatar updated locally to ID: " + selectedId);

            // 1. Update variable
            this.currentProfileAvatarResId = selectedId;

            // ⭐ 2. MARK AS UPDATED so the database fetch doesn't overwrite it
            this.isAvatarUpdated = true;

            // 3. Update UI immediately
            if (profilePic != null) {
                profilePic.setImageResource(selectedId);
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

        // Initialize Views
        editName = view.findViewById(R.id.edit_name);
        editProvince = view.findViewById(R.id.edit_province);
        editCity = view.findViewById(R.id.edit_city);
        editBarangay = view.findViewById(R.id.edit_barangay);
        editStreet = view.findViewById(R.id.edit_street);
        editContact = view.findViewById(R.id.edit_contact);

        profilePic = view.findViewById(R.id.profile_pic);
        btnEditImage = view.findViewById(R.id.btn_edit_image);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);

        // ⭐ Set the image immediately (In case listener fired before view creation)
        if (profilePic != null) {
            profilePic.setImageResource(currentProfileAvatarResId);
        }

        // Load Data from Database
        loadCurrentUserProfile();

        // Open Picker
        btnEditImage.setOnClickListener(v -> {
            AvatarPicker_fragment avatarPickerFragment = new AvatarPicker_fragment();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, avatarPickerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Save
        btnSaveProfile.setOnClickListener(v -> saveUserData());
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

                // ⭐ CRITICAL FIX: Only overwrite the avatar if the user HAS NOT picked a new one yet
                if (!isAvatarUpdated) {
                    String dbAvatarName = profile.getAvatarName();
                    currentProfileAvatarResId = AvatarHelper.getDrawableId(getContext(), dbAvatarName);
                    if (profilePic != null) {
                        profilePic.setImageResource(currentProfileAvatarResId);
                    }
                } else {
                    Log.d("EditProfile", "Skipping DB avatar load because user selected a new one.");
                }
            }
            return null;
        });
    }

    private void saveUserData() {
        String name = editName.getText().toString().trim();
        String contact = editContact.getText().toString().trim();
        String province = editProvince.getText().toString().trim();
        String city = editCity.getText().toString().trim();
        String barangay = editBarangay.getText().toString().trim();
        String street = editStreet.getText().toString().trim();

        if (name.isEmpty() || contact.isEmpty()) {
            Toast.makeText(getContext(), "Name and Contact are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get String Name using Helper
        String avatarName = AvatarHelper.getResourceName(getContext(), currentProfileAvatarResId);

        // Show loading
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }

        SupabaseJavaHelper.updateUserProfile(
                name, contact, province, city, barangay, street, avatarName,
                new SupabaseJavaHelper.ProfileUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            if (getActivity() instanceof BaseActivity) {
                                ((BaseActivity) getActivity()).hideLoading();
                            }
                            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                            if (getActivity() != null) {
                                getActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            if (getActivity() instanceof BaseActivity) {
                                ((BaseActivity) getActivity()).hideLoading();
                            }
                            Toast.makeText(getContext(), "Update failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }
}