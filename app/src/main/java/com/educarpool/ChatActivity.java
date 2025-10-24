package com.educarpool;

import com.educarpool.TimeUtils;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.educarpool.R;
import com.educarpool.SupabaseRepository;
import com.educarpool.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private SupabaseRepository repo;
    private UUID myUserId; // TODO set from session
    private UUID matchId, otherUserId;

    private TextView tvTopName;
    private RecyclerView rv;
    private EditText et;
    private ImageButton btnSend, btnHandshake, btnBack, btnCall;
    private com.educarpool.ChatAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_chat);

        repo = new SupabaseRepository();
        myUserId = /* TODO: */ UUID.fromString("REPLACE_WITH_SIGNED_IN_USER_ID");

        matchId = UUID.fromString(getIntent().getStringExtra("match_id"));
        otherUserId = UUID.fromString(getIntent().getStringExtra("other_user_id"));
        String otherName = getIntent().getStringExtra("other_name");

        tvTopName = findViewById(R.id.tvTopName);
        tvTopName.setText(otherName != null ? otherName : "User");

        rv = findViewById(R.id.rvMessages);
        et = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnHandshake = findViewById(R.id.btnHandshake);
        btnBack = findViewById(R.id.btnBack);
        btnCall = findViewById(R.id.btnCall);

        adapter = new ChatAdapter(myUserId);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Call coming soon", Toast.LENGTH_SHORT).show());
        btnHandshake.setOnClickListener(v -> Toast.makeText(this, "Agreement panel coming soon", Toast.LENGTH_SHORT).show());

        btnSend.setOnClickListener(v -> {
            String text = et.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            sendMessage(text);
        });

        loadHistory();
    }

    private void loadHistory() {
        repo.fetchMessages(matchId, (arr, err) -> {
            if (err != null) { Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show(); return; }
            adapter.submit(parseMessages(arr));
            rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
        });

        // Realtime fallback: polling every 2000ms
        repo.startPolling(matchId, 2000, (arr, err) -> {
            if (arr != null) {
                int oldCount = adapter.getItemCount();
                adapter.submit(parseMessages(arr));
                if (adapter.getItemCount() > oldCount) {
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });
    }


    private List<ChatMessage> parseMessages(JSONArray arr) {
        List<ChatMessage> list = new ArrayList<>();
        if (arr == null) return list;
        try {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ChatMessage m = new ChatMessage();
                m.messageId = UUID.fromString(o.getString("message_id"));
                m.matchId = UUID.fromString(o.getString("match_id"));
                m.senderId = UUID.fromString(o.getString("sender_id"));
                m.receiverId = UUID.fromString(o.getString("receiver_id"));
                m.messageText = o.getString("message_text");
                // parse ISO timestamp -> epoch
                m.timestampMillis = TimeUtils.parseIsoOffsetToEpochMillis(o.getString("timestamp"));
                list.add(m);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void sendMessage(String text) {
        et.setText("");
        repo.sendMessage(matchId, myUserId, otherUserId, text, (obj, err) -> {
            if (err != null) {
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
                return;
            }
            // optimistically reload (poller will also catch it)
            repo.fetchMessages(matchId, (arr, e2) -> {
                if (arr != null) {
                    adapter.submit(parseMessages(arr));
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                }
            });
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (matchId != null) repo.stopPolling(matchId);
    }
}

