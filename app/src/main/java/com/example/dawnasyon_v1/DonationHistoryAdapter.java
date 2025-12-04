package com.example.dawnasyon_v1;

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
    private OnReceiptClickListener listener; // ⭐ NEW: Listener Field

    // ⭐ NEW: Interface definition
    public interface OnReceiptClickListener {
        void onReceiptClick(DonationHistoryItem item);
    }

    // ⭐ UPDATED: Constructor accepts the listener
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

        holder.tvName.setText(item.getDonorName());
        holder.tvDate.setText(item.getDate());
        String fullText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit... (Placeholder for receipt details)";
        holder.tvDesc.setText(fullText);
        holder.imgAvatar.setImageResource(item.getImageResId());

        // ⭐ UPDATED: Click listener calls the interface, not the FragmentManager directly
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