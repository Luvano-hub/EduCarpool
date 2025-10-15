package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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

                            // Handle coordinates - they might be null initially
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

    public interface UserUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UserFetchCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}