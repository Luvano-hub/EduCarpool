package com.educarpool;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CarpoolMatchesActivity extends AppCompatActivity {

    private RecyclerView recyclerMatches;
    private LinearLayout tvEmptyState;
    private MaterialButton btnBackToDashboard;
    private ImageButton btnBack;
    private UserRepository userRepository;
    private String userEmail;
    private MatchesAdapter matchesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpool_matches);

        userRepository = new UserRepository();

        // Get user email from intent
        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null) {
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        setupMatchesList();
        loadAcceptedMatches();
    }

    private void initializeViews() {
        recyclerMatches = findViewById(R.id.recycler_matches);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBackToDashboard.setOnClickListener(v -> finish());
    }

    private void setupMatchesList() {
        matchesAdapter = new MatchesAdapter(new ArrayList<>(), userEmail);
        matchesAdapter.setOnMatchClickListener(match -> {
            openChatActivity(match);
        });

        recyclerMatches.setLayoutManager(new LinearLayoutManager(this));
        recyclerMatches.setAdapter(matchesAdapter);
    }

    private void loadAcceptedMatches() {
        userRepository.getAcceptedMatches(userEmail, new UserRepository.AcceptedMatchesCallback() {
            @Override
            public void onSuccess(List<AcceptedMatch> matches) {
                runOnUiThread(() -> {
                    if (matches.isEmpty()) {
                        showEmptyState();
                    } else {
                        // Fetch user details for each match to get actual names
                        fetchUserDetailsForMatches(matches);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("CarpoolMatches", "Error loading accepted matches: " + error);
                runOnUiThread(() -> {
                    showEmptyState();
                });
            }
        });
    }

    private void fetchUserDetailsForMatches(List<AcceptedMatch> matches) {
        List<MatchWithUser> matchesWithUsers = new ArrayList<>();
        int totalMatches = matches.size();
        final int[] completedMatches = {0};

        for (AcceptedMatch match : matches) {
            String otherUserEmail = match.getOtherUserEmail(userEmail);

            userRepository.getUserByEmail(otherUserEmail, new UserRepository.UserFetchCallback() {
                @Override
                public void onSuccess(User user) {
                    matchesWithUsers.add(new MatchWithUser(match, user));
                    completedMatches[0]++;

                    if (completedMatches[0] == totalMatches) {
                        runOnUiThread(() -> {
                            showMatchesListWithUsers(matchesWithUsers);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("CarpoolMatches", "Error fetching user details: " + error);
                    // Still add the match but with default name
                    matchesWithUsers.add(new MatchWithUser(match, null));
                    completedMatches[0]++;

                    if (completedMatches[0] == totalMatches) {
                        runOnUiThread(() -> {
                            showMatchesListWithUsers(matchesWithUsers);
                        });
                    }
                }
            });
        }
    }

    private void showMatchesListWithUsers(List<MatchWithUser> matchesWithUsers) {
        tvEmptyState.setVisibility(View.GONE);
        recyclerMatches.setVisibility(View.VISIBLE);
        matchesAdapter.updateMatchesWithUsers(matchesWithUsers);
    }

    private void showEmptyState() {
        recyclerMatches.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void showMatchesList(List<AcceptedMatch> matches) {
        tvEmptyState.setVisibility(View.GONE);
        recyclerMatches.setVisibility(View.VISIBLE);
        matchesAdapter.updateMatches(matches);
    }

    private void openChatActivity(AcceptedMatch match) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("match_id", match.getId());
        intent.putExtra("user_email", userEmail);
        intent.putExtra("other_user_email", match.getOtherUserEmail(userEmail));
        startActivity(intent);
    }
}