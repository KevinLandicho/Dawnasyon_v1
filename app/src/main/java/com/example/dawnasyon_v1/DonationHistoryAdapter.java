package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DonationHistoryAdapter extends RecyclerView.Adapter<DonationHistoryAdapter.ViewHolder> {

    private List<DonationHistoryItem> historyList;
    private OnReceiptClickListener listener;

    public interface OnReceiptClickListener {
        void onReceiptClick(DonationHistoryItem item);
    }

    public DonationHistoryAdapter(List<DonationHistoryItem> historyList, OnReceiptClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    // ⭐ ADDED: A safe way to forcefully update the adapter's data
    public void updateData(List<DonationHistoryItem> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donation_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonationHistoryItem item = historyList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Set Name
        String name = "My Donation";
        holder.tvName.setText(name);

        // 2. Set Date
        holder.tvDate.setText(item.getFormattedDate());

        // ⭐ 3. CRITICAL FIX: Set Description WITHOUT the TranslationHelper
        // Translating dynamic text inside a RecyclerView causes race conditions.
        // It forces the old item's text to overwrite the new item's text when filtering!
        String description = item.getDisplayDescription();
        holder.tvDesc.setText(description);

        // 4. Set Avatar
        holder.imgAvatar.setImageResource(item.getImageResId());

        // 5. Handle Status Color
        String status = item.getStatus();
        if (status != null && (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Verified") || status.equalsIgnoreCase("In Inventory"))) {
            holder.tvDesc.setTextColor(Color.parseColor("#388E3C")); // Green
        } else if (status != null && status.equalsIgnoreCase("Pending")) {
            holder.tvDesc.setTextColor(Color.parseColor("#F57C00")); // Orange
        } else if (status != null && (status.equalsIgnoreCase("Declined") || status.equalsIgnoreCase("Rejected"))) {
            holder.tvDesc.setTextColor(Color.parseColor("#D32F2F")); // Red
        } else {
            holder.tvDesc.setTextColor(Color.DKGRAY);
        }

        // 6. Static Translations
        // These are safe to translate because the text is the exact same for every single row.
        TranslationHelper.autoTranslate(context, holder.tvName, name);
        TranslationHelper.autoTranslate(context, holder.btnReceipt, "View Receipt");

        // 7. Click Listener
        holder.btnReceipt.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReceiptClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvDesc;
        ImageView imgAvatar;
        Button btnReceipt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_donor_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDesc = itemView.findViewById(R.id.tv_description);
            imgAvatar = itemView.findViewById(R.id.img_donor_avatar);
            btnReceipt = itemView.findViewById(R.id.btn_view_receipt);
        }
    }
}