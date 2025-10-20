package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private OkHttpClient client;

    public UserRepository() {
        client = new OkHttpClient();
    }

    // Existing methods...
    public void updateUserCoordinates(String email, double latitude, double longitude, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("latitude", latitude);
            json.put("longitude", longitude);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email;
            Log.d(TAG, "Updating coordinates at: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Update coordinates failed: " + e.getMessage());
                    callback.onError("Update failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update coordinates response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Update failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    public void updateUserAddress(String email, String newAddress, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("home_address", newAddress);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email;
            Log.d(TAG, "Updating address at: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Update address failed: " + e.getMessage());
                    callback.onError("Update failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update address response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Update failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    public void updateUserProfile(String email, String name, String studentId, String phone,
                                  String homeAddress, String password, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("student_id", studentId);
            json.put("phone", phone);
            json.put("home_address", homeAddress);

            if (password != null && !password.trim().isEmpty()) {
                if (password.length() < 6) {
                    callback.onError("Password must be at least 6 characters");
                    return;
                }
                String passwordHash = hashPassword(password);
                json.put("password_hash", passwordHash);
            }

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email;
            Log.d(TAG, "Updating profile at: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Update profile failed: " + e.getMessage());
                    callback.onError("Update failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update profile response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorMsg = "Update failed with code: " + response.code();
                        if (response.code() == 400) {
                            errorMsg = "Invalid data format. Please check your information.";
                        } else if (response.code() == 401) {
                            errorMsg = "Authentication failed. Please try again.";
                        }
                        callback.onError(errorMsg);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    public void getUserByEmail(String email, UserFetchCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/users?email=eq." + email + "&select=*";
        Log.d(TAG, "Fetching user from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", AuthRepository.API_KEY)
                .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error fetching user: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "User fetch response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray usersArray = new JSONArray(responseBody);
                        if (usersArray.length() > 0) {
                            JSONObject userJson = usersArray.getJSONObject(0);
                            User user = new User();
                            user.setEmail(userJson.getString("email"));
                            user.setName(userJson.getString("name"));
                            user.setHomeAddress(userJson.getString("home_address"));
                            user.setStudentId(userJson.getString("student_id"));
                            user.setPhone(userJson.getString("phone"));
                            user.setRole(userJson.getString("role"));

                            if (userJson.has("verified") && !userJson.isNull("verified")) {
                                user.setVerified(userJson.getBoolean("verified"));
                            }
                            if (userJson.has("rating") && !userJson.isNull("rating")) {
                                user.setRating(userJson.getDouble("rating"));
                            }
                            if (userJson.has("latitude") && !userJson.isNull("latitude")) {
                                user.setLatitude(userJson.getDouble("latitude"));
                            }
                            if (userJson.has("longitude") && !userJson.isNull("longitude")) {
                                user.setLongitude(userJson.getDouble("longitude"));
                            }

                            Log.d(TAG, "User parsed successfully - Has coords: " + user.hasCoordinates());
                            callback.onSuccess(user);
                        } else {
                            Log.e(TAG, "No user found with email: " + email);
                            callback.onError("User not found");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("JSON parsing error");
                    }
                } else {
                    Log.e(TAG, "HTTP error fetching user: " + response.code());
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Get verified drivers with coordinates
    public void getVerifiedDriversWithCoordinates(DriversFetchCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/users?role=eq.driver&verified=eq.true&select=*";
        Log.d(TAG, "Fetching verified drivers from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", AuthRepository.API_KEY)
                .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error fetching drivers: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Drivers fetch response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray usersArray = new JSONArray(responseBody);
                        List<User> drivers = new ArrayList<>();

                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userJson = usersArray.getJSONObject(i);
                            User user = new User();
                            user.setEmail(userJson.getString("email"));
                            user.setName(userJson.getString("name"));
                            user.setRole(userJson.getString("role"));

                            if (userJson.has("verified") && !userJson.isNull("verified")) {
                                user.setVerified(userJson.getBoolean("verified"));
                            }
                            if (userJson.has("rating") && !userJson.isNull("rating")) {
                                user.setRating(userJson.getDouble("rating"));
                            }
                            if (userJson.has("latitude") && !userJson.isNull("latitude")) {
                                user.setLatitude(userJson.getDouble("latitude"));
                            }
                            if (userJson.has("longitude") && !userJson.isNull("longitude")) {
                                user.setLongitude(userJson.getDouble("longitude"));
                            }

                            // Only add drivers that have coordinates
                            if (user.hasCoordinates()) {
                                drivers.add(user);
                            }
                        }

                        Log.d(TAG, "Found " + drivers.size() + " verified drivers with coordinates");
                        callback.onSuccess(drivers);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("JSON parsing error");
                    }
                } else {
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Save match to database
    public void saveMatch(String passengerEmail, String driverEmail, double distance, int duration, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("passenger_email", passengerEmail);
            json.put("driver_email", driverEmail);
            json.put("distance_km", distance);
            json.put("duration_min", duration);
            json.put("status", "pending");
            json.put("created_at", new java.util.Date().toString());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches";
            Log.d(TAG, "Saving match to: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Save match failed: " + e.getMessage());
                    callback.onError("Save match failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Save match response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Save match failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

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
            return password;
        }
    }

    public interface UserUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UserFetchCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public interface DriversFetchCallback {
        void onSuccess(List<User> drivers);
        void onError(String error);
    }
}