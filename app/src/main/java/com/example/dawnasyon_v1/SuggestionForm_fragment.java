package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SuggestionForm_fragment extends BaseFragment {

    public SuggestionForm_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggestion_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        Button btnSendSuggestion = view.findViewById(R.id.btn_send_suggestion);
        EditText etSuggestion = view.findViewById(R.id.et_suggestion);

        // Back Button Logic
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Send Button Logic
        btnSendSuggestion.setOnClickListener(v -> {
            String suggestionText = etSuggestion.getText().toString().trim();

            if (suggestionText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a suggestion.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button to prevent double clicks
            btnSendSuggestion.setEnabled(false);

            // ⭐ Update text and translate it immediately
            String sendingText = "Sending...";
            btnSendSuggestion.setText(sendingText);
            TranslationHelper.autoTranslate(getContext(), btnSendSuggestion, sendingText);

            // ⭐ FIX: Updated to SupabaseJavaHelper.ApplicationCallback
            SupabaseJavaHelper.submitSuggestion(suggestionText, new SupabaseJavaHelper.ApplicationCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        // Navigate to the Thank You Fragment
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new SuggestionTY_fragment())
                                .commit();
                    }
                }

                @Override
                public void onError(@NonNull String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();

                        // Re-enable button so they can try again
                        btnSendSuggestion.setEnabled(true);

                        // Reset text and translate back
                        String resetText = "Send Suggestion";
                        btnSendSuggestion.setText(resetText);
                        TranslationHelper.autoTranslate(getContext(), btnSendSuggestion, resetText);
                    }
                }
            });
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR THIS SCREEN
        applyTagalogTranslation(view);
    }
}