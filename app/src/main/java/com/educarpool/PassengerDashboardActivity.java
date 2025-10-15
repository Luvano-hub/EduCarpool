package com.educarpool;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PassengerDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvNoAddress, tvAddress;
    private CardView cardAddress;
    private ImageButton btnEdit, btnRefresh;
    private UserRepository userRepository;
    private GeocodingService geocodingService;
    private String userEmail;
    private boolean isMapReady = false;
    private boolean isUserDataLoaded = false;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_dashboard);

        userRepository = new UserRepository();
        geocodingService = new GeocodingService();

        // Get user email from intent (passed from LoginActivity)
        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("PassengerDashboard", "Starting dashboard for user: " + userEmail);

        initializeViews();
        setupClickListeners();
        setupMap();
        loadUserData();
    }

    private void initializeViews() {
        tvNoAddress = findViewById(R.id.tv_no_address);
        tvAddress = findViewById(R.id.tv_address);
        cardAddress = findViewById(R.id.card_address);
        btnEdit = findViewById(R.id.btn_edit);
        btnRefresh = findViewById(R.id.btn_refresh);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> showEditAddressDialog());
        btnRefresh.setOnClickListener(v -> refreshLocation());
    }

    private void setupMap() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager.findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("PassengerDashboard", "Map fragment not found");
        }
    }

    private void loadUserData() {
        Log.d("PassengerDashboard", "Loading user data from database...");

        userRepository.getUserByEmail(userEmail, new UserRepository.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                Log.d("PassengerDashboard", "User data loaded successfully");
                currentUser = user;
                isUserDataLoaded = true;

                runOnUiThread(() -> {
                    if (user.getHomeAddress() == null || user.getHomeAddress().isEmpty()) {
                        Log.d("PassengerDashboard", "No home address found in database");
                        showNoAddressMessage();
                    } else {
                        // Update address card
                        updateAddressCard(user.getHomeAddress());

                        if (user.hasCoordinates()) {
                            // Coordinates exist in database, center map
                            Log.d("PassengerDashboard", "Coordinates found in database, centering map");
                            centerMapOnLocation(user.getLatitude(), user.getLongitude(), user.getHomeAddress());
                        } else {
                            // Coordinates missing, geocode address and save to database
                            Log.d("PassengerDashboard", "No coordinates in database, geocoding address: " + user.getHomeAddress());
                            geocodeAddressAndUpdateUser(user.getHomeAddress(), user.getEmail(), false);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Error loading user data: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PassengerDashboardActivity.this, "Failed to load user data", Toast.LENGTH_LONG).show();
                    showNoAddressMessage();
                });
            }
        });
    }

    private void updateAddressCard(String address) {
        tvAddress.setText(address);
        cardAddress.setVisibility(View.VISIBLE);

        // Add fade-in animation
        cardAddress.setAlpha(0f);
        cardAddress.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
    }

    private void showEditAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Home Address");

        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_address, null);
        EditText etNewAddress = dialogView.findViewById(R.id.et_new_address);

        // Pre-fill with current address
        if (currentUser != null && currentUser.getHomeAddress() != null) {
            etNewAddress.setText(currentUser.getHomeAddress());
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newAddress = etNewAddress.getText().toString().trim();
            if (!newAddress.isEmpty()) {
                updateUserAddress(newAddress);
            } else {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUserAddress(String newAddress) {
        // Show loading state
        btnEdit.setEnabled(false);
        Toast.makeText(this, "Updating address...", Toast.LENGTH_SHORT).show();

        // Update address in database
        userRepository.updateUserAddress(userEmail, newAddress, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("PassengerDashboard", "Address updated successfully");

                // Update local user object
                if (currentUser != null) {
                    currentUser.setHomeAddress(newAddress);
                }

                // Update UI
                runOnUiThread(() -> {
                    updateAddressCard(newAddress);
                    btnEdit.setEnabled(true);

                    // Re-geocode the new address
                    geocodeAddressAndUpdateUser(newAddress, userEmail, true);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Failed to update address: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PassengerDashboardActivity.this, "Failed to update address: " + error, Toast.LENGTH_LONG).show();
                    btnEdit.setEnabled(true);
                });
            }
        });
    }

    private void refreshLocation() {
        if (currentUser != null && currentUser.getHomeAddress() != null && !currentUser.getHomeAddress().isEmpty()) {
            btnRefresh.setEnabled(false);
            Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show();

            // Re-geocode the current address
            geocodeAddressAndUpdateUser(currentUser.getHomeAddress(), userEmail, true);
        } else {
            Toast.makeText(this, "No address to refresh", Toast.LENGTH_SHORT).show();
        }
    }

    private void geocodeAddressAndUpdateUser(String address, String email, boolean isRefresh) {
        Log.d("PassengerDashboard", "Geocoding address: " + address);

        geocodingService.geocodeAddress(address, new GeocodingService.GeocodingCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                Log.d("PassengerDashboard", "Geocoding successful, saving coordinates to database...");

                // Update user record with coordinates in database
                userRepository.updateUserCoordinates(email, latitude, longitude, new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("PassengerDashboard", "Coordinates saved to database successfully");
                        runOnUiThread(() -> {
                            // Re-enable refresh button
                            btnRefresh.setEnabled(true);

                            // Center the map with the new coordinates
                            centerMapOnLocation(latitude, longitude, address);

                            if (isRefresh) {
                                Toast.makeText(PassengerDashboardActivity.this, "Location refreshed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("PassengerDashboard", "Failed to save coordinates to database: " + error);
                        runOnUiThread(() -> {
                            btnRefresh.setEnabled(true);
                            Toast.makeText(PassengerDashboardActivity.this, "Failed to save location", Toast.LENGTH_LONG).show();
                            // Still center map even if database update fails
                            centerMapOnLocation(latitude, longitude, address);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Geocoding failed: " + error);
                runOnUiThread(() -> {
                    btnRefresh.setEnabled(true);
                    Toast.makeText(PassengerDashboardActivity.this, "Failed to find location: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void centerMapOnLocation(double latitude, double longitude, String address) {
        if (mMap != null) {
            LatLng location = new LatLng(latitude, longitude);

            // Clear existing markers
            mMap.clear();

            // Add marker
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Home")
                    .snippet(address));

            // Animate camera to location with zoom
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

            // Hide no address message
            tvNoAddress.setVisibility(View.GONE);

            Log.d("PassengerDashboard", "Map centered on coordinates: " + latitude + ", " + longitude);
        } else {
            Log.e("PassengerDashboard", "Map is not ready yet, cannot center");
        }
    }

    private void showNoAddressMessage() {
        tvNoAddress.setVisibility(View.VISIBLE);
        cardAddress.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady = true;

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        Log.d("PassengerDashboard", "Google Maps is ready");

        // If user data is already loaded, center the map
        if (isUserDataLoaded && currentUser != null && currentUser.hasCoordinates()) {
            Log.d("PassengerDashboard", "Centering map on pre-existing coordinates");
            centerMapOnLocation(currentUser.getLatitude(), currentUser.getLongitude(), currentUser.getHomeAddress());
        }
    }
}