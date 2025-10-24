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

public class DirectionsService {
    private static final String TAG = "DirectionsService";
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String API_KEY = "AIzaSyB6WmzAWb12sdN9oETPShgBSqkaaSlhO7w";

    private OkHttpClient client;

    public DirectionsService() {
        client = new OkHttpClient();
    }

    public void getRoute(double originLat, double originLng, double destLat, double destLng, RouteCallback callback) {
        try {
            String origin = originLat + "," + originLng;
            String destination = destLat + "," + destLng;

            String url = DIRECTIONS_API_URL + "?origin=" + URLEncoder.encode(origin, "UTF-8") +
                    "&destination=" + URLEncoder.encode(destination, "UTF-8") +
                    "&mode=driving&key=" + API_KEY;

            Log.d(TAG, "Directions URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Directions API failed: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Directions API response: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");

                            if ("OK".equals(status)) {
                                RouteResult routeResult = parseRouteResponse(jsonResponse);
                                if (routeResult != null) {
                                    callback.onSuccess(routeResult);
                                } else {
                                    callback.onError("Failed to parse route data");
                                }
                            } else {
                                String errorMessage = "Directions API error: " + status;
                                if (jsonResponse.has("error_message")) {
                                    errorMessage += " - " + jsonResponse.getString("error_message");
                                }
                                callback.onError(errorMessage);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Error parsing directions response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("HTTP error: " + response.code() + " - " + response.message());
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL encoding error: " + e.getMessage());
            callback.onError("URL encoding error");
        }
    }

    public void getRouteWithWaypoints(double originLat, double originLng, double waypointLat, double waypointLng,
                                      double destLat, double destLng, RouteCallback callback) {
        try {
            String origin = originLat + "," + originLng;
            String waypoint = waypointLat + "," + waypointLng;
            String destination = destLat + "," + destLng;

            String url = DIRECTIONS_API_URL + "?origin=" + URLEncoder.encode(origin, "UTF-8") +
                    "&destination=" + URLEncoder.encode(destination, "UTF-8") +
                    "&waypoints=" + URLEncoder.encode(waypoint, "UTF-8") +
                    "&mode=driving&key=" + API_KEY;

            Log.d(TAG, "Directions with waypoints URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Directions API with waypoints failed: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Directions API with waypoints response: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");

                            if ("OK".equals(status)) {
                                RouteResult routeResult = parseRouteResponse(jsonResponse);
                                if (routeResult != null) {
                                    callback.onSuccess(routeResult);
                                } else {
                                    callback.onError("Failed to parse route data");
                                }
                            } else {
                                String errorMessage = "Directions API error: " + status;
                                if (jsonResponse.has("error_message")) {
                                    errorMessage += " - " + jsonResponse.getString("error_message");
                                }
                                callback.onError(errorMessage);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Error parsing directions response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("HTTP error: " + response.code() + " - " + response.message());
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL encoding error: " + e.getMessage());
            callback.onError("URL encoding error");
        }
    }

    private RouteResult parseRouteResponse(JSONObject jsonResponse) throws JSONException {
        JSONArray routes = jsonResponse.getJSONArray("routes");
        if (routes.length() > 0) {
            JSONObject route = routes.getJSONObject(0);

            // Get overview polyline
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String encodedPolyline = overviewPolyline.getString("points");

            // Get total distance and duration
            JSONArray legs = route.getJSONArray("legs");
            double totalDistance = 0;
            int totalDuration = 0;

            for (int i = 0; i < legs.length(); i++) {
                JSONObject leg = legs.getJSONObject(i);
                JSONObject distance = leg.getJSONObject("distance");
                JSONObject duration = leg.getJSONObject("duration");

                totalDistance += distance.getDouble("value") / 1000.0; // Convert meters to km
                totalDuration += (int) Math.ceil(duration.getDouble("value") / 60.0); // Convert seconds to minutes
            }

            return new RouteResult(encodedPolyline, totalDistance, totalDuration);
        }
        return null;
    }

    public interface RouteCallback {
        void onSuccess(RouteResult routeResult);
        void onError(String error);
    }
}