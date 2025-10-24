package com.educarpool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PendingDriversAdapter adapter;
    private List<User> pendingDrivers;
    private AuthRepository authRepository;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        authRepository = new AuthRepository();
        pendingDrivers = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadPendingDrivers();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        Button btnLogout = findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new PendingDriversAdapter(pendingDrivers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadPendingDrivers() {
        Request request = new Request.Builder()
                .url(AuthRepository.SUPABASE_URL + "/rest/v1/users?role=eq.driver&verified=eq.false&select=*")
                .get()
                .addHeader("apikey", AuthRepository.API_KEY)
                .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        authRepository.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to load pending drivers", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONArray usersArray = new JSONArray(responseBody);
                        pendingDrivers.clear();

                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userJson = usersArray.getJSONObject(i);
                            User user = new User();
                            user.setEmail(userJson.getString("email"));
                            user.setName(userJson.getString("name"));
                            user.setStudentId(userJson.getString("student_id"));
                            user.setPhone(userJson.getString("phone"));
                            user.setHomeAddress(userJson.getString("home_address"));
                            user.setRole(userJson.getString("role"));
                            user.setVerified(userJson.getBoolean("verified"));

                            // Handle potential null values
                            if (userJson.has("detour_range") && !userJson.isNull("detour_range")) {
                                user.setDetourRange(userJson.getInt("detour_range"));
                            }
                            if (userJson.has("license_url") && !userJson.isNull("license_url")) {
                                user.setLicenseUrl(userJson.getString("license_url"));
                            }
                            if (userJson.has("car_reg_url") && !userJson.isNull("car_reg_url")) {
                                user.setCarRegUrl(userJson.getString("car_reg_url"));
                            }

                            pendingDrivers.add(user);
                        }

                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        });

                    } catch (JSONException e) {
                        Log.e("AdminDashboard", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> updateEmptyState());
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminDashboardActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    });
                }
            }
        });
    }

    private void updateEmptyState() {
        if (pendingDrivers.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDriverVerification(String email, boolean verified) {
        try {
            JSONObject json = new JSONObject();
            json.put("verified", verified);

            Log.d("AdminDashboard", "Updating driver: " + email + " to verified: " + verified);
            Log.d("AdminDashboard", "Request JSON: " + json.toString());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            // Use the user's email to update their record
            Request request = new Request.Builder()
                    .url(AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email)
                    .patch(body)
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            Log.d("AdminDashboard", "Sending PATCH request to: " + AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email);

            authRepository.client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("AdminDashboard", "Network error: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(AdminDashboardActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d("AdminDashboard", "Update response - Code: " + response.code() + ", Body: " + responseBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    verified ? "Driver approved! They can now access the driver dashboard." : "Driver rejected!",
                                    Toast.LENGTH_LONG).show();
                            // Refresh the list to remove the approved driver
                            loadPendingDrivers();
                        } else {
                            String errorMessage = "Update failed - HTTP " + response.code();
                            if (response.code() == 401) {
                                errorMessage += ": Authentication failed. Check API key.";
                            } else if (response.code() == 403) {
                                errorMessage += ": Permission denied. Check RLS policies.";
                            } else if (response.code() == 404) {
                                errorMessage += ": User not found.";
                            }
                            Toast.makeText(AdminDashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("AdminDashboard", "Error response: " + responseBody);
                        }
                    });
                }
            });
        } catch (JSONException e) {
            Log.e("AdminDashboard", "JSON error: " + e.getMessage());
            runOnUiThread(() ->
                    Toast.makeText(AdminDashboardActivity.this, "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
    }

    // Helper method to decode Base64 string to Bitmap
    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            // Remove data URL prefix if present
            String base64Image = base64String;
            if (base64String.contains(",")) {
                base64Image = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error decoding Base64 image: " + e.getMessage());
            return null;
        }
    }

    // Method to show full image in a dialog
    private void showFullImageDialog(Bitmap bitmap, String title) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title);

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxHeight(800);
        imageView.setMaxWidth(600);

        builder.setView(imageView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    // RecyclerView Adapter
    private class PendingDriversAdapter extends RecyclerView.Adapter<PendingDriversAdapter.ViewHolder> {

        private final List<User> drivers;

        public PendingDriversAdapter(List<User> drivers) {
            this.drivers = drivers;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_driver, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            User driver = drivers.get(position);
            holder.bind(driver);
        }

        @Override
        public int getItemCount() {
            return drivers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvStudentId, tvEmail, tvPhone, tvAddress, tvDetourRange;
            private final Button btnApprove, btnReject;
            private final ImageView ivLicense, ivCarReg;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvStudentId = itemView.findViewById(R.id.tv_student_id);
                tvEmail = itemView.findViewById(R.id.tv_email);
                tvPhone = itemView.findViewById(R.id.tv_phone);
                tvAddress = itemView.findViewById(R.id.tv_address);
                tvDetourRange = itemView.findViewById(R.id.tv_detour_range);
                btnApprove = itemView.findViewById(R.id.btn_approve);
                btnReject = itemView.findViewById(R.id.btn_reject);
                ivLicense = itemView.findViewById(R.id.iv_license);
                ivCarReg = itemView.findViewById(R.id.iv_car_reg);
            }

            void bind(User driver) {
                tvName.setText("Name: " + driver.getName());
                tvStudentId.setText("Student ID: " + driver.getStudentId());
                tvEmail.setText("Email: " + driver.getEmail());
                tvPhone.setText("Phone: " + driver.getPhone());
                tvAddress.setText("Address: " + driver.getHomeAddress());

                if (driver.getDetourRange() != null) {
                    tvDetourRange.setText("Detour Range: " + driver.getDetourRange() + " km");
                } else {
                    tvDetourRange.setText("Detour Range: Not specified");
                }

                // Set up image previews if available
                if (driver.getLicenseUrl() != null && !driver.getLicenseUrl().isEmpty()) {
                    Bitmap licenseBitmap = decodeBase64ToBitmap(driver.getLicenseUrl());
                    if (licenseBitmap != null) {
                        ivLicense.setImageBitmap(licenseBitmap);
                        ivLicense.setVisibility(View.VISIBLE);

                        // Add click listener to view full image
                        ivLicense.setOnClickListener(v -> showFullImageDialog(licenseBitmap, "Driver's License - " + driver.getName()));
                    } else {
                        ivLicense.setVisibility(View.GONE);
                    }
                } else {
                    ivLicense.setVisibility(View.GONE);
                }

                if (driver.getCarRegUrl() != null && !driver.getCarRegUrl().isEmpty()) {
                    Bitmap carRegBitmap = decodeBase64ToBitmap(driver.getCarRegUrl());
                    if (carRegBitmap != null) {
                        ivCarReg.setImageBitmap(carRegBitmap);
                        ivCarReg.setVisibility(View.VISIBLE);

                        // Add click listener to view full image
                        ivCarReg.setOnClickListener(v -> showFullImageDialog(carRegBitmap, "Car Registration - " + driver.getName()));
                    } else {
                        ivCarReg.setVisibility(View.GONE);
                    }
                } else {
                    ivCarReg.setVisibility(View.GONE);
                }

                btnApprove.setOnClickListener(v ->
                        updateDriverVerification(driver.getEmail(), true)
                );

                btnReject.setOnClickListener(v ->
                        updateDriverVerification(driver.getEmail(), false)
                );
            }
        }
    }
}