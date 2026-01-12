package com.example.dawnasyon_v1;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donation_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonationHistoryItem item = historyList.get(position);

        // 1. Set Name (Static for "My History", or you can fetch profile name)
        holder.tvName.setText("My Donation");

        // 2. Set Date
        holder.tvDate.setText(item.getFormattedDate());

        // 3. Set Description (Calculated in Fragment)
        holder.tvDesc.setText(item.getDisplayDescription());

        // 4. Set Avatar
        holder.imgAvatar.setImageResource(item.getImageResId());

        // 5. Handle Status Color (Optional visual improvement)
        String status = item.getStatus();
        if (status != null && status.equalsIgnoreCase("Verified")) {
            holder.tvDesc.setTextColor(Color.parseColor("#388E3C")); // Green
        } else if (status != null && status.equalsIgnoreCase("Pending")) {
            holder.tvDesc.setTextColor(Color.parseColor("#F57C00")); // Orange
        } else {
            holder.tvDesc.setTextColor(Color.DKGRAY);
        }

        // 6. Click Listener
        holder.btnReceipt.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReceiptClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
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