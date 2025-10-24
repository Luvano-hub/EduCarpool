package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<RideRequest> requests;
    private OnRequestActionListener listener;

    public RequestAdapter(List<RideRequest> requests) {
        this.requests = requests;
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        holder.bind(request);

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(request);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(request);
            }
        });

        // Optional: Show passenger location on map when clicked
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowOnMap(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<RideRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPassengerName, tvDistance, tvDuration, tvAddress;
        private ImageView ivVerified;
        private com.google.android.material.button.MaterialButton btnAccept, btnDecline;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tv_passenger_name);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvAddress = itemView.findViewById(R.id.tv_address);
            ivVerified = itemView.findViewById(R.id.iv_verified);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }

        void bind(RideRequest request) {
            tvPassengerName.setText(request.getPassengerName());
            tvDistance.setText(String.format("%.1f km away", request.getDistance()));
            tvDuration.setText(request.getDuration() + " min");

            if (request.getPassengerHomeAddress() != null) {
                tvAddress.setText(request.getPassengerHomeAddress());
                tvAddress.setVisibility(View.VISIBLE);
            } else {
                tvAddress.setVisibility(View.GONE);
            }

            // For now, show verified icon for all passengers
            // Later you can add verification status to users
            ivVerified.setVisibility(View.VISIBLE);
        }
    }

    public interface OnRequestActionListener {
        void onAccept(RideRequest request);
        void onDecline(RideRequest request);
        void onShowOnMap(RideRequest request);
    }
}