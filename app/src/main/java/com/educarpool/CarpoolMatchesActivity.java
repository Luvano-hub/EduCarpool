package com.educarpool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CarpoolMatchesActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private com.google.android.material.button.MaterialButton btnBackToDashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpool_matches);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnBackToDashboard.setOnClickListener(v -> finish());
    }
}