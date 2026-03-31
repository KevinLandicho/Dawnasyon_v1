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

import java.util.ArrayList;
import java.util.List;

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

        // ⭐ If Resident, Auto-Fetch Family from Census and Lock Inputs!
        if ("Resident".equalsIgnoreCase(RegistrationCache.userType)) {

            etHouseNum.setEnabled(false);
            etHouseNum.setFocusable(false);
            etHouseNum.setAlpha(0.6f);

            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

            SupabaseJavaHelper.fetchFamilyFromCensus(RegistrationCache.tempFullName, new SupabaseJavaHelper.CensusFamilyCallback() {
                @Override
                public void onSuccess(String familyId, String houseNo, String street, List<String> memberNames) {
                    if (!isAdded()) return;
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                    List<String> sortedNames = new ArrayList<>();
                    sortedNames.add(RegistrationCache.tempFullName);
                    for (String name : memberNames) {
                        if (!name.equalsIgnoreCase(RegistrationCache.tempFullName)) {
                            sortedNames.add(name);
                        }
                    }

                    etHouseNum.setText(String.valueOf(sortedNames.size()));
                    membersContainer.removeAllViews();
                    for (int i = 0; i < sortedNames.size(); i++) {
                        addMemberRow(i + 1, sortedNames.get(i));
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    Toast.makeText(getContext(), "Error loading family: " + message, Toast.LENGTH_LONG).show();
                }
            });

        } else {
            // Fallback for Non-Residents/Overseas
            etHouseNum.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateMemberRows(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            etHouseNum.setText("1");
        }

        btnNext.setOnClickListener(v -> {
            if (saveMembersToCache()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new SignUpStep3Location_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        applyTagalogTranslation(view);
    }

    private boolean saveMembersToCache() {
        RegistrationCache.tempHouseholdList.clear();
        int childCount = membersContainer.getChildCount();

        if (childCount == 0) {
            Toast.makeText(getContext(), "Please add at least one household member.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < childCount; i++) {
            View row = membersContainer.getChildAt(i);
            int memberIndex = i + 1;

            EditText etName = row.findViewById(R.id.et_name);
            EditText etAge = row.findViewById(R.id.et_age);
            Spinner spGender = row.findViewById(R.id.sp_gender);
            Spinner spRelation = row.findViewById(R.id.sp_relation);

            if (etName == null || etAge == null) continue;

            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String gender = spGender.getSelectedItem().toString();
            String relation = spRelation.getSelectedItem().toString();

            if (name.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in Name and Age for member #" + memberIndex, Toast.LENGTH_SHORT).show();
                return false;
            }

            int age = Integer.parseInt(ageStr);

            HouseholdMember member = new HouseholdMember(
                    0L, null, name, relation, age, gender, true, false, null
            );

            RegistrationCache.tempHouseholdList.add(member);
        }
        return true;
    }

    private void updateMemberRows(String input) {
        int count = 1;
        try {
            if (!input.trim().isEmpty()) count = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) { count = 1; }

        if (count < 1) count = 1;
        if (count > 15) count = 15;

        int currentChildCount = membersContainer.getChildCount();

        if (count > currentChildCount) {
            for (int i = currentChildCount; i < count; i++) {
                addMemberRow(i + 1, null);
            }
        } else if (count < currentChildCount) {
            for (int i = currentChildCount - 1; i >= count; i--) {
                membersContainer.removeViewAt(i);
            }
        }
    }

    private void addMemberRow(int index, String predefinedName) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_household_member, membersContainer, false);

        TextView tvNumber = row.findViewById(R.id.tv_row_number);
        if (tvNumber != null) tvNumber.setText(index + ".");

        EditText etName = row.findViewById(R.id.et_name);
        Spinner spGender = row.findViewById(R.id.sp_gender);
        Spinner spRelation = row.findViewById(R.id.sp_relation);
        EditText etAge = row.findViewById(R.id.et_age);

        // ⭐ Hide the upload button programmatically so it doesn't show up from the XML
        Button btnUploadDoc = row.findViewById(R.id.btn_upload_doc);
        if (btnUploadDoc != null) {
            btnUploadDoc.setVisibility(View.GONE);
        }

        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spGender.setAdapter(genderAdapter);

        if (predefinedName != null && !predefinedName.isEmpty()) {
            etName.setText(predefinedName);
            etName.setEnabled(false);
            etName.setFocusable(false);
            etName.setAlpha(0.8f);
        }

        if (index == 1) {
            if (predefinedName == null && !RegistrationCache.tempFullName.isEmpty()) {
                etName.setText(RegistrationCache.tempFullName);
                etName.setEnabled(false);
                etName.setFocusable(false);
            }

            String[] headRelation = {"Head"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, headRelation);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(0);
            spRelation.setEnabled(false);
            spRelation.setClickable(false);

        } else {
            String[] relations = {"Spouse", "Son", "Daughter", "Parent", "Sibling", "Relative"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, relations);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(1);
        }

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
        TranslationHelper.translateViewHierarchy(getContext(), row);
    }
}