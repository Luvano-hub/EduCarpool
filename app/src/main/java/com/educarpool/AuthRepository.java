package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthRepository {
    private static final String TAG = "AuthRepository";

    // public
    public static final String SUPABASE_URL = "https://qybcfkjhwykpnhqhveqq.supabase.co";
    public static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF5YmNma2pod3lrcG5ocWh2ZXFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAyNzE4NzMsImV4cCI6MjA3NTg0Nzg3M30.Bryi2_Fy4nNSkqGCptYZcvs-uufykf-9F9TFgoSv-Y8";

    // public
    public OkHttpClient client;

    public AuthRepository() {
        client = new OkHttpClient();
    }

    // Registration method - stores user directly in database with hashed password
    public void registerUser(User user, AuthCallback callback) {
        try {
            // Hash the password
            String passwordHash = hashPassword(user.getPassword());
            Log.d(TAG, "Password hashed successfully for: " + user.getEmail());

            // Build JSON with only the fields that exist in the database
            JSONObject json = new JSONObject();
            json.put("email", user.getEmail());
            json.put("password_hash", passwordHash);
            json.put("name", user.getName());
            json.put("student_id", user.getStudentId());
            json.put("phone", user.getPhone());
            json.put("home_address", user.getHomeAddress());
            json.put("role", user.getRole());

            // Set verified status: drivers start unverified, passengers are auto-verified
            boolean verified = !user.getRole().equals("driver");
            json.put("verified", verified);

            // Only add detour_range if it's provided (for drivers)
            if (user.getDetourRange() != null) {
                json.put("detour_range", user.getDetourRange());
            }

            // Set default rating for new users
            json.put("rating", 5.0);

            // Add Base64 image strings if they exist (for drivers)
            if (user.getLicenseUrl() != null) {
                json.put("license_url", user.getLicenseUrl());
            }
            if (user.getCarRegUrl() != null) {
                json.put("car_reg_url", user.getCarRegUrl());
            }

            Log.d(TAG, "Creating JSON for registration: " + json.toString(2)); // Pretty print

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/users")
                    .post(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            Log.d(TAG, "Sending registration request to: " + SUPABASE_URL + "/rest/v1/users");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Registration failed - Network error: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Registration response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "Registration successful for: " + user.getEmail());
                        callback.onSuccess();
                    } else {
                        String errorMsg;
                        switch (response.code()) {
                            case 400:
                                errorMsg = parse400Error(responseBody);
                                break;
                            case 401:
                                errorMsg = "Authentication failed. Please check API configuration.";
                                break;
                            case 409:
                                errorMsg = "User with this email or student ID already exists.";
                                break;
                            case 422:
                                errorMsg = "Validation error. Please check your input data.";
                                break;
                            case 500:
                                errorMsg = "Server error. Please try again later.";
                                break;
                            default:
                                errorMsg = "Registration failed (HTTP " + response.code() + ")";
                                break;
                        }
                        Log.e(TAG, "Registration error: " + errorMsg + " - Response: " + responseBody);
                        callback.onError(errorMsg);
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("Registration error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            callback.onError("Unexpected error: " + e.getMessage());
        }
    }

    // Parse 400 error to get more specific information
    private String parse400Error(String responseBody) {
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            if (errorJson.has("message")) {
                return errorJson.getString("message");
            }
            if (errorJson.has("details")) {
                return errorJson.getString("details");
            }
            if (errorJson.has("hint")) {
                return errorJson.getString("hint");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse 400 error response: " + e.getMessage());
        }
        return "Invalid data format. Please check your information.";
    }

    // Login method - verifies password against stored hash
    public void loginUser(String email, String password, LoginCallback callback) {
        try {
            Log.d(TAG, "Attempting login for: " + email);

            // First, get the user by email
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/users?email=eq." + email + "&select=*")
                    .get()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Sending login request to: " + SUPABASE_URL + "/rest/v1/users?email=eq." + email);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Login failed - Network error: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Login response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONArray usersArray = new JSONArray(responseBody);
                            if (usersArray.length() > 0) {
                                JSONObject userJson = usersArray.getJSONObject(0);

                                // Check if user has password_hash field
                                if (!userJson.has("password_hash")) {
                                    Log.e(TAG, "User record missing password_hash field");
                                    callback.onError("Account configuration error. Please contact support.");
                                    return;
                                }

                                String storedHash = userJson.getString("password_hash");
                                String inputHash = hashPassword(password);

                                Log.d(TAG, "Password verification - Stored: " + storedHash.substring(0, 10) + "... Input: " + inputHash.substring(0, 10) + "...");

                                // Verify password
                                if (storedHash.equals(inputHash)) {
                                    User user = new User();
                                    user.setEmail(userJson.getString("email"));
                                    user.setName(userJson.getString("name"));
                                    user.setRole(userJson.getString("role"));
                                    user.setVerified(userJson.getBoolean("verified"));
                                    user.setStudentId(userJson.getString("student_id"));

                                    Log.d(TAG, "Login successful for: " + user.getEmail() + " Role: " + user.getRole() + " Verified: " + user.getVerified());
                                    callback.onSuccess(user);
                                } else {
                                    Log.w(TAG, "Password mismatch for: " + email);
                                    callback.onError("Invalid email or password");
                                }
                            } else {
                                Log.w(TAG, "No user found with email: " + email);
                                callback.onError("Invalid email or password");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Login failed: Invalid response format");
                        }
                    } else {
                        String errorMsg;
                        switch (response.code()) {
                            case 400:
                                errorMsg = "Invalid request.";
                                break;
                            case 401:
                                errorMsg = "Authentication failed.";
                                break;
                            case 404:
                                errorMsg = "User not found.";
                                break;
                            default:
                                errorMsg = "Login failed (HTTP " + response.code() + ")";
                                break;
                        }
                        Log.e(TAG, "Login error: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Login error: " + e.getMessage());
            callback.onError("Login error: " + e.getMessage());
        }
    }

    // Simple password hashing function (SHA-256)
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Hashing algorithm not found: " + e.getMessage());
            return password; // Fallback to plain text (not secure)
        }
    }

    // Interfaces for callbacks
    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface LoginCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}