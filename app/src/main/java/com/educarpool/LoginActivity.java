package com.educarpool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    private AuthRepository authRepository;

    // Hardcoded admin credentials
    private static final String ADMIN_EMAIL = "Admin@vossie.net";
    private static final String ADMIN_PASSWORD = "Admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }

        if (!email.endsWith("@vossie.net")) {
            etEmail.setError("Please use your @vossie.net email");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Check for admin credentials first
        if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
            // Redirect to admin dashboard
            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Show loading
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        authRepository.loginUser(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // Redirect based on user role and verification status
                    Intent intent;
                    if (user.getRole().equals("passenger")) {
                        intent = new Intent(LoginActivity.this, PassengerDashboardActivity.class);
                        intent.putExtra("user_email", user.getEmail()); // Add user email
                    } else {
                        if (user.getVerified()) {
                            intent = new Intent(LoginActivity.this, DriverDashboardActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, VerificationPendingActivity.class);
                        }
                    }
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}