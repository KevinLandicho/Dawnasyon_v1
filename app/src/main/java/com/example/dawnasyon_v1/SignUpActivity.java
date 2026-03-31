package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;

public class SignUpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // ⭐ THE FIX: Load SignUpResident_fragment immediately
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container_signup, new SignUpResident_fragment());
            transaction.commit();
        }

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        TranslationHelper.translateViewHierarchy(this, findViewById(android.R.id.content));
    }
}