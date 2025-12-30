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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SignUpStep2Household_fragment extends BaseFragment {

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
            if (saveMembersToCache()) { // Save data before moving
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new SignUpStep3Location_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    /**
     * Loops through the dynamic views and saves them to RegistrationCache
     */
    private boolean saveMembersToCache() {
        RegistrationCache.tempHouseholdList.clear(); // Start fresh
        int childCount = membersContainer.getChildCount();

        if (childCount == 0) {
            Toast.makeText(getContext(), "Please add at least one household member.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < childCount; i++) {
            View row = membersContainer.getChildAt(i);

            // Find inputs inside the row
            EditText etName = row.findViewById(R.id.et_name);
            EditText etAge = row.findViewById(R.id.et_age);
            Spinner spGender = row.findViewById(R.id.sp_gender);
            Spinner spRelation = row.findViewById(R.id.sp_relation);

            // Safety check: if views are missing (e.g. wrong ID), skip or handle error
            if (etName == null || etAge == null) continue;

            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String gender = spGender.getSelectedItem().toString();
            String relation = spRelation.getSelectedItem().toString();

            if (name.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in Name and Age for member #" + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            int age = Integer.parseInt(ageStr);

            // ⭐ CRITICAL FIX: Set is_registered_census to TRUE
            HouseholdMember member = new HouseholdMember(
                    null, // head_id (set later)
                    name,
                    relation,
                    age,
                    gender,
                    true,  // <--- CHANGED THIS FROM false TO true
                    false  // is_authorized_proxy (default false)
            );

            RegistrationCache.tempHouseholdList.add(member);
        }
        return true;
    }

    private void updateMemberRows(String input) {
        int count = 0;
        try {
            if (!input.isEmpty()) {
                count = Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            count = 0;
        }

        if (count > 15) count = 15;

        int currentChildCount = membersContainer.getChildCount();

        if (count > currentChildCount) {
            for (int i = currentChildCount; i < count; i++) {
                addMemberRow(i + 1);
            }
        } else if (count < currentChildCount) {
            for (int i = currentChildCount - 1; i >= count; i--) {
                membersContainer.removeViewAt(i);
            }
        }
    }

    private void addMemberRow(int index) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_household_member, membersContainer, false);

        TextView tvNumber = row.findViewById(R.id.tv_row_number);
        if (tvNumber != null) tvNumber.setText(index + ".");

        Spinner spGender = row.findViewById(R.id.sp_gender);
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spGender.setAdapter(genderAdapter);

        Spinner spRelation = row.findViewById(R.id.sp_relation);
        String[] relations = {"Head", "Spouse", "Son", "Daughter", "Parent", "Relative"};
        ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, relations);
        spRelation.setAdapter(relationAdapter);

        // Auto-select "Head" for the first row
        if (index == 1) {
            spRelation.setSelection(0);
            spRelation.setEnabled(false);
        } else {
            spRelation.setSelection(2);
        }

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