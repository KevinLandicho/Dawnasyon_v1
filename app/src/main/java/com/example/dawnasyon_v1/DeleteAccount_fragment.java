package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DeleteAccount_fragment extends BaseFragment {

    public DeleteAccount_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_delete_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnDelete = view.findViewById(R.id.btn_delete_confirm);

        // 1. Cancel -> Go back to Profile
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // 2. Delete -> Navigate to the Confirmation Timer Screen
        btnDelete.setOnClickListener(v -> {
            // We navigate to the DeleteConfirmation_fragment where the 10s timer is.
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DeleteConfirmation_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }
}