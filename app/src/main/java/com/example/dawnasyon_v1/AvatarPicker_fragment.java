package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable; // Needed if listener were passed via arguments, but using field is simpler

/**
 * Fragment to display a grid of avatars for selection.
 */
public class AvatarPicker_fragment extends BaseFragment {

    // 1. Interface for communicating the selection back to the calling Fragment
    public interface OnAvatarSelectedListener extends Serializable {
        void onAvatarSelected(int selectedAvatarResId);
    }

    private OnAvatarSelectedListener listener;
    private RecyclerView avatarRecyclerView;
    private AvatarAdapter avatarAdapter;
    private Button btnSelectAvatar;

    // List of drawable resource IDs for the avatars
    // NOTE: You must ensure these drawables (avatar_1, avatar_2, etc.) exist in res/drawable
    private final int[] avatarResources = {
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5,
            R.drawable.avatar6,
            R.drawable.avatar7,
            R.drawable.avatar8,
            R.drawable.avatar9
    };

    private int selectedAvatarResId = -1; // Stores the resource ID of the currently selected avatar

    public AvatarPicker_fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance and pass the listener.
     * This replaces the generic newInstance template.
     * * @param listener The object that will receive the selected avatar ID.
     * @return A new instance of fragment AvatarPicker_fragment.
     */
    public static AvatarPicker_fragment newInstance(OnAvatarSelectedListener listener) {
        AvatarPicker_fragment fragment = new AvatarPicker_fragment();
        fragment.listener = listener;
        // No need for Bundle args since we pass the listener directly via field
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_avatar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarRecyclerView = view.findViewById(R.id.avatar_grid_recyclerview);
        btnSelectAvatar = view.findViewById(R.id.btn_select_avatar);

        // --- Setup RecyclerView ---
        avatarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns

        List<Integer> avatarList = new ArrayList<>();
        for (int resId : avatarResources) {
            avatarList.add(resId);
        }

        // Initialize Adapter with click handler
        avatarAdapter = new AvatarAdapter(avatarList, new AvatarAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int avatarResId) {
                selectedAvatarResId = avatarResId; // Update the selected ID
            }
        });

        avatarRecyclerView.setAdapter(avatarAdapter);

        // --- Setup Select Button Listener ---
        btnSelectAvatar.setOnClickListener(v -> {
            if (selectedAvatarResId != -1 && listener != null) {
                // Return the result via the listener interface
                listener.onAvatarSelected(selectedAvatarResId);

                // Navigate back to the previous fragment (Edit Profile)
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Please select an avatar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Since the listener is passed via field, no need to use the default onCreate/onSaveInstanceState logic here.
}