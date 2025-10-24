package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DistanceMatrixService {
    private static final String TAG = "DistanceMatrixService";
    private static final String DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String API_KEY = "AIzaSyB6WmzAWb12sdN9oETPShgBSqkaaSlhO7w";

    private OkHttpClient client;

    public DistanceMatrixService() {
        client = new OkHttpClient();
    }

    public void calculateDistances(double originLat, double originLng, List<User> drivers, DistanceMatrixCallback callback) {
        if (drivers == null || drivers.isEmpty()) {
            callback.onError("No drivers provided");
            return;
        }

        try {
            // Build destinations parameter
            StringBuilder destinationsBuilder = new StringBuilder();
            for (int i = 0; i < drivers.size(); i++) {
                User driver = drivers.get(i);
                if (driver.hasCoordinates()) {
                    if (destinationsBuilder.length() > 0) {
                        destinationsBuilder.append("|");
                    }
                    destinationsBuilder.append(driver.getLatitude()).append(",").append(driver.getLongitude());
                }
            }

            if (destinationsBuilder.length() == 0) {
                callback.onError("No drivers with valid coordinates");
                return;
            }

            String origins = originLat + "," + originLng;
            String destinations = destinationsBuilder.toString();

            String url = DISTANCE_MATRIX_API_URL + "?origins=" + URLEncoder.encode(origins, "UTF-8") +
                    "&destinations=" + URLEncoder.encode(destinations, "UTF-8") +
                    "&key=" + API_KEY;

            Log.d(TAG, "Distance Matrix URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Distance Matrix failed: " + e.getMessage());
                    callback.onError("Distance calculation failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Distance Matrix response: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");

                            if ("OK".equals(status)) {
                                List<DriverMatch> matches = parseDistanceMatrixResponse(jsonResponse, drivers);
                                callback.onSuccess(matches);
                            } else {
                                callback.onError("Distance Matrix API error: " + status);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Error parsing distance matrix response");
                        }
                    } else {
                        callback.onError("HTTP error: " + response.code());
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL encoding error: " + e.getMessage());
            callback.onError("URL encoding error");
        }
    }

    private List<DriverMatch> parseDistanceMatrixResponse(JSONObject jsonResponse, List<User> drivers) throws JSONException {
        List<DriverMatch> matches = new ArrayList<>();

        JSONArray rows = jsonResponse.getJSONArray("rows");
        if (rows.length() > 0) {
            JSONObject firstRow = rows.getJSONObject(0);
            JSONArray elements = firstRow.getJSONArray("elements");

            // Create a list of drivers that actually have coordinates
            List<User> validDrivers = new ArrayList<>();
            for (User driver : drivers) {
                if (driver.hasCoordinates()) {
                    validDrivers.add(driver);
                }
            }

            for (int i = 0; i < elements.length() && i < validDrivers.size(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String elementStatus = element.getString("status");

                if ("OK".equals(elementStatus)) {
                    User driver = validDrivers.get(i);

                    JSONObject distance = element.getJSONObject("distance");
                    JSONObject duration = element.getJSONObject("duration");

                    double distanceKm = distance.getDouble("value") / 1000.0; // Convert meters to km
                    int durationMin = (int) Math.ceil(duration.getDouble("value") / 60.0); // Convert seconds to minutes

                    DriverMatch match = new DriverMatch();
                    match.setId(driver.getEmail()); // Use email as ID for now
                    match.setName(driver.getName());
                    match.setRating(driver.getRating() != null ? driver.getRating() : 5.0);
                    match.setDistance(distanceKm);
                    match.setDuration(durationMin);
                    match.setVerified(driver.getVerified() != null ? driver.getVerified() : false);
                    match.setLatitude(driver.getLatitude());
                    match.setLongitude(driver.getLongitude());

                    matches.add(match);
                    Log.d(TAG, "Match calculated - Driver: " + driver.getName() +
                            ", Distance: " + distanceKm + " km, Duration: " + durationMin + " min");
                }
            }
        }

        return matches;
    }

    public interface DistanceMatrixCallback {
        void onSuccess(List<DriverMatch> matches);
        void onError(String error);
    }
}