package com.educarpool;

import java.util.UUID;

public class ChatMessage {
    public UUID messageId;
    public UUID matchId;
    public UUID senderId;
    public UUID receiverId;
    public String messageText;
    public long timestampMillis;

    public boolean isMine(UUID myId) { return myId != null && myId.equals(senderId); }
}

