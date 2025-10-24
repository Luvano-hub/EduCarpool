package com.educarpool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class VerificationPendingActivity extends AppCompatActivity {

    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_pending);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(VerificationPendingActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}