package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    // ⭐ Master list and Active Filtered List
    private List<ApplicationHistoryDTO> fullAppList = new ArrayList<>();
    private List<ApplicationHistoryDTO> filteredList = new ArrayList<>();

    // Filter Buttons
    private Button btnAll, btnPending, btnApproved, btnDeclined, btnClaimed;

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

        // Bind Buttons
        btnAll = view.findViewById(R.id.btn_filter_all);
        btnPending = view.findViewById(R.id.btn_filter_pending);
        btnApproved = view.findViewById(R.id.btn_filter_approved);
        btnDeclined = view.findViewById(R.id.btn_filter_declined);
        btnClaimed = view.findViewById(R.id.btn_filter_claimed);

        // Back Button Logic
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Filter Click Listeners
        btnAll.setOnClickListener(v -> applyFilter("All", btnAll));
        btnPending.setOnClickListener(v -> applyFilter("Pending", btnPending));
        btnApproved.setOnClickListener(v -> applyFilter("Approved", btnApproved));
        btnDeclined.setOnClickListener(v -> applyFilter("Declined", btnDeclined));
        btnClaimed.setOnClickListener(v -> applyFilter("Claimed", btnClaimed));

        setupRecyclerView();
        loadApplications();

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC VIEWS
        applyTagalogTranslation(view);
    }

    private void setupRecyclerView() {
        rvApplications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TrackerAdapter(filteredList, this::showProcessDialog);
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

                    // Save to master list
                    fullAppList.clear();
                    fullAppList.addAll(data);

                    // Default to showing "All"
                    applyFilter("All", btnAll);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (fullAppList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error: " + message);
                    rvApplications.setVisibility(View.GONE);
                } else {
                    Toast.makeText(getContext(), "Sync Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilter(String targetStatus, Button activeBtn) {
        resetButtonStyles();

        activeBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#27869B")));
        activeBtn.setTextColor(Color.WHITE);

        filteredList.clear();
        for (ApplicationHistoryDTO app : fullAppList) {
            if (targetStatus.equals("All")) {
                filteredList.add(app);
            } else {
                String itemStatus = app.getStatus() != null ? app.getStatus().trim().toLowerCase() : "pending";

                if (targetStatus.equals("Approved")) {
                    if (itemStatus.contains("approve") || itemStatus.contains("ready") || itemStatus.contains("accept")) {
                        filteredList.add(app);
                    }
                }
                else if (targetStatus.equals("Declined")) {
                    if (itemStatus.contains("decline") || itemStatus.contains("reject") || itemStatus.contains("cancel")) {
                        filteredList.add(app);
                    }
                }
                else if (targetStatus.equals("Pending")) {
                    if (itemStatus.contains("pending")) {
                        filteredList.add(app);
                    }
                }
                else if (targetStatus.equals("Claimed")) {
                    if (itemStatus.contains("claim")) {
                        filteredList.add(app);
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            tvEmpty.setText("No " + targetStatus + " applications found.");
            tvEmpty.setVisibility(View.VISIBLE);
            rvApplications.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvApplications.setVisibility(View.VISIBLE);
        }

        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }

    private void resetButtonStyles() {
        Button[] buttons = {btnAll, btnPending, btnApproved, btnDeclined, btnClaimed};
        int inactiveColor = Color.parseColor("#E0E0E0");
        int inactiveTextColor = Color.BLACK;

        for (Button b : buttons) {
            if (b != null) {
                b.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
                b.setTextColor(inactiveTextColor);
            }
        }
    }

    private void showProcessDialog(ApplicationHistoryDTO app) {
        String title = (app.getRelief_drives() != null) ? app.getRelief_drives().getName() : "Relief Operation";

        // ⭐ THE FIX: Extract the Relief Item List from the Drive
        String itemList = "Items not specified.";
        if (app.getRelief_drives() != null && app.getRelief_drives().getRelief_item_list() != null) {
            itemList = app.getRelief_drives().getRelief_item_list();
        }

        TrackerDetailsDialog_Fragment dialog = TrackerDetailsDialog_Fragment.newInstance(
                title,
                app.getStatus(),
                app.getCreated_at(),
                app.getProof_photo(),
                itemList // ⭐ Passed to dialog
        );
        dialog.show(getParentFragmentManager(), "TrackerDetails");
    }

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

            String date = item.getCreated_at();
            if (date != null && date.length() > 10) {
                date = date.substring(0, 10);
            }
            String dateText = "Applied: " + date;
            holder.tvDate.setText(dateText);

            String status = item.getStatus().toUpperCase();
            holder.tvStatus.setText(status);

            if (status.equals("PENDING")) {
                holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            } else if (status.equals("APPROVED") || status.equals("READY") || status.equals("CLAIMED")) {
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
            return list != null ? list.size() : 0;
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