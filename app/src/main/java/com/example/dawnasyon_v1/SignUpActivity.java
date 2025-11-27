// File: SignUpActivity.java
package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming your layout for this Activity is simple, possibly activity_sign_up.xml
        setContentView(R.layout.activity_sign_up);

        // Load the first fragment (SignUpOptions_fragment) immediately
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container_signup, new SignUpOptions_fragment());
            transaction.commit();
        }
    }
}