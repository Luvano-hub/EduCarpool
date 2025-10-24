package com.educarpool;

import com.educarpool.TimeUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.educarpool.R;
import com.educarpool.SupabaseRepository;
import com.educarpool.MatchSummary;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CarpoolMatchesActivity extends AppCompatActivity {

    private SupabaseRepository repo;
    private UUID myUserId; // TODO: set from your auth/session
    private RecyclerView rv;
    private TextView tvEmpty;
    private MatchAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_carpool_matches_list);

        repo = new SupabaseRepository();
        myUserId = /* TODO: */ UUID.fromString("REPLACE_WITH_SIGNED_IN_USER_ID");

        rv = findViewById(R.id.rvMatches);
        tvEmpty = findViewById(R.id.tvEmpty);
        ((ImageButton)findViewById(R.id.btnBack)).setOnClickListener(v -> onBackPressed());

        adapter = new MatchAdapter(m -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("match_id", m.matchId.toString());
            i.putExtra("other_user_id", m.otherUserId.toString());
            i.putExtra("other_name", m.otherName);
            startActivity(i);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadMatches();
    }


    private void loadMatches() {
        repo.fetchAcceptedMatches(myUserId, (arr, err) -> {
            if (err != null) { showEmpty(true); return; }
            // collect other user IDs
            List<UUID> otherIds = new ArrayList<>();
            Map<UUID, UUID> matchToOther = new HashMap<>();

            try {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    UUID matchId = UUID.fromString(row.getString("match_id"));
                    UUID passenger = UUID.fromString(row.getString("passenger_id"));
                    UUID driver = UUID.fromString(row.getString("driver_id"));
                    UUID other = myUserId.equals(passenger) ? driver : passenger;
                    otherIds.add(other);
                    matchToOther.put(matchId, other);
                }
            } catch (Exception e) { showEmpty(true); return; }

            if (matchToOther.isEmpty()) { showEmpty(true); return; }

            repo.fetchUsersByIds(otherIds, (users, e2) -> {
                if (e2 != null) { showEmpty(true); return; }
                Map<UUID, String> names = new HashMap<>();
                try {
                    for (int i = 0; i < users.length(); i++) {
                        JSONObject u = users.getJSONObject(i);
                        names.put(UUID.fromString(u.getString("id")), u.optString("name", "User"));
                    }
                } catch (Exception ignored) {}

                // Build match summaries and attach last message
                List<MatchSummary> result = new ArrayList<>();
                final int[] pending = {matchToOther.size()};
                for (UUID matchId : matchToOther.keySet()) {
                    UUID other = matchToOther.get(matchId);
                    String name = names.getOrDefault(other, "User");
                    // placeholder – we fetch last message for each
                    repo.fetchLastMessage(matchId, (lastArr, e3) -> {
                        String preview = null; long ts = 0;
                        try {
                            if (lastArr != null && lastArr.length() > 0) {
                                JSONObject m = lastArr.getJSONObject(0);
                                preview = m.optString("message_text", null);
                                ts = TimeUtils.parseIsoOffsetToEpochMillis(m.getString("timestamp"));
                            }
                        } catch (Exception ignored) {}
                        synchronized (result) {
                            result.add(new MatchSummary(matchId, other, name, null, preview, ts));
                            pending[0]--;
                            if (pending[0] == 0) {
                                // sort by last message desc
                                result.sort((a,b) -> Long.compare(b.lastMessageEpochMillis, a.lastMessageEpochMillis));
                                adapter.submit(result);
                                showEmpty(result.isEmpty());
                            }
                        }
                    });
                }
            });
        });
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rv.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
