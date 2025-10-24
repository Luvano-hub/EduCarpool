package com.educarpool;

public class AcceptedMatch {
    private String id;
    private String passengerEmail;
    private String driverEmail;
    private double distance;
    private int duration;
    private String status;
    private String createdAt;

    public AcceptedMatch() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public String getDriverEmail() { return driverEmail; }
    public void setDriverEmail(String driverEmail) { this.driverEmail = driverEmail; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // Helper method to get the other user's email
    public String getOtherUserEmail(String currentUserEmail) {
        if (currentUserEmail.equals(passengerEmail)) {
            return driverEmail;
        } else {
            return passengerEmail;
        }
    }
}