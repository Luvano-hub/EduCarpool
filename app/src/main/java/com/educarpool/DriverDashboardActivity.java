package com.educarpool;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class DriverDashboardActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private TextView tvNoAddress, tvAddress;
    private CardView cardAddress;
    private ImageButton btnEdit, btnRefresh, btnMenu;
    private UserRepository userRepository;
    private GeocodingService geocodingService;
    private String userEmail;
    private boolean isMapReady = false;
    private boolean isUserDataLoaded = false;
    private User currentUser;

    // Navigation Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvUserName, tvUserEmail, tvUserRating;

    // New variables for request functionality
    private TextView tvRequestCount;
    private MaterialButton btnRefreshRequests;
    private LinearLayout bottomSheet, layoutEmpty;
    private RecyclerView recyclerRequests;
    private com.google.android.material.bottomsheet.BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private RequestAdapter requestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        userRepository = new UserRepository();
        geocodingService = new GeocodingService();

        // Get user email from intent (passed from LoginActivity)
        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null) {
            Log.e("DriverDashboard", "No user email provided in intent");
            Toast.makeText(this, "User not found - please login again", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d("DriverDashboard", "Starting dashboard for user: " + userEmail);

        initializeViews();
        setupNavigationDrawer();
        setupClickListeners();
        setupMap();
        loadUserData();

        // After all other setup
        setupDriverDashboard();
    }

    private void initializeViews() {
        tvNoAddress = findViewById(R.id.tv_no_address);
        tvAddress = findViewById(R.id.tv_address);
        cardAddress = findViewById(R.id.card_address);
        btnEdit = findViewById(R.id.btn_edit);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnMenu = findViewById(R.id.btn_menu);

        // Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup navigation header views
        View headerView = navigationView.getHeaderView(0);
        tvUserName = headerView.findViewById(R.id.tv_user_name);
        tvUserEmail = headerView.findViewById(R.id.tv_user_email);
        tvUserRating = headerView.findViewById(R.id.tv_user_rating);

        ImageButton btnCloseDrawer = headerView.findViewById(R.id.btn_close_drawer);
        com.google.android.material.button.MaterialButton btnUpdateProfile = headerView.findViewById(R.id.btn_update_profile);

        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        btnUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());

        // Bottom sheet views
        tvRequestCount = findViewById(R.id.tv_request_count);
        btnRefreshRequests = findViewById(R.id.btn_refresh_requests);
        bottomSheet = findViewById(R.id.bottom_sheet);
        recyclerRequests = findViewById(R.id.recycler_requests);
        layoutEmpty = findViewById(R.id.layout_empty);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        // Set up menu button to open drawer
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
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
            Log.e("DriverDashboard", "Map fragment not found");
        }
    }

    private void loadUserData() {
        Log.d("DriverDashboard", "Loading user data from database for: " + userEmail);

        userRepository.getUserByEmail(userEmail, new UserRepository.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                Log.d("DriverDashboard", "User data loaded successfully: " + user.getEmail());
                currentUser = user;
                isUserDataLoaded = true;

                runOnUiThread(() -> {
                    // Update navigation drawer header
                    updateNavigationHeader(user);

                    if (user.getHomeAddress() == null || user.getHomeAddress().isEmpty()) {
                        Log.d("DriverDashboard", "No home address found in database");
                        showNoAddressMessage();
                    } else {
                        // Update address card
                        updateAddressCard(user.getHomeAddress());

                        if (user.hasCoordinates()) {
                            // Coordinates exist in database, center map
                            Log.d("DriverDashboard", "Coordinates found in database, centering map");
                            centerMapOnLocation(user.getLatitude(), user.getLongitude(), user.getHomeAddress());
                        } else {
                            // Coordinates missing, geocode address and save to database
                            Log.d("DriverDashboard", "No coordinates in database, geocoding address: " + user.getHomeAddress());
                            geocodeAddressAndUpdateUser(user.getHomeAddress(), user.getEmail(), false);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Error loading user data: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this, "Failed to load user data: " + error, Toast.LENGTH_LONG).show();
                    showNoAddressMessage();
                });
            }
        });
    }

    private void updateNavigationHeader(User user) {
        tvUserName.setText(user.getName());
        tvUserEmail.setText(user.getEmail());
        if (user.getRating() != null) {
            tvUserRating.setText(String.format("%.1f", user.getRating()));
        }
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

    private void showUpdateProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Information");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_profile, null);

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etStudentId = dialogView.findViewById(R.id.et_student_id);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        EditText etHomeAddress = dialogView.findViewById(R.id.et_home_address);
        EditText etPassword = dialogView.findViewById(R.id.et_password);

        // Pre-fill with current user data
        if (currentUser != null) {
            etName.setText(currentUser.getName());
            etStudentId.setText(currentUser.getStudentId());
            etPhone.setText(currentUser.getPhone());
            etHomeAddress.setText(currentUser.getHomeAddress());
            // Don't pre-fill password for security
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Update Profile", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etHomeAddress.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateProfileInputs(name, studentId, phone, address)) {
                updateUserProfile(name, studentId, phone, address, password);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean validateProfileInputs(String name, String studentId, String phone, String address) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Student ID is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Home address is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateUserProfile(String name, String studentId, String phone, String address, String password) {
        // Show loading
        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        userRepository.updateUserProfile(userEmail, name, studentId, phone, address, password, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("DriverDashboard", "Profile updated successfully");
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Update local user object
                    if (currentUser != null) {
                        currentUser.setName(name);
                        currentUser.setStudentId(studentId);
                        currentUser.setPhone(phone);
                        currentUser.setHomeAddress(address);
                        updateNavigationHeader(currentUser);
                        updateAddressCard(address);
                    }

                    // Close drawer
                    drawerLayout.closeDrawer(GravityCompat.START);

                    // Re-geocode if address changed
                    if (currentUser != null && !address.equals(currentUser.getHomeAddress())) {
                        geocodeAddressAndUpdateUser(address, userEmail, true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Failed to update profile: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this, "Failed to update profile: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateUserAddress(String newAddress) {
        // Show loading state
        btnEdit.setEnabled(false);
        Toast.makeText(this, "Updating address...", Toast.LENGTH_SHORT).show();

        // Update address in database
        userRepository.updateUserAddress(userEmail, newAddress, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("DriverDashboard", "Address updated successfully");

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
                Log.e("DriverDashboard", "Failed to update address: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this, "Failed to update address: " + error, Toast.LENGTH_LONG).show();
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
        Log.d("DriverDashboard", "Geocoding address: " + address);

        geocodingService.geocodeAddress(address, new GeocodingService.GeocodingCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                Log.d("DriverDashboard", "Geocoding successful, saving coordinates to database...");

                // Update user record with coordinates in database
                userRepository.updateUserCoordinates(email, latitude, longitude, new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("DriverDashboard", "Coordinates saved to database successfully");
                        runOnUiThread(() -> {
                            // Re-enable refresh button
                            btnRefresh.setEnabled(true);

                            // Center the map with the new coordinates
                            centerMapOnLocation(latitude, longitude, address);

                            if (isRefresh) {
                                Toast.makeText(DriverDashboardActivity.this, "Location refreshed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("DriverDashboard", "Failed to save coordinates to database: " + error);
                        runOnUiThread(() -> {
                            btnRefresh.setEnabled(true);
                            Toast.makeText(DriverDashboardActivity.this, "Failed to save location", Toast.LENGTH_LONG).show();
                            // Still center map even if database update fails
                            centerMapOnLocation(latitude, longitude, address);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Geocoding failed: " + error);
                runOnUiThread(() -> {
                    btnRefresh.setEnabled(true);
                    Toast.makeText(DriverDashboardActivity.this, "Failed to find location: " + error, Toast.LENGTH_LONG).show();
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

            Log.d("DriverDashboard", "Map centered on coordinates: " + latitude + ", " + longitude);
        } else {
            Log.e("DriverDashboard", "Map is not ready yet, cannot center");
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

        Log.d("DriverDashboard", "Google Maps is ready");

        // If user data is already loaded, center the map
        if (isUserDataLoaded && currentUser != null && currentUser.hasCoordinates()) {
            Log.d("DriverDashboard", "Centering map on pre-existing coordinates");
            centerMapOnLocation(currentUser.getLatitude(), currentUser.getLongitude(), currentUser.getHomeAddress());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_carpool_matches) {
            // Navigate to CarpoolMatchesActivity
            startActivity(new Intent(this, com.educarpool.CarpoolMatchesActivity.class));
        } else if (itemId == R.id.nav_safety) {
            Toast.makeText(this, "Safety Guidelines - Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_support) {
            Toast.makeText(this, "Support - Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(DriverDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // New methods for request functionality
    private void setupDriverDashboard() {
        setupBottomSheet();
        setupRequestList();
        setupRequestClickListeners();

        // Load requests when dashboard is ready
        if (currentUser != null) {
            loadPendingRequests();
        }
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.5f);

        bottomSheet.setVisibility(View.VISIBLE);
        bottomSheet.bringToFront();

        bottomSheetBehavior.addBottomSheetCallback(new com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED:
                        Log.d("DriverBottomSheet", "Collapsed");
                        break;
                    case com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED:
                        Log.d("DriverBottomSheet", "Half Expanded");
                        break;
                    case com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED:
                        bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void setupRequestList() {
        requestAdapter = new RequestAdapter(new ArrayList<>());
        requestAdapter.setOnRequestActionListener(new RequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(RideRequest request) {
                acceptRideRequest(request);
            }

            @Override
            public void onDecline(RideRequest request) {
                declineRideRequest(request);
            }

            @Override
            public void onShowOnMap(RideRequest request) {
                showPassengerOnMap(request);
            }
        });

        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerRequests.setAdapter(requestAdapter);
        showEmptyRequestState();
    }

    private void setupRequestClickListeners() {
        btnRefreshRequests.setOnClickListener(v -> refreshRequests());
    }

    private void refreshRequests() {
        if (currentUser == null) return;

        btnRefreshRequests.setEnabled(false);
        btnRefreshRequests.setText("Refreshing...");
        Toast.makeText(this, "Refreshing requests...", Toast.LENGTH_SHORT).show();

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        if (currentUser == null) return;

        userRepository.getPendingRequests(currentUser.getEmail(), new UserRepository.RequestsCallback() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                Log.d("DriverDashboard", "Loaded " + requests.size() + " pending requests");
                runOnUiThread(() -> {
                    btnRefreshRequests.setEnabled(true);
                    btnRefreshRequests.setText("Refresh Requests");
                    showRequestList(requests);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Failed to load requests: " + error);
                runOnUiThread(() -> {
                    btnRefreshRequests.setEnabled(true);
                    btnRefreshRequests.setText("Refresh Requests");
                    Toast.makeText(DriverDashboardActivity.this, "Failed to load requests: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showEmptyRequestState() {
        runOnUiThread(() -> {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerRequests.setVisibility(View.GONE);
            tvRequestCount.setText("0 requests");
        });
    }

    private void showRequestList(List<RideRequest> requests) {
        runOnUiThread(() -> {
            if (requests.isEmpty()) {
                showEmptyRequestState();
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerRequests.setVisibility(View.VISIBLE);
                requestAdapter.updateRequests(requests);
                tvRequestCount.setText(requests.size() + " request" + (requests.size() == 1 ? "" : "s"));
            }
        });
    }

    private void acceptRideRequest(RideRequest request) {
        Toast.makeText(this, "Accepting request from " + request.getPassengerName(), Toast.LENGTH_SHORT).show();

        userRepository.updateRequestStatus(request.getId(), "accepted", new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("DriverDashboard", "Request accepted successfully");
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this,
                            "Request accepted! You can now chat with " + request.getPassengerName(),
                            Toast.LENGTH_LONG).show();

                    // Remove the request from the list
                    loadPendingRequests();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Failed to accept request: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this,
                            "Failed to accept request: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void declineRideRequest(RideRequest request) {
        Toast.makeText(this, "Declining request from " + request.getPassengerName(), Toast.LENGTH_SHORT).show();

        userRepository.updateRequestStatus(request.getId(), "declined", new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("DriverDashboard", "Request declined successfully");
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this,
                            "Request declined",
                            Toast.LENGTH_SHORT).show();

                    // Remove the request from the list
                    loadPendingRequests();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("DriverDashboard", "Failed to decline request: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(DriverDashboardActivity.this,
                            "Failed to decline request: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showPassengerOnMap(RideRequest request) {
        if (request.getPassengerLat() != null && request.getPassengerLng() != null) {
            LatLng passengerLocation = new LatLng(request.getPassengerLat(), request.getPassengerLng());

            if (mMap != null) {
                mMap.clear();

                // Add marker for passenger
                mMap.addMarker(new MarkerOptions()
                        .position(passengerLocation)
                        .title(request.getPassengerName())
                        .snippet(request.getPassengerHomeAddress()));

                // Add marker for driver (current location)
                if (currentUser != null && currentUser.hasCoordinates()) {
                    LatLng driverLocation = new LatLng(currentUser.getLatitude(), currentUser.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(driverLocation)
                            .title("Your Location")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE)));
                }

                // Zoom to show both markers
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(passengerLocation);
                if (currentUser != null && currentUser.hasCoordinates()) {
                    builder.include(new LatLng(currentUser.getLatitude(), currentUser.getLongitude()));
                }
                LatLngBounds bounds = builder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

                Toast.makeText(this, "Showing " + request.getPassengerName() + "'s location", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Passenger location not available", Toast.LENGTH_SHORT).show();
        }
    }
}