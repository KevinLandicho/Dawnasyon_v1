package com.example.dawnasyon_v1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<Integer> avatarResIds;
    private final OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // Tracks the currently selected index

    // Interface to communicate item clicks back to the Fragment
    public interface OnItemClickListener {
        void onItemClick(int avatarResId);
    }

    public AvatarAdapter(List<Integer> avatarResIds, OnItemClickListener listener) {
        this.avatarResIds = avatarResIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the single avatar item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        int avatarResId = avatarResIds.get(position);
        holder.avatarImage.setImageResource(avatarResId);

        // --- Selection and Visual Feedback Logic ---

        // Check if the current position is the selected one
        if (position == selectedPosition) {
            // Show checkmark and highlight border
            holder.selectedCheck.setVisibility(View.VISIBLE);

        } else {
            // Hide checkmark and show default border
            holder.selectedCheck.setVisibility(View.GONE);

        }

        // Set click listener on the entire item view
        holder.itemView.setOnClickListener(v -> {
            int previousSelectedPosition = selectedPosition;

            // 1. Update the selected position
            selectedPosition = holder.getAdapterPosition();

            // 2. Notify the adapter to visually unselect the previous item
            if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelectedPosition);
            }

            // 3. Notify the adapter to visually select the current item
            notifyItemChanged(selectedPosition);

            // 4. Communicate the selected resource ID back to the Fragment
            listener.onItemClick(avatarResId);
        });
    }

    @Override
    public int getItemCount() {
        return avatarResIds.size();
    }

    /**
     * ViewHolder holds references to the views in item_avatar.xml.
     */
    public static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        ImageView selectedCheck;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            selectedCheck = itemView.findViewById(R.id.avatar_selected_check);
        }
    }
}