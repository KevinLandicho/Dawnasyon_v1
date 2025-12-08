package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SuggestionTY_fragment extends BaseFragment {

    public SuggestionTY_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggestion_ty, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);

        // logic: Go back to the previous screen (which is the Form, or Profile)
        // You might want to pop back TWICE to go straight to Profile, or just once to the Form.
        // Here we just pop once.
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}