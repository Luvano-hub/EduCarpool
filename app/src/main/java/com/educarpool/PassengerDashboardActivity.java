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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class PassengerDashboardActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private TextView tvNoAddress, tvAddress, tvDriverCount;
    private CardView cardAddress;
    private ImageButton btnEdit, btnRefresh, btnMenu;
    private MaterialButton btnFindMatches;
    private LinearLayout bottomSheet, layoutEmpty;
    private RecyclerView recyclerDrivers;
    private com.google.android.material.bottomsheet.BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private UserRepository userRepository;
    private GeocodingService geocodingService;
    private DistanceMatrixService distanceMatrixService;
    private String userEmail;
    private boolean isMapReady = false;
    private boolean isUserDataLoaded = false;
    private User currentUser;
    private DriverAdapter driverAdapter;

    // Navigation Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvUserName, tvUserEmail, tvUserRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_dashboard);

        userRepository = new UserRepository();
        geocodingService = new GeocodingService();
        distanceMatrixService = new DistanceMatrixService();

        // Get user email from intent (passed from LoginActivity)
        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("PassengerDashboard", "Starting dashboard for user: " + userEmail);

        initializeViews();
        setupNavigationDrawer();
        setupBottomSheet();
        setupClickListeners();
        setupMap();
        setupDriverList();
        loadUserData();
    }

    private void initializeViews() {
        tvNoAddress = findViewById(R.id.tv_no_address);
        tvAddress = findViewById(R.id.tv_address);
        cardAddress = findViewById(R.id.card_address);
        btnEdit = findViewById(R.id.btn_edit);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnMenu = findViewById(R.id.btn_menu);
        btnFindMatches = findViewById(R.id.btn_find_matches);
        bottomSheet = findViewById(R.id.bottom_sheet);
        recyclerDrivers = findViewById(R.id.recycler_drivers);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvDriverCount = findViewById(R.id.tv_driver_count);

        // Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup navigation header views
        View headerView = navigationView.getHeaderView(0);
        tvUserName = headerView.findViewById(R.id.tv_user_name);
        tvUserEmail = headerView.findViewById(R.id.tv_user_email);
        tvUserRating = headerView.findViewById(R.id.tv_user_rating);

        ImageButton btnCloseDrawer = headerView.findViewById(R.id.btn_close_drawer);
        MaterialButton btnUpdateProfile = headerView.findViewById(R.id.btn_update_profile);

        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        btnUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        // Set up menu button to open drawer
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
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
                        Log.d("BottomSheet", "Collapsed - Peek height (120dp)");
                        break;
                    case com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED:
                        Log.d("BottomSheet", "Half Expanded - 50% of screen");
                        break;
                    case com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED:
                        Log.d("BottomSheet", "Fully Expanded");
                        bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: Add visual effects during slide
            }
        });

        Log.d("BottomSheet", "Bottom sheet initialized - should be visible at bottom");
    }

    private void setupDriverList() {
        driverAdapter = new DriverAdapter(new ArrayList<>());
        driverAdapter.setOnDriverClickListener(driver -> {
            sendRideRequestToDriver(driver);
        });
        recyclerDrivers.setLayoutManager(new LinearLayoutManager(this));
        recyclerDrivers.setAdapter(driverAdapter);
        showEmptyDriverState();
    }

    // sendRideRequestToDriver with proper button state management
    private void sendRideRequestToDriver(Driver driver) {
        if (currentUser == null) return;

        // Disable the button for this specific driver
        updateDriverButtonState(driver.getId(), false);

        Toast.makeText(this, "Sending ride request to " + driver.getName(), Toast.LENGTH_SHORT).show();

        // Save match to database
        userRepository.saveMatch(
                currentUser.getEmail(),
                driver.getId(),
                driver.getDistance(),
                driver.getDuration(),
                new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(PassengerDashboardActivity.this,
                                    "Ride request sent to " + driver.getName(),
                                    Toast.LENGTH_LONG).show();

                            // Re-enable button after success
                            updateDriverButtonState(driver.getId(), true);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(PassengerDashboardActivity.this,
                                    "Failed to send request: " + error,
                                    Toast.LENGTH_LONG).show();

                            // Re-enable button on error
                            updateDriverButtonState(driver.getId(), true);
                        });
                    }
                }
        );
    }

    // Helper method to update driver button state using public methods
    private void updateDriverButtonState(String driverId, boolean enabled) {
        int position = driverAdapter.findPositionByDriverId(driverId);
        if (position != -1) {
            RecyclerView.ViewHolder holder = recyclerDrivers.findViewHolderForAdapterPosition(position);
            if (holder instanceof DriverAdapter.DriverViewHolder) {
                DriverAdapter.DriverViewHolder driverHolder = (DriverAdapter.DriverViewHolder) holder;
                driverHolder.setButtonEnabled(enabled);
            } else {
                // If view holder is not available, notify the adapter to update the item
                driverAdapter.notifyItemChanged(position);
            }
        }
    }

    private void showEmptyDriverState() {
        runOnUiThread(() -> {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerDrivers.setVisibility(View.GONE);
            tvDriverCount.setText("0 drivers");
            Log.d("BottomSheet", "Showing empty driver state");
        });
    }

    private void showDriverList(List<DriverMatch> matches) {
        runOnUiThread(() -> {
            if (matches.isEmpty()) {
                showEmptyDriverState();
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerDrivers.setVisibility(View.VISIBLE);

                // Convert DriverMatch to Driver for the adapter
                List<Driver> drivers = new ArrayList<>();
                for (DriverMatch match : matches) {
                    Driver driver = new Driver();
                    driver.setId(match.getId());
                    driver.setName(match.getName());
                    driver.setRating(match.getRating());
                    driver.setDistance(match.getDistance());
                    driver.setDuration(match.getDuration());
                    driver.setVerified(match.isVerified());
                    driver.setLatitude(match.getLatitude());
                    driver.setLongitude(match.getLongitude());
                    drivers.add(driver);
                }

                driverAdapter.updateDrivers(drivers);
                tvDriverCount.setText(drivers.size() + " driver" + (drivers.size() == 1 ? "" : "s"));
                Log.d("BottomSheet", "Showing " + drivers.size() + " drivers");
            }
        });
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> showEditAddressDialog());
        btnRefresh.setOnClickListener(v -> refreshLocation());
        btnFindMatches.setOnClickListener(v -> findDriverMatches());
    }

    // findDriverMatches with button state management
    private void findDriverMatches() {
        if (currentUser == null || !currentUser.hasCoordinates()) {
            Toast.makeText(this, "Please set your home address first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFindMatches.setEnabled(false);
        btnFindMatches.setText("Finding Matches...");
        Toast.makeText(this, "Finding nearby drivers...", Toast.LENGTH_SHORT).show();

        // Fetch all verified drivers with coordinates
        userRepository.getVerifiedDriversWithCoordinates(new UserRepository.DriversFetchCallback() {
            @Override
            public void onSuccess(List<User> drivers) {
                Log.d("PassengerDashboard", "Found " + drivers.size() + " verified drivers with coordinates");

                if (drivers.isEmpty()) {
                    runOnUiThread(() -> {
                        btnFindMatches.setEnabled(true);
                        btnFindMatches.setText("Find Matches");
                        Toast.makeText(PassengerDashboardActivity.this, "No verified drivers found in your area", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Calculate distances using Distance Matrix API
                distanceMatrixService.calculateDistances(
                        currentUser.getLatitude(),
                        currentUser.getLongitude(),
                        drivers,
                        new DistanceMatrixService.DistanceMatrixCallback() {
                            @Override
                            public void onSuccess(List<DriverMatch> matches) {
                                Log.d("PassengerDashboard", "Distance calculation successful, found " + matches.size() + " matches");

                                // Sort matches by distance (closest first)
                                matches.sort((m1, m2) -> Double.compare(m1.getDistance(), m2.getDistance()));

                                // Save matches to database
                                saveMatchesToDatabase(matches);

                                // Update UI with matches
                                runOnUiThread(() -> {
                                    btnFindMatches.setEnabled(true);
                                    btnFindMatches.setText("Find Matches");
                                    showDriverList(matches);
                                    Toast.makeText(PassengerDashboardActivity.this,
                                            "Found " + matches.size() + " drivers nearby",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("PassengerDashboard", "Distance calculation failed: " + error);
                                runOnUiThread(() -> {
                                    btnFindMatches.setEnabled(true);
                                    btnFindMatches.setText("Find Matches");
                                    Toast.makeText(PassengerDashboardActivity.this,
                                            "Failed to calculate distances: " + error,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Failed to fetch drivers: " + error);
                runOnUiThread(() -> {
                    btnFindMatches.setEnabled(true);
                    btnFindMatches.setText("Find Matches");
                    Toast.makeText(PassengerDashboardActivity.this,
                            "Failed to find drivers: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // saveMatchesToDatabase - now uses the improved saveMatch method
    private void saveMatchesToDatabase(List<DriverMatch> matches) {
        int totalMatches = matches.size();
        final int[] completedMatches = {0};

        for (DriverMatch match : matches) {
            userRepository.saveMatch(
                    currentUser.getEmail(),
                    match.getId(),
                    match.getDistance(),
                    match.getDuration(),
                    new UserRepository.UserUpdateCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("PassengerDashboard", "Match saved/updated successfully for driver: " + match.getName());
                            completedMatches[0]++;

                            // Log progress
                            if (completedMatches[0] == totalMatches) {
                                Log.d("PassengerDashboard", "All " + totalMatches + " matches processed successfully");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("PassengerDashboard", "Failed to save match for driver: " + match.getName() + " - " + error);
                            completedMatches[0]++;
                        }
                    }
            );
        }
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
                    // Update navigation drawer header
                    updateNavigationHeader(user);

                    if (user.getHomeAddress() == null || user.getHomeAddress().isEmpty()) {
                        Log.d("PassengerDashboard", "No home address found in database");
                        showNoAddressMessage();
                    } else {
                        updateAddressCard(user.getHomeAddress());

                        if (user.hasCoordinates()) {
                            Log.d("PassengerDashboard", "Coordinates found in database, centering map");
                            centerMapOnLocation(user.getLatitude(), user.getLongitude(), user.getHomeAddress());
                        } else {
                            Log.d("PassengerDashboard", "No coordinates in database, geocoding address: " + user.getHomeAddress());
                            geocodeAddressAndUpdateUser(user.getHomeAddress(), user.getEmail(), false);
                        }
                    }

                    bottomSheet.setVisibility(View.VISIBLE);
                    bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Error loading user data: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PassengerDashboardActivity.this, "Failed to load user data", Toast.LENGTH_LONG).show();
                    showNoAddressMessage();
                    bottomSheet.setVisibility(View.VISIBLE);
                    bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
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
        cardAddress.setAlpha(0f);
        cardAddress.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
    }

    private void showEditAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Home Address");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_address, null);
        EditText etNewAddress = dialogView.findViewById(R.id.et_new_address);

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
        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        userRepository.updateUserProfile(userEmail, name, studentId, phone, address, password, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("PassengerDashboard", "Profile updated successfully");
                runOnUiThread(() -> {
                    Toast.makeText(PassengerDashboardActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    if (currentUser != null) {
                        currentUser.setName(name);
                        currentUser.setStudentId(studentId);
                        currentUser.setPhone(phone);
                        currentUser.setHomeAddress(address);
                        updateNavigationHeader(currentUser);
                        updateAddressCard(address);
                    }

                    drawerLayout.closeDrawer(GravityCompat.START);

                    if (currentUser != null && !address.equals(currentUser.getHomeAddress())) {
                        geocodeAddressAndUpdateUser(address, userEmail, true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PassengerDashboard", "Failed to update profile: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PassengerDashboardActivity.this, "Failed to update profile: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateUserAddress(String newAddress) {
        btnEdit.setEnabled(false);
        Toast.makeText(this, "Updating address...", Toast.LENGTH_SHORT).show();

        userRepository.updateUserAddress(userEmail, newAddress, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d("PassengerDashboard", "Address updated successfully");
                if (currentUser != null) {
                    currentUser.setHomeAddress(newAddress);
                }
                runOnUiThread(() -> {
                    updateAddressCard(newAddress);
                    btnEdit.setEnabled(true);
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

                userRepository.updateUserCoordinates(email, latitude, longitude, new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("PassengerDashboard", "Coordinates saved to database successfully");
                        runOnUiThread(() -> {
                            btnRefresh.setEnabled(true);
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
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Home")
                    .snippet(address));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
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
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        Log.d("PassengerDashboard", "Google Maps is ready");

        if (isUserDataLoaded && currentUser != null && currentUser.hasCoordinates()) {
            Log.d("PassengerDashboard", "Centering map on pre-existing coordinates");
            centerMapOnLocation(currentUser.getLatitude(), currentUser.getLongitude(), currentUser.getHomeAddress());
        }

        bottomSheet.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_carpool_matches) {
            Intent intent = new Intent(this, CarpoolMatchesActivity.class);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
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
            Intent intent = new Intent(PassengerDashboardActivity.this, MainActivity.class);
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
}