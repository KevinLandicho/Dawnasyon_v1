package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Reference_fragment extends BaseFragment {

    private static final String ARG_REFERENCE_NO = "reference_number";
    private String referenceNo;

    public Reference_fragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this fragment.
     * @param referenceNumber The generated reference number to display.
     */
    public static Reference_fragment newInstance(String referenceNumber) {
        Reference_fragment fragment = new Reference_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_REFERENCE_NO, referenceNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            referenceNo = getArguments().getString(ARG_REFERENCE_NO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // This inflates the XML you provided in the request
        return inflater.inflate(R.layout.fragment_reference, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtReference = view.findViewById(R.id.txtReference);

        if (referenceNo != null) {
            // 🟢 Set the generated reference number to the TextView
            txtReference.setText(referenceNo);
        } else {
            // Fallback text if the reference number was not passed
            txtReference.setText("N/A - ERROR");
        }

        // ⭐ ENABLE AUTO-TRANSLATION (Translates "Thank You" and labels)
        applyTagalogTranslation(view);
    }
}