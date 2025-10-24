package com.educarpool;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.*;

public class SupabaseRepository {
    private static final String BASE_URL = "https://YOUR-PROJECT.supabase.co/rest/v1/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient();
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback<T> { void onResult(@Nullable T data, @Nullable Throwable err); }

    private String getAnonKey() {
        // TODO: secure storage; never hardcode in production
        return "YOUR_SUPABASE_ANON_KEY";
    }
    private String getUserJwt() {
        // TODO: return the GoTrue user access token after sign-in (preferred over anon)
        return "USER_JWT_OR_ANON";
    }

    private Request.Builder baseRequest(String path) {
        return new Request.Builder()
                .url(BASE_URL + path)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer " + getUserJwt())
                .addHeader("Accept-Profile", "public")  // schema
                .addHeader("Content-Profile", "public");
    }

    // 3.1 Fetch accepted matches for a user (either passenger or driver)
    public void fetchAcceptedMatches(UUID myUserId, Callback<JSONArray> cb) {
        // status=eq.accepted&or=(passenger_id.eq.<id>,driver_id.eq.<id>)
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL + "matches"))
                .newBuilder()
                .addQueryParameter("select", "match_id,passenger_id,driver_id")
                .addQueryParameter("status", "eq.accepted")
                .addQueryParameter("or", "(passenger_id.eq." + myUserId + ",driver_id.eq." + myUserId + ")")
                .build();
        Request req = baseRequest("matches")
                .url(url)
                .get()
                .addHeader("Accept", "application/json")
                .build();
        run(req, cb);
    }

    // 3.2 Fetch user profiles (batch by ids) – assumes 'users' table has id, name, photo_url
    public void fetchUsersByIds(List<UUID> ids, Callback<JSONArray> cb) {
        String inList = "(" + joinUUIDs(ids) + ")";
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL + "users"))
                .newBuilder()
                .addQueryParameter("select", "id,name,photo_url")
                .addQueryParameter("id", "in." + inList)
                .build();
        Request req = baseRequest("users").url(url).get().build();
        run(req, cb);
    }

    // 3.3 Last message per match (preview/timestamp)
    public void fetchLastMessage(UUID matchId, Callback<JSONArray> cb) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL + "messages"))
                .newBuilder()
                .addQueryParameter("select", "message_id,message_text,timestamp")
                .addQueryParameter("match_id", "eq." + matchId)
                .addQueryParameter("order", "timestamp.desc")
                .addQueryParameter("limit", "1")
                .build();
        Request req = baseRequest("messages").url(url).get().build();
        run(req, cb);
    }

    // 3.4 Load full chat history
    public void fetchMessages(UUID matchId, Callback<JSONArray> cb) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL + "messages"))
                .newBuilder()
                .addQueryParameter("select", "message_id,match_id,sender_id,receiver_id,message_text,timestamp")
                .addQueryParameter("match_id", "eq." + matchId)
                .addQueryParameter("order", "timestamp.asc")
                .build();
        Request req = baseRequest("messages").url(url).get().build();
        run(req, cb);
    }

    // 3.5 Send a message
    public void sendMessage(UUID matchId, UUID senderId, UUID receiverId, String text, Callback<JSONObject> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("match_id", matchId.toString());
            body.put("sender_id", senderId.toString());
            body.put("receiver_id", receiverId.toString());
            body.put("message_text", text);
        } catch (Exception ignored) {}

        Request req = baseRequest("messages")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Prefer", "return=representation") // get inserted row back
                .build();
        runObj(req, cb);
    }

    // 3.6 Simple polling (fallback while you wire Realtime)
    private final Map<UUID, Runnable> pollers = new HashMap<>();
    public void startPolling(UUID matchId, long ms, Callback<JSONArray> onUpdate) {
        stopPolling(matchId);
        Runnable r = new Runnable() {
            @Override public void run() {
                fetchMessages(matchId, (data, err) -> {
                    onUpdate.onResult(data, err);
                    main.postDelayed(this, ms);
                });
            }
        };
        pollers.put(matchId, r);
        main.post(r);
    }
    public void stopPolling(UUID matchId) {
        Runnable r = pollers.remove(matchId);
        if (r != null) main.removeCallbacks(r);
    }

    // --- helpers ---
    private String joinUUIDs(List<UUID> ids) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            b.append(ids.get(i));
            if (i < ids.size() - 1) b.append(",");
        }
        return b.toString();
    }

    private void run(Request req, Callback<JSONArray> cb) {
        bg.submit(() -> {
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                String s = resp.body() != null ? resp.body().string() : "[]";
                JSONArray arr = new JSONArray(s);
                main.post(() -> cb.onResult(arr, null));
            } catch (Throwable t) {
                main.post(() -> cb.onResult(null, t));
            }
        });
    }
    private void runObj(Request req, Callback<JSONObject> cb) {
        bg.submit(() -> {
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                String s = resp.body() != null ? resp.body().string() : "{}";
                JSONArray arr = new JSONArray(s);
                JSONObject obj = arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
                main.post(() -> cb.onResult(obj, null));
            } catch (Throwable t) {
                main.post(() -> cb.onResult(null, t));
            }
        });
    }
}

