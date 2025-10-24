package com.educarpool;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnCall, btnHandshake;
    private TextView tvUserName;
    private MessagesAdapter messagesAdapter;
    private UserRepository userRepository;

    private String matchId;
    private String userEmail;
    private String otherUserEmail;
    private String otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        userRepository = new UserRepository();

        // Get data from intent
        matchId = getIntent().getStringExtra("match_id");
        userEmail = getIntent().getStringExtra("user_email");
        otherUserEmail = getIntent().getStringExtra("other_user_email");

        if (matchId == null || userEmail == null || otherUserEmail == null) {
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        setupMessagesList();
        loadOtherUserName();
        loadMessages();
    }

    private void initializeViews() {
        recyclerMessages = findViewById(R.id.recycler_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        btnCall = findViewById(R.id.btn_call);
        btnHandshake = findViewById(R.id.btn_handshake);
        tvUserName = findViewById(R.id.tv_user_name);

        // Set the other user's name (will be updated when loaded)
        tvUserName.setText("Loading...");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCall.setOnClickListener(v -> {
            Toast.makeText(this, "Call feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // ✅ Updated handshake button click listener
        btnHandshake.setOnClickListener(v -> {
            openAgreementPanel();
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    // ✅ New method to open the Agreement Panel
    private void openAgreementPanel() {
        Intent intent = new Intent(ChatActivity.this, AgreementPanelActivity.class);
        intent.putExtra("match_id", matchId);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("other_user_email", otherUserEmail);
        startActivity(intent);
    }

    private void setupMessagesList() {
        messagesAdapter = new MessagesAdapter(new ArrayList<>(), userEmail);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messagesAdapter);
    }

    private void loadOtherUserName() {
        userRepository.getUserByEmail(otherUserEmail, new UserRepository.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    otherUserName = user.getName();
                    tvUserName.setText(otherUserName);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ChatActivity", "Error loading user name: " + error);
                runOnUiThread(() -> {
                    // Fallback to email username
                    String username = otherUserEmail.split("@")[0];
                    otherUserName = username;
                    tvUserName.setText(username);
                });
            }
        });
    }

    private void loadMessages() {
        userRepository.getMessages(matchId, new UserRepository.MessagesCallback() {
            @Override
            public void onSuccess(List<Message> messages) {
                runOnUiThread(() -> {
                    messagesAdapter.updateMessages(messages);
                    // Scroll to bottom
                    if (messages.size() > 0) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ChatActivity", "Error loading messages: " + error);
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Disable send button temporarily
        btnSend.setEnabled(false);

        userRepository.sendMessage(matchId, userEmail, otherUserEmail, messageText,
                new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            etMessage.setText("");
                            btnSend.setEnabled(true);
                            loadMessages(); // Reload messages to show the new one
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(ChatActivity.this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
}
