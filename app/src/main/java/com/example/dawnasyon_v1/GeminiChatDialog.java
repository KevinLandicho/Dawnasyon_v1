package com.example.dawnasyon_v1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiChatDialog extends BottomSheetDialogFragment {

    // ⭐ YOUR API KEY HERE
    private static final String GEMINI_API_KEY = "AQ.Ab8RN6IZm1FamQ1hmpom-B9yw6uhLnvhJYRGBKhNrW9dyiwAeg";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

    private String dashboardContext = "";
    private LinearLayout chatContainer;
    private EditText etMessage;
    private ScrollView scrollView;
    private final OkHttpClient client = new OkHttpClient();

    public static GeminiChatDialog newInstance(String contextData) {
        GeminiChatDialog fragment = new GeminiChatDialog();
        Bundle args = new Bundle();
        args.putString("context", contextData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(32, 32, 32, 32);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Header
        TextView header = new TextView(getContext());
        header.setText("Dawnasyon AI Assistant");
        header.setTextSize(20f);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(Color.parseColor("#27869B"));
        header.setPadding(0, 0, 0, 24);
        root.addView(header);

        // Chat History Scroll
        scrollView = new ScrollView(getContext());
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        chatContainer = new LinearLayout(getContext());
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(chatContainer);
        root.addView(scrollView);

        // Horizontal Scroll for Preset Suggestions
        HorizontalScrollView hScrollView = new HorizontalScrollView(getContext());
        hScrollView.setHorizontalScrollBarEnabled(false);
        hScrollView.setPadding(0, 16, 0, 16);

        LinearLayout suggestionsContainer = new LinearLayout(getContext());
        suggestionsContainer.setOrientation(LinearLayout.HORIZONTAL);

        // ⭐ NEW: Added Inventory Analysis Preset
        String[] presets = {
                "What items are missing in our inventory?",
                "What should I donate today?",
                "What is the current disaster status?",
                "Which areas are most affected?",
                "How do I prepare an emergency kit?"
        };

        for (String preset : presets) {
            TextView chip = new TextView(getContext());
            chip.setText(preset);
            chip.setTextColor(Color.parseColor("#27869B"));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setPadding(32, 16, 32, 16);

            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.WHITE);
            border.setStroke(2, Color.parseColor("#27869B"));
            border.setCornerRadius(50f);
            chip.setBackground(border);

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(chipParams);

            chip.setOnClickListener(v -> {
                addMessage("You", preset);
                askGemini(preset);
            });

            suggestionsContainer.addView(chip);
        }
        hScrollView.addView(suggestionsContainer);
        root.addView(hScrollView);

        // Input Field Area
        LinearLayout inputLayout = new LinearLayout(getContext());
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(0, 8, 0, 0);

        etMessage = new EditText(getContext());
        etMessage.setHint("Type your message...");
        etMessage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView btnSend = new ImageView(getContext());
        btnSend.setImageResource(android.R.drawable.ic_menu_send);
        btnSend.setColorFilter(Color.parseColor("#F5901A"));
        btnSend.setPadding(16, 16, 16, 16);

        inputLayout.addView(etMessage);
        inputLayout.addView(btnSend);
        root.addView(inputLayout);

        if (getArguments() != null) {
            dashboardContext = getArguments().getString("context", "");
        }

        addMessage("Bot", "Hello! I am monitoring the barangay's live data. Tap a suggestion below or ask me a question!");

        btnSend.setOnClickListener(v -> {
            String userMsg = etMessage.getText().toString().trim();
            if (!userMsg.isEmpty()) {
                addMessage("You", userMsg);
                etMessage.setText("");
                askGemini(userMsg);
            }
        });

        return root;
    }

    private void addMessage(String sender, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            TextView tv = new TextView(getContext());
            tv.setText(message);
            tv.setPadding(32, 24, 32, 24);
            tv.setTextSize(15f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);

            GradientDrawable bubble = new GradientDrawable();
            bubble.setCornerRadius(30f);

            if (sender.equals("You")) {
                bubble.setColor(Color.parseColor("#E3F2FD"));
                tv.setTextColor(Color.parseColor("#0D47A1"));
                params.gravity = android.view.Gravity.END;
                params.setMargins(100, 8, 0, 8);
            } else {
                bubble.setColor(Color.parseColor("#F5F5F5"));
                tv.setTextColor(Color.BLACK);
                params.gravity = android.view.Gravity.START;
                params.setMargins(0, 8, 100, 8);
            }

            tv.setBackground(bubble);
            tv.setLayoutParams(params);
            chatContainer.addView(tv);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void askGemini(String userMessage) {
        addMessage("Bot", "Thinking...");
        new Thread(() -> {
            try {
                // ⭐ NEW: Taught Gemini how to analyze the inventory data!
                String promptText = "You are the official AI Assistant for Barangay Sta. Lucia's disaster management app, Dawnasyon. " +
                        "Here is the LIVE barangay data right now: [" + dashboardContext + "]. " +
                        "INSTRUCTIONS:\n" +
                        "1. You are allowed to use your vast general knowledge to answer questions about disaster preparedness, safety guidelines, first aid, weather safety, relief goods, and community updates.\n" +
                        "2. If the user asks about the current status of the barangay, use the LIVE data provided to answer accurately.\n" +
                        "3. INVENTORY ANALYSIS: If the user asks what items are needed, missing, or what they should donate, look at the 'Full Inventory Breakdown' in the LIVE data. Compare it to standard disaster needs (Food, Water, Medicine, Hygiene Kits). Suggest the categories that have the lowest counts or are missing entirely, and encourage them to donate those.\n" +
                        "4. STRICT RESTRICTION: If the user asks a question that is COMPLETELY UNRELATED to disasters, relief distribution, barangay safety, first aid, or the Dawnasyon app, you MUST politely decline.\n" +
                        "5. Keep your answers concise, empathetic, and friendly.\n\n" +
                        "User Question: " + userMessage;

                JSONObject textPart = new JSONObject();
                textPart.put("text", promptText);

                JSONArray partsArray = new JSONArray();
                partsArray.put(textPart);

                JSONObject contentObj = new JSONObject();
                contentObj.put("parts", partsArray);

                JSONArray contentsArray = new JSONArray();
                contentsArray.put(contentObj);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contentsArray);

                RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(GEMINI_URL)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        String botReply = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        new Handler(Looper.getMainLooper()).post(() -> {
                            chatContainer.removeViewAt(chatContainer.getChildCount() - 1);
                            addMessage("Bot", botReply);
                        });
                    } else {
                        String errorResponse = response.body() != null ? response.body().string() : "No details";
                        final int errorCode = response.code();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            chatContainer.removeViewAt(chatContainer.getChildCount() - 1);
                            addMessage("Bot", "Google Error " + errorCode + ": \n" + errorResponse);
                        });
                    }
                }
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> {
                    chatContainer.removeViewAt(chatContainer.getChildCount() - 1);
                    addMessage("Bot", "App Error: " + errorMsg);
                });
            }
        }).start();
    }
}