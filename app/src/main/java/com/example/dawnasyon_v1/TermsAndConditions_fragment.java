package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TermsAndConditions_fragment extends BaseFragment {

    private TextView tvDate, tvBody;

    // ⭐ Supabase Constants
    private static final String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";

    public TermsAndConditions_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terms_and_conditions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDate = view.findViewById(R.id.tv_date);
        tvBody = view.findViewById(R.id.tv_body);
        Button btnClose = view.findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Fetch Data from Supabase
        fetchTermsFromDatabase(view);
    }

    private void fetchTermsFromDatabase(View rootView) {
        new Thread(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/terms?select=*&limit=1";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .build();

                OkHttpClient client = new OkHttpClient();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);

                        if (array.length() > 0) {
                            JSONObject item = array.getJSONObject(0);
                            String bodyText = item.optString("body", "Terms and conditions not found.");
                            String updatedAt = item.optString("updated_at", "");

                            // Format the Date cleanly
                            String formattedDate = formatDate(updatedAt);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (isAdded()) {
                                    tvBody.setText(bodyText);
                                    tvDate.setText("Last updated: " + formattedDate);

                                    // ⭐ Dynamically translate the newly fetched text!
                                    TranslationHelper.autoTranslate(getContext(), tvBody, bodyText);
                                    TranslationHelper.autoTranslate(getContext(), tvDate, "Last updated: " + formattedDate);

                                    // Translate the static UI (Title, Close Button)
                                    applyTagalogTranslation(rootView);
                                }
                            });
                        }
                    } else {
                        showError("Failed to load terms.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Network Error.");
            }
        }).start();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "Unknown";
        try {
            // Supabase returns time in UTC
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(rawDate);

            // Format it to look pretty (e.g. September 28, 2025)
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
                tvBody.setText("Error loading terms and conditions. Please try again later.");
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}