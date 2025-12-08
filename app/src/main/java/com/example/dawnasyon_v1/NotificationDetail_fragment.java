package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NotificationDetail_fragment extends BaseFragment {

    // Constants for Arguments
    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";
    private static final String ARG_TIME = "time";
    private static final String ARG_TYPE = "type";

    public NotificationDetail_fragment() {}

    // Factory method to create instance
    public static NotificationDetail_fragment newInstance(NotificationItem item) {
        NotificationDetail_fragment fragment = new NotificationDetail_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_BODY, item.getDescription());
        args.putString(ARG_TIME, item.getTime());
        args.putInt(ARG_TYPE, item.getType());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        TextView tvSubject = view.findViewById(R.id.tv_subject);
        TextView tvBody = view.findViewById(R.id.tv_body);
        TextView tvTime = view.findViewById(R.id.tv_time);
        ImageView imgAttachment = view.findViewById(R.id.img_attachment);

        // Load data from arguments
        if (getArguments() != null) {
            tvSubject.setText(getArguments().getString(ARG_TITLE));
            tvBody.setText(getArguments().getString(ARG_BODY));
            tvTime.setText(getArguments().getString(ARG_TIME));

            int type = getArguments().getInt(ARG_TYPE);
            // Show attachment only if type is 1 (Decline/Alert)
            if (type == 1) {
                imgAttachment.setVisibility(View.VISIBLE);
            } else {
                imgAttachment.setVisibility(View.GONE);
            }
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}