package com.educarpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {

    private List<Driver> drivers;
    private OnDriverClickListener listener;

    public DriverAdapter(List<Driver> drivers) {
        this.drivers = drivers;
    }

    public void setOnDriverClickListener(OnDriverClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = drivers.get(position);
        holder.bind(driver);

        // Set click listener for the request button
        holder.getSendRequestButton().setOnClickListener(v -> {
            if (listener != null) {
                listener.onDriverClick(driver);
            }
        });
    }

    @Override
    public int getItemCount() {
        return drivers.size();
    }

    public void updateDrivers(List<Driver> drivers) {
        this.drivers = drivers;
        notifyDataSetChanged();
    }

    // Get driver by position
    public Driver getDriverAtPosition(int position) {
        if (position >= 0 && position < drivers.size()) {
            return drivers.get(position);
        }
        return null;
    }

    // Find position by driver ID
    public int findPositionByDriverId(String driverId) {
        for (int i = 0; i < drivers.size(); i++) {
            if (drivers.get(i).getId().equals(driverId)) {
                return i;
            }
        }
        return -1;
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDriverName, tvRating, tvDistance, tvDuration;
        private ImageView ivVerified;
        private com.google.android.material.button.MaterialButton btnSendRequest;

        DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tv_driver_name);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            ivVerified = itemView.findViewById(R.id.iv_verified);
            btnSendRequest = itemView.findViewById(R.id.btn_send_request);
        }

        void bind(Driver driver) {
            tvDriverName.setText(driver.getName());
            tvRating.setText(String.format("%.1f", driver.getRating()));
            tvDistance.setText(String.format("%.1f km away", driver.getDistance()));
            tvDuration.setText(driver.getDuration() + " min");

            if (driver.isVerified()) {
                ivVerified.setVisibility(View.VISIBLE);
            } else {
                ivVerified.setVisibility(View.GONE);
            }
        }

        // Public getter for the send request button
        public com.google.android.material.button.MaterialButton getSendRequestButton() {
            return btnSendRequest;
        }

        // Method to update button state
        public void setButtonEnabled(boolean enabled) {
            btnSendRequest.setEnabled(enabled);
            btnSendRequest.setText(enabled ? "Send Request" : "Sending...");
        }
    }

    public interface OnDriverClickListener {
        void onDriverClick(Driver driver);
    }
}