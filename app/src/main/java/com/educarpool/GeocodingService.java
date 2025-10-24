package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeocodingService {
    private static final String TAG = "GeocodingService";
    private static final String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String API_KEY = "AIzaSyB6WmzAWb12sdN9oETPShgBSqkaaSlhO7w";

    private OkHttpClient client;

    public GeocodingService() {
        client = new OkHttpClient();
    }

    public void geocodeAddress(String address, GeocodingCallback callback) {
        try {
            // Use the address exactly as stored in database
            String encodedAddress = URLEncoder.encode(address, "UTF-8");
            String url = GEOCODING_API_URL + "?address=" + encodedAddress + "&key=" + API_KEY;

            Log.d(TAG, "Geocoding URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Geocoding failed: " + e.getMessage());
                    callback.onError("Geocoding failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Geocoding full response: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");

                            if ("OK".equals(status)) {
                                JSONArray results = jsonResponse.getJSONArray("results");
                                if (results.length() > 0) {
                                    JSONObject firstResult = results.getJSONObject(0);
                                    JSONObject geometry = firstResult.getJSONObject("geometry");
                                    JSONObject location = geometry.getJSONObject("location");

                                    double lat = location.getDouble("lat");
                                    double lng = location.getDouble("lng");

                                    String formattedAddress = firstResult.getString("formatted_address");
                                    Log.d(TAG, "Geocoding successful - Lat: " + lat + ", Lng: " + lng + ", Address: " + formattedAddress);
                                    callback.onSuccess(lat, lng);
                                } else {
                                    callback.onError("No results found for address");
                                }
                            } else {
                                callback.onError("Geocoding API error: " + status);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Error parsing geocoding response");
                        }
                    } else {
                        callback.onError("HTTP error: " + response.code());
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL encoding error: " + e.getMessage());
            callback.onError("Address encoding error");
        }
    }

    public interface GeocodingCallback {
        void onSuccess(double latitude, double longitude);
        void onError(String error);
    }
}