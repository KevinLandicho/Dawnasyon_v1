package com.example.dawnasyon_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // ⭐ NEW IMPORT for TextView

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends BaseActivity {
    private Button btnSignin;
    private TextView btnSignup; // ⭐ Declare the SignUp TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        btnSignin = findViewById(R.id.btnSignin);
        btnSignup = findViewById(R.id.btnSignup); // ⭐ Initialize the Signup TextView

        // --- 1. Sign In Button Listener (Existing) ---
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement login verification here
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });

        // --- 2. Sign Up Button Listener (NEW) ---
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ⭐ Action: Start a new activity dedicated to the sign-up process.
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}