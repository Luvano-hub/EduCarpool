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

    // UPDATED: saveMatch method with duplicate prevention
    public void saveMatch(String passengerEmail, String driverEmail, double distance, int duration, UserUpdateCallback callback) {
        try {
            // First, check if a pending match already exists between this passenger and driver
            String checkUrl = AuthRepository.SUPABASE_URL + "/rest/v1/matches?passenger_email=eq." + passengerEmail +
                    "&driver_email=eq." + driverEmail + "&status=eq.pending";
            Log.d(TAG, "Checking for existing match: " + checkUrl);

            Request checkRequest = new Request.Builder()
                    .url(checkUrl)
                    .get()
                    .addHeader("apikey", AuthRepository.API_KEY)
                    .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(checkRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Check existing match failed: " + e.getMessage());
                    // If check fails, try to insert as new
                    insertNewMatch(passengerEmail, driverEmail, distance, duration, callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Check existing match response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONArray existingMatches = new JSONArray(responseBody);
                            if (existingMatches.length() > 0) {
                                // Existing match found - update it
                                JSONObject existingMatch = existingMatches.getJSONObject(0);
                                String matchId = existingMatch.getString("id");
                                updateExistingMatch(matchId, distance, duration, callback);
                            } else {
                                // No existing match - insert new
                                insertNewMatch(passengerEmail, driverEmail, distance, duration, callback);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error in check: " + e.getMessage());
                            // If parsing fails, try to insert as new
                            insertNewMatch(passengerEmail, driverEmail, distance, duration, callback);
                        }
                    } else {
                        // If check fails, try to insert as new
                        insertNewMatch(passengerEmail, driverEmail, distance, duration, callback);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in saveMatch: " + e.getMessage());
            callback.onError("Error saving match: " + e.getMessage());
        }
    }

    // Helper method to insert a new match
    private void insertNewMatch(String passengerEmail, String driverEmail, double distance, int duration, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("passenger_email", passengerEmail);
            json.put("driver_email", driverEmail);
            json.put("distance_km", distance);
            json.put("duration_min", duration);
            json.put("status", "pending");

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches";
            Log.d(TAG, "Inserting new match to: " + url);

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
                    Log.e(TAG, "Insert new match failed: " + e.getMessage());
                    callback.onError("Insert match failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Insert new match response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Insert match failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in insert: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Helper method to update an existing match
    private void updateExistingMatch(String matchId, double distance, int duration, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("distance_km", distance);
            json.put("duration_min", duration);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches?id=eq." + matchId;
            Log.d(TAG, "Updating existing match at: " + url);

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
                    Log.e(TAG, "Update existing match failed: " + e.getMessage());
                    callback.onError("Update match failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update existing match response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Update match failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in update: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // FIXED: getPendingRequests method - removed nested select that was causing HTTP 300 error
    public void getPendingRequests(String driverEmail, RequestsCallback callback) {
        // Simple query without complex nested selects
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches?driver_email=eq." + driverEmail + "&status=eq.pending";
        Log.d(TAG, "Fetching pending requests from: " + url);

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
                Log.e(TAG, "Network error fetching requests: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Requests fetch response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray requestsArray = new JSONArray(responseBody);
                        List<RideRequest> requests = new ArrayList<>();

                        for (int i = 0; i < requestsArray.length(); i++) {
                            JSONObject requestJson = requestsArray.getJSONObject(i);

                            RideRequest rideRequest = new RideRequest();
                            rideRequest.setId(requestJson.getString("id"));
                            rideRequest.setPassengerEmail(requestJson.getString("passenger_email"));
                            rideRequest.setDriverEmail(requestJson.getString("driver_email"));
                            rideRequest.setDistance(requestJson.getDouble("distance_km"));
                            rideRequest.setDuration(requestJson.getInt("duration_min"));
                            rideRequest.setStatus(requestJson.getString("status"));

                            if (requestJson.has("created_at") && !requestJson.isNull("created_at")) {
                                rideRequest.setCreatedAt(requestJson.getString("created_at"));
                            }

                            // For passenger details, we'll need to make a separate call
                            // or update the query to use proper joins if supported by Supabase
                            // For now, we'll set passenger name as email (temporary)
                            rideRequest.setPassengerName(requestJson.getString("passenger_email"));

                            requests.add(rideRequest);
                            Log.d(TAG, "Parsed request from: " + rideRequest.getPassengerEmail());
                        }

                        Log.d(TAG, "Found " + requests.size() + " pending requests");

                        // If we have requests, fetch passenger details for each
                        if (!requests.isEmpty()) {
                            fetchPassengerDetails(requests, callback);
                        } else {
                            callback.onSuccess(requests);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("Error parsing requests data");
                    }
                } else {
                    Log.e(TAG, "HTTP error fetching requests: " + response.code());
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Helper method to fetch passenger details for requests
    private void fetchPassengerDetails(List<RideRequest> requests, RequestsCallback callback) {
        // Create a list to track completed requests
        List<RideRequest> completedRequests = new ArrayList<>();
        final int totalRequests = requests.size();

        for (RideRequest request : requests) {
            getUserByEmail(request.getPassengerEmail(), new UserFetchCallback() {
                @Override
                public void onSuccess(User passenger) {
                    request.setPassengerName(passenger.getName());
                    request.setPassengerHomeAddress(passenger.getHomeAddress());
                    request.setPassengerLat(passenger.getLatitude());
                    request.setPassengerLng(passenger.getLongitude());

                    completedRequests.add(request);

                    // When all requests are processed, return the complete list
                    if (completedRequests.size() == totalRequests) {
                        callback.onSuccess(requests);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to fetch passenger details: " + error);
                    completedRequests.add(request);

                    // Continue even if some passenger details fail
                    if (completedRequests.size() == totalRequests) {
                        callback.onSuccess(requests);
                    }
                }
            });
        }
    }

    public void updateRequestStatus(String requestId, String status, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("status", status);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches?id=eq." + requestId;
            Log.d(TAG, "Updating request status at: " + url);

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
                    Log.e(TAG, "Update request status failed: " + e.getMessage());
                    callback.onError("Update failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update request status response - Code: " + response.code() + ", Body: " + responseBody);

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

    // NEW: Get accepted matches for a user (both as passenger and driver)
    public void getAcceptedMatches(String userEmail, AcceptedMatchesCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/matches?or=(passenger_email.eq." + userEmail + ",driver_email.eq." + userEmail + ")&status=eq.accepted";
        Log.d(TAG, "Fetching accepted matches from: " + url);

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
                Log.e(TAG, "Network error fetching accepted matches: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Accepted matches response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray matchesArray = new JSONArray(responseBody);
                        List<AcceptedMatch> acceptedMatches = new ArrayList<>();

                        for (int i = 0; i < matchesArray.length(); i++) {
                            JSONObject matchJson = matchesArray.getJSONObject(i);

                            AcceptedMatch match = new AcceptedMatch();
                            match.setId(matchJson.getString("id"));
                            match.setPassengerEmail(matchJson.getString("passenger_email"));
                            match.setDriverEmail(matchJson.getString("driver_email"));
                            match.setDistance(matchJson.getDouble("distance_km"));
                            match.setDuration(matchJson.getInt("duration_min"));
                            match.setStatus(matchJson.getString("status"));

                            if (matchJson.has("created_at") && !matchJson.isNull("created_at")) {
                                match.setCreatedAt(matchJson.getString("created_at"));
                            }

                            acceptedMatches.add(match);
                        }

                        Log.d(TAG, "Found " + acceptedMatches.size() + " accepted matches");
                        callback.onSuccess(acceptedMatches);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("Error parsing accepted matches data");
                    }
                } else {
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Get messages for a specific match
    public void getMessages(String matchId, MessagesCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/messages?match_id=eq." + matchId + "&order=timestamp.asc";
        Log.d(TAG, "Fetching messages from: " + url);

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
                Log.e(TAG, "Network error fetching messages: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Messages response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray messagesArray = new JSONArray(responseBody);
                        List<Message> messages = new ArrayList<>();

                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageJson = messagesArray.getJSONObject(i);

                            Message message = new Message();
                            message.setId(messageJson.getString("id"));
                            message.setMatchId(messageJson.getString("match_id"));
                            message.setSenderId(messageJson.getString("sender_id"));
                            message.setReceiverId(messageJson.getString("receiver_id"));
                            message.setMessageText(messageJson.getString("message_text"));

                            if (messageJson.has("timestamp") && !messageJson.isNull("timestamp")) {
                                message.setTimestamp(messageJson.getString("timestamp"));
                            }
                            if (messageJson.has("is_read") && !messageJson.isNull("is_read")) {
                                message.setRead(messageJson.getBoolean("is_read"));
                            }

                            messages.add(message);
                        }

                        Log.d(TAG, "Found " + messages.size() + " messages for match: " + matchId);
                        callback.onSuccess(messages);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("Error parsing messages data");
                    }
                } else {
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Send a new message
    public void sendMessage(String matchId, String senderId, String receiverId, String messageText, UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("match_id", matchId);
            json.put("sender_id", senderId);
            json.put("receiver_id", receiverId);
            json.put("message_text", messageText);
            json.put("timestamp", new java.util.Date().toString());
            json.put("is_read", false);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/messages";
            Log.d(TAG, "Sending message to: " + url);

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
                    Log.e(TAG, "Send message failed: " + e.getMessage());
                    callback.onError("Send message failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Send message response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Send message failed with code: " + response.code());
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

    public interface RequestsCallback {
        void onSuccess(List<RideRequest> requests);
        void onError(String error);
    }

    // NEW: Add these interfaces to UserRepository
    public interface AcceptedMatchesCallback {
        void onSuccess(List<AcceptedMatch> matches);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onSuccess(List<Message> messages);
        void onError(String error);
    }
}