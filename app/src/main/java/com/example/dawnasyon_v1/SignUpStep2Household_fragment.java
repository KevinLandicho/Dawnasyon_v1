package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpStep2Household_fragment extends Fragment {

    private LinearLayout membersContainer;
    private EditText etHouseNum;

    public SignUpStep2Household_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step2_household, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHouseNum = view.findViewById(R.id.et_house_num);
        membersContainer = view.findViewById(R.id.ll_members_container);
        Button btnNext = view.findViewById(R.id.btn_next);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // --- DYNAMIC POPULATION LOGIC ---
        etHouseNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMemberRows(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Navigation
        btnNext.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStep3Location_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    /**
     * dynamically adds or removes rows based on the input number.
     */
    private void updateMemberRows(String input) {
        int count = 0;
        try {
            if (!input.isEmpty()) {
                count = Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            count = 0;
        }

        // Limit the number to prevent app crashing/lag (optional safety)
        if (count > 20) count = 20;

        int currentChildCount = membersContainer.getChildCount();

        // If we need MORE rows
        if (count > currentChildCount) {
            for (int i = currentChildCount; i < count; i++) {
                addMemberRow(i + 1);
            }
        }
        // If we need FEWER rows
        else if (count < currentChildCount) {
            // Remove from the bottom up to preserve top data
            for (int i = currentChildCount - 1; i >= count; i--) {
                membersContainer.removeViewAt(i);
            }
        }
    }

    /**
     * Inflates a single row layout and adds it to the container.
     */
    private void addMemberRow(int index) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_household_member, membersContainer, false);

        // 1. Set Row Number
        TextView tvNumber = row.findViewById(R.id.tv_row_number);
        tvNumber.setText(index + ".");

        // 2. Setup Spinner
        Spinner spGender = row.findViewById(R.id.sp_gender);
        String[] genders = {"Gender", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spGender.setAdapter(adapter);

        // 3. Setup Age Buttons
        EditText etAge = row.findViewById(R.id.et_age);
        View btnUp = row.findViewById(R.id.btn_age_up);
        View btnDown = row.findViewById(R.id.btn_age_down);

        btnUp.setOnClickListener(v -> {
            int age = 0;
            try {
                String text = etAge.getText().toString();
                if (!text.isEmpty()) age = Integer.parseInt(text);
            } catch (NumberFormatException e) { age = 0; }
            etAge.setText(String.valueOf(age + 1));
        });

        btnDown.setOnClickListener(v -> {
            int age = 0;
            try {
                String text = etAge.getText().toString();
                if (!text.isEmpty()) age = Integer.parseInt(text);
            } catch (NumberFormatException e) { age = 0; }
            if (age > 0) etAge.setText(String.valueOf(age - 1));
        });

        membersContainer.addView(row);
    }
}