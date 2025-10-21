package com.educarpool;

public class RideRequest {
    private String id;
    private String passengerEmail;
    private String passengerName;
    private String driverEmail;
    private double distance;
    private int duration;
    private String status; // "pending", "accepted", "declined"
    private String createdAt;
    private Double passengerLat;
    private Double passengerLng;
    private String passengerHomeAddress;

    public RideRequest() {}

    public RideRequest(String passengerEmail, String passengerName, String driverEmail,
                       double distance, int duration, String status) {
        this.passengerEmail = passengerEmail;
        this.passengerName = passengerName;
        this.driverEmail = driverEmail;
        this.distance = distance;
        this.duration = duration;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

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

    public Double getPassengerLat() { return passengerLat; }
    public void setPassengerLat(Double passengerLat) { this.passengerLat = passengerLat; }

    public Double getPassengerLng() { return passengerLng; }
    public void setPassengerLng(Double passengerLng) { this.passengerLng = passengerLng; }

    public String getPassengerHomeAddress() { return passengerHomeAddress; }
    public void setPassengerHomeAddress(String passengerHomeAddress) { this.passengerHomeAddress = passengerHomeAddress; }
}