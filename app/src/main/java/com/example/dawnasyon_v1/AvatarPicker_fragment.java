package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AvatarPicker_fragment extends BaseFragment {

    private RecyclerView avatarRecyclerView;
    private AvatarAdapter avatarAdapter;
    private Button btnSelectAvatar;

    // Make sure these IDs match your actual drawables
    private final int[] avatarResources = {
            R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
            R.drawable.avatar4, R.drawable.avatar5, R.drawable.avatar6,
            R.drawable.avatar7, R.drawable.avatar8, R.drawable.avatar11
    };

    private int selectedAvatarResId = -1;

    public AvatarPicker_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarRecyclerView = view.findViewById(R.id.avatar_grid_recyclerview);
        btnSelectAvatar = view.findViewById(R.id.btn_select_avatar);

        avatarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<Integer> avatarList = new ArrayList<>();
        for (int resId : avatarResources) {
            avatarList.add(resId);
        }

        // Setup Adapter
        avatarAdapter = new AvatarAdapter(avatarList, avatarResId -> {
            selectedAvatarResId = avatarResId;
        });
        avatarRecyclerView.setAdapter(avatarAdapter);

        // Setup Selection Button
        btnSelectAvatar.setOnClickListener(v -> {
            if (selectedAvatarResId != -1) {
                // ⭐ THE FIX: Send result securely using Fragment Result API
                Bundle result = new Bundle();
                result.putInt("selected_avatar_id", selectedAvatarResId);

                // This key ("requestKey_avatar") matches what EditProfile listens for
                getParentFragmentManager().setFragmentResult("requestKey_avatar", result);

                // Close picker
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Please select an avatar.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}