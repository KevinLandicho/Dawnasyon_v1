package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ApplicationTracker_fragment extends BaseFragment {

    private RecyclerView rvApplications;
    private TrackerAdapter adapter;
    private TextView tvEmpty;

    public ApplicationTracker_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_application_tracker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        rvApplications = view.findViewById(R.id.rv_applications);
        tvEmpty = view.findViewById(R.id.tv_empty);

        // Back Button Logic
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        setupRecyclerView();
        loadApplications();

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC VIEWS (e.g., Empty Text)
        applyTagalogTranslation(view);
    }

    private void setupRecyclerView() {
        rvApplications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TrackerAdapter(new ArrayList<>(), this::showProcessDialog);
        rvApplications.setAdapter(adapter);
    }

    private void loadApplications() {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }

        SupabaseJavaHelper.fetchUserApplications(getContext(), new SupabaseJavaHelper.ApplicationHistoryCallback() {
            @Override
            public void onLoaded(List<ApplicationHistoryDTO> data) {
                if (!isAdded()) return;

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (data == null || data.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvApplications.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvApplications.setVisibility(View.VISIBLE);
                    adapter.updateList(data);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (adapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error: " + message);
                } else {
                    Toast.makeText(getContext(), "Sync Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProcessDialog(ApplicationHistoryDTO app) {
        String title = (app.getRelief_drives() != null) ? app.getRelief_drives().getName() : "Relief Operation";

        TrackerDetailsDialog_Fragment dialog = TrackerDetailsDialog_Fragment.newInstance(
                title,
                app.getStatus(),
                app.getCreated_at()
        );
        dialog.show(getParentFragmentManager(), "TrackerDetails");
    }

    // ====================================================
    // ⭐ INNER ADAPTER CLASS
    // ====================================================
    private static class TrackerAdapter extends RecyclerView.Adapter<TrackerAdapter.ViewHolder> {
        private List<ApplicationHistoryDTO> list;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(ApplicationHistoryDTO item);
        }

        public TrackerAdapter(List<ApplicationHistoryDTO> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        public void updateList(List<ApplicationHistoryDTO> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application_tracker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApplicationHistoryDTO item = list.get(position);
            Context context = holder.itemView.getContext();

            String title = (item.getRelief_drives() != null) ? item.getRelief_drives().getName() : "Relief Operation";
            holder.tvTitle.setText(title);

            // 2. Date
            String date = item.getCreated_at();
            if (date != null && date.length() > 10) {
                date = date.substring(0, 10);
            }
            String dateText = "Applied: " + date;
            holder.tvDate.setText(dateText);

            // 3. Status & Colors
            String status = item.getStatus().toUpperCase();
            holder.tvStatus.setText(status);

            // ⭐ TRANSLATE LIST ITEMS AUTOMATICALLY
            TranslationHelper.autoTranslate(context, holder.tvTitle, title);
            TranslationHelper.autoTranslate(context, holder.tvStatus, status);
            TranslationHelper.autoTranslate(context, holder.tvDate, dateText);

            if (status.equals("PENDING")) {
                holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            } else if (status.equals("APPROVED") || status.equals("CLAIMED")) {
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvStatus;
            CardView cardStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_drive_title);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
                cardStatus = itemView.findViewById(R.id.card_status);
            }
        }
    }
}