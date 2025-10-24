package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String currentUserEmail;

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public MessagesAdapter(List<Message> messages, String currentUserEmail) {
        this.messages = messages;
        this.currentUserEmail = currentUserEmail;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserEmail)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    public void updateMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage, tvTimestamp;
        private int viewType;

        MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessageText());

            // Format timestamp - FIXED: Better timestamp handling
            try {
                // Try to parse the timestamp string
                SimpleDateFormat inputFormat;
                if (message.getTimestamp().contains("+")) {
                    // ISO format with timezone
                    inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                } else {
                    // Default format
                    inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
                }

                Date date = inputFormat.parse(message.getTimestamp());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.US);
                String formattedTime = outputFormat.format(date);
                tvTimestamp.setText(formattedTime);
            } catch (Exception e) {
                // If parsing fails, show "Now" or the raw timestamp
                tvTimestamp.setText("Now");
            }
        }
    }
}