package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {

    private List<MatchWithUser> matchesWithUsers;
    private String currentUserEmail;
    private OnMatchClickListener listener;

    public MatchesAdapter(List<AcceptedMatch> matches, String currentUserEmail) {
        this.matchesWithUsers = new ArrayList<>();
        this.currentUserEmail = currentUserEmail;
    }

    public void setOnMatchClickListener(OnMatchClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        MatchWithUser matchWithUser = matchesWithUsers.get(position);
        holder.bind(matchWithUser, currentUserEmail); // Pass currentUserEmail as parameter

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMatchClick(matchWithUser.match);
            }
        });
    }

    @Override
    public int getItemCount() {
        return matchesWithUsers.size();
    }

    public void updateMatches(List<AcceptedMatch> matches) {
        this.matchesWithUsers.clear();
        for (AcceptedMatch match : matches) {
            this.matchesWithUsers.add(new MatchWithUser(match, null));
        }
        notifyDataSetChanged();
    }

    public void updateMatchesWithUsers(List<MatchWithUser> matchesWithUsers) {
        this.matchesWithUsers = matchesWithUsers;
        notifyDataSetChanged();
    }

    // Remove static modifier and pass currentUserEmail as parameter
    class MatchViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvUserName, tvLastMessage, tvTimestamp;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        void bind(MatchWithUser matchWithUser, String currentUserEmail) {
            if (matchWithUser.user != null) {
                // Show actual user name
                tvUserName.setText(matchWithUser.user.getName());
            } else {
                // Fallback to email or generic name
                String otherUserEmail = matchWithUser.match.getOtherUserEmail(currentUserEmail);
                tvUserName.setText(otherUserEmail.split("@")[0]); // Show username part of email
            }

            tvLastMessage.setText("Tap to start chatting");
            tvTimestamp.setText("Now");

            // Set default profile picture
            ivProfile.setImageResource(R.drawable.ic_person);
        }
    }

    public interface OnMatchClickListener {
        void onMatchClick(AcceptedMatch match);
    }
}