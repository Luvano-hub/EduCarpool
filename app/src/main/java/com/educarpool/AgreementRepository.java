package com.educarpool;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AgreementRepository {
    private static final String TAG = "AgreementRepository";
    private OkHttpClient client;
    private UserRepository userRepository;

    public AgreementRepository() {
        client = new OkHttpClient();
        userRepository = new UserRepository();
    }

    public void createAgreement(Agreement agreement, UserRepository.UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("match_id", agreement.getMatchId());
            json.put("proposed_by", agreement.getProposedBy());
            json.put("pickup_time", agreement.getPickupTime());
            json.put("departure_time", agreement.getDepartureTime());
            json.put("pickup_location", agreement.getPickupLocation());
            json.put("dropoff_location", agreement.getDropoffLocation());
            json.put("price", agreement.getPrice());
            json.put("days_of_week", new JSONArray(agreement.getDaysOfWeek()));
            json.put("notes", agreement.getNotes());
            json.put("status", agreement.getStatus());
            json.put("payment_method", agreement.getPaymentMethod());
            json.put("pickup_notes", agreement.getPickupNotes());
            json.put("trip_frequency", agreement.getTripFrequency());
            json.put("emergency_contact", agreement.getEmergencyContact());
            json.put("passenger_count", agreement.getPassengerCount());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements";
            Log.d(TAG, "Creating agreement at: " + url);

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
                    Log.e(TAG, "Create agreement failed: " + e.getMessage());
                    callback.onError("Create agreement failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Create agreement response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Create agreement failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    public void getAgreementsByMatch(String matchId, AgreementsCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements?match_id=eq." + matchId + "&order=created_at.desc";
        Log.d(TAG, "Fetching agreements from: " + url);

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
                Log.e(TAG, "Network error fetching agreements: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Agreements response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray agreementsArray = new JSONArray(responseBody);
                        List<Agreement> agreements = parseAgreementsArray(agreementsArray);

                        Log.d(TAG, "Found " + agreements.size() + " agreements for match: " + matchId);
                        callback.onSuccess(agreements);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        callback.onError("Error parsing agreements data");
                    }
                } else {
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    public void updateAgreementStatus(String agreementId, String status, UserRepository.UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("status", status);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements?agreement_id=eq." + agreementId;
            Log.d(TAG, "Updating agreement status at: " + url);

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
                    Log.e(TAG, "Update agreement status failed: " + e.getMessage());
                    callback.onError("Update agreement status failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Update agreement status response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Update agreement status failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // NEW: Delete agreement when rejected
    public void deleteAgreement(String agreementId, UserRepository.UserUpdateCallback callback) {
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements?agreement_id=eq." + agreementId;
        Log.d(TAG, "Deleting agreement at: " + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", AuthRepository.API_KEY)
                .addHeader("Authorization", "Bearer " + AuthRepository.API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Delete agreement failed: " + e.getMessage());
                callback.onError("Delete agreement failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Delete agreement response - Code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Delete agreement failed with code: " + response.code());
                }
            }
        });
    }

    // NEW: Expire all other agreements when one is accepted
    public void expireOtherAgreements(String matchId, String currentAgreementId, UserRepository.UserUpdateCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("status", "expired");

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements?match_id=eq." + matchId + "&agreement_id=neq." + currentAgreementId + "&status=eq.proposed";
            Log.d(TAG, "Expiring other agreements at: " + url);

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
                    Log.e(TAG, "Expire other agreements failed: " + e.getMessage());
                    callback.onError("Expire other agreements failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Expire other agreements response - Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Expire other agreements failed with code: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // NEW: Send agreement reminder via chat
    public void sendAgreementReminder(String matchId, String userEmail, String otherUserEmail, String reminderType, UserRepository.UserUpdateCallback callback) {
        String messageText;

        switch (reminderType) {
            case "pickup_reminder":
                messageText = "🔔 Friendly reminder: You have a carpool pickup in 1 hour!";
                break;
            case "agreement_pending":
                messageText = "⏰ Reminder: You have a pending trip agreement waiting for your response!";
                break;
            case "weekly_reminder":
                messageText = "📅 Weekly carpool reminder: Don't forget your scheduled trips this week!";
                break;
            case "manual_reminder":
                messageText = "🔔 Reminder from your carpool partner about your active trip agreement!";
                break;
            default:
                messageText = "🔔 Carpool reminder";
        }

        userRepository.sendMessage(matchId, userEmail, otherUserEmail, messageText, callback);
    }

    // NEW: Get agreements that need reminders
    public void getAgreementsNeedingReminders(String userEmail, AgreementsCallback callback) {
        // This would query agreements that are active and have upcoming pickups
        // For now, we'll just get active agreements for the user
        String url = AuthRepository.SUPABASE_URL + "/rest/v1/agreements?status=eq.active&or=(proposed_by.eq." + userEmail + ",match_id.in.(select(id).from(matches).where(or(passenger_email.eq." + userEmail + ",driver_email.eq." + userEmail + "))))";

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
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray agreementsArray = new JSONArray(response.body().string());
                        List<Agreement> agreements = parseAgreementsArray(agreementsArray);
                        callback.onSuccess(agreements);
                    } catch (JSONException e) {
                        callback.onError("Error parsing agreements");
                    }
                } else {
                    callback.onError("HTTP error: " + response.code());
                }
            }
        });
    }

    // NEW: Add this helper method to parse agreements array
    private List<Agreement> parseAgreementsArray(JSONArray agreementsArray) throws JSONException {
        List<Agreement> agreements = new ArrayList<>();

        for (int i = 0; i < agreementsArray.length(); i++) {
            JSONObject agreementJson = agreementsArray.getJSONObject(i);

            Agreement agreement = new Agreement();
            agreement.setAgreementId(agreementJson.getString("agreement_id"));
            agreement.setMatchId(agreementJson.getString("match_id"));
            agreement.setProposedBy(agreementJson.getString("proposed_by"));

            // Handle optional fields
            if (agreementJson.has("pickup_time") && !agreementJson.isNull("pickup_time")) {
                agreement.setPickupTime(agreementJson.getString("pickup_time"));
            }
            if (agreementJson.has("departure_time") && !agreementJson.isNull("departure_time")) {
                agreement.setDepartureTime(agreementJson.getString("departure_time"));
            }
            if (agreementJson.has("pickup_location") && !agreementJson.isNull("pickup_location")) {
                agreement.setPickupLocation(agreementJson.getString("pickup_location"));
            }
            if (agreementJson.has("dropoff_location") && !agreementJson.isNull("dropoff_location")) {
                agreement.setDropoffLocation(agreementJson.getString("dropoff_location"));
            }
            if (agreementJson.has("price") && !agreementJson.isNull("price")) {
                agreement.setPrice(agreementJson.getDouble("price"));
            }

            // Parse days_of_week array
            if (agreementJson.has("days_of_week") && !agreementJson.isNull("days_of_week")) {
                JSONArray daysArray = agreementJson.getJSONArray("days_of_week");
                List<String> daysList = new ArrayList<>();
                for (int j = 0; j < daysArray.length(); j++) {
                    daysList.add(daysArray.getString(j));
                }
                agreement.setDaysOfWeek(daysList);
            }

            if (agreementJson.has("notes") && !agreementJson.isNull("notes")) {
                agreement.setNotes(agreementJson.getString("notes"));
            }
            agreement.setStatus(agreementJson.getString("status"));

            if (agreementJson.has("created_at") && !agreementJson.isNull("created_at")) {
                agreement.setCreatedAt(agreementJson.getString("created_at"));
            }

            agreements.add(agreement);
        }

        return agreements;
    }

    public interface AgreementsCallback {
        void onSuccess(List<Agreement> agreements);
        void onError(String error);
    }
}