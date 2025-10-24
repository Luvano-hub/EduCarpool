package com.educarpool;

import java.util.UUID;

public class MatchSummary {
    public UUID matchId;
    public UUID otherUserId;
    public String otherName;
    public String otherPhotoUrl; // optional
    public String lastMessagePreview;
    public long lastMessageEpochMillis;

    public MatchSummary(UUID matchId, UUID otherUserId, String otherName, String otherPhotoUrl,
                        String lastMessagePreview, long lastMessageEpochMillis) {
        this.matchId = matchId;
        this.otherUserId = otherUserId;
        this.otherName = otherName;
        this.otherPhotoUrl = otherPhotoUrl;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageEpochMillis = lastMessageEpochMillis;
    }
}

