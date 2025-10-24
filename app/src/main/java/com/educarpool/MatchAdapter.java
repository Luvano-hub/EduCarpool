package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.educarpool.R;
import com.educarpool.MatchSummary;

import java.text.SimpleDateFormat;
import java.util.*;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.VH> {

    public interface OnClick {
        void onMatchClick(MatchSummary m);
    }

    private final List<MatchSummary> items = new ArrayList<>();
    private final OnClick listener;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MatchAdapter(OnClick listener) { this.listener = listener; }

    public void submit(List<MatchSummary> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MatchSummary m = items.get(pos);
        h.tvName.setText(m.otherName != null ? m.otherName : "User");
        h.tvPreview.setText(m.lastMessagePreview != null ? m.lastMessagePreview : "Say hi 👋");
        if (m.lastMessageEpochMillis > 0) h.tvTime.setText(timeFmt.format(new Date(m.lastMessageEpochMillis)));
        else h.tvTime.setText("");

        // TODO load avatar if you use Glide/Picasso (m.otherPhotoUrl)

        h.itemView.setOnClickListener(v -> listener.onMatchClick(m));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar; TextView tvName; TextView tvPreview; TextView tvTime;
        VH(@NonNull View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            tvName = v.findViewById(R.id.tvName);
            tvPreview = v.findViewById(R.id.tvPreview);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}

