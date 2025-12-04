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
import androidx.fragment.app.Fragment;

public class SuggestionForm_fragment extends Fragment {

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
                } else {
                    // Here you would typically send this data to your database

                    // Navigate to the Thank You Fragment
                    if (getActivity() != null) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new SuggestionTY_fragment())
                                // ⭐ CRITICAL: NOT adding to back stack here makes the
                                // next "Back" click skip this form and go to Profile.
                                .commit();
                    }
                }
            });
    }
}