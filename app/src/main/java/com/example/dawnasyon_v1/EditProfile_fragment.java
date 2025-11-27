package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

// ⭐ FIX 1: Implement the required interface to receive the result from the picker
public class EditProfile_fragment extends Fragment implements AvatarPicker_fragment.OnAvatarSelectedListener {

    // You would typically use a ViewModel for real data, but for UI, we use fields:
    private TextInputEditText editName, editProvince, editCity, editBarangay, editStreet, editContact;

    // ⭐ FIX 2: Added required fields for profile image management
    private ImageView profilePic;
    private ImageView btnEditImage;
    private Button btnSaveProfile;

    // To store the current selected avatar resource ID for saving (start with default)
    private int currentProfileAvatarResId = R.drawable.ic_profile_avatar;

    public EditProfile_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Input Fields
        editName = view.findViewById(R.id.edit_name);
        editProvince = view.findViewById(R.id.edit_province);
        editCity = view.findViewById(R.id.edit_city);
        editBarangay = view.findViewById(R.id.edit_barangay);
        editStreet = view.findViewById(R.id.edit_street);
        editContact = view.findViewById(R.id.edit_contact);

        // ⭐ FIX 3: Initialize the profile picture ImageView
        profilePic = view.findViewById(R.id.profile_pic);
        btnEditImage = view.findViewById(R.id.btn_edit_image);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);

        // Set the current avatar (default or previously selected)
        profilePic.setImageResource(currentProfileAvatarResId);


        // 2. Setup Click Listeners

        btnEditImage.setOnClickListener(v -> {
            // Navigate to AvatarPicker_fragment, passing 'this' as the listener
            if (getActivity() != null) {
                AvatarPicker_fragment avatarPickerFragment = AvatarPicker_fragment.newInstance(this);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, avatarPickerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnSaveProfile.setOnClickListener(v -> {
            saveUserData();
        });
    }

    // ⭐ FIX 4: Implement the result callback method from the interface
    @Override
    public void onAvatarSelected(int selectedAvatarResId) {
        // This method receives the selected avatar ID when the user hits 'Select'
        this.currentProfileAvatarResId = selectedAvatarResId;
        if (profilePic != null) {
            profilePic.setImageResource(selectedAvatarResId);
        }
        Toast.makeText(getContext(), "Avatar updated on edit screen.", Toast.LENGTH_SHORT).show();
    }


    /**
     * Handles validation and saves updated user data.
     */
    private void saveUserData() {
        String name = editName.getText().toString().trim();
        String contact = editContact.getText().toString().trim();

        if (name.isEmpty() || contact.isEmpty()) {
            Toast.makeText(getContext(), "Name and Contact are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement logic to update the data in SQLite/Firebase

        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();

        // After successful save, navigate back to the main Profile Fragment
        if (getActivity() != null) {
            // Note: When you go back, the main Profile Fragment will need to be refreshed
            // or receive this updated avatar ID to show the change.
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}