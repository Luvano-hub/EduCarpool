package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.educarpool.R;
import com.educarpool.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int OUT = 1, IN = 2;
    private final List<ChatMessage> items = new ArrayList<>();
    private final UUID myId;
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(UUID myId) { this.myId = myId; }

    public void submit(List<ChatMessage> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return items.get(position).isMine(myId) ? OUT : IN;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        int layout = (t == OUT) ? R.layout.item_message_out : R.layout.item_message_in;
        View v = LayoutInflater.from(p.getContext()).inflate(layout, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        ChatMessage m = items.get(pos);
        VH v = (VH) h;
        v.tvText.setText(m.messageText);
        v.tvTime.setText(fmt.format(new Date(m.timestampMillis)));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        VH(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tvText);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}

