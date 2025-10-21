package com.educarpool;

public class Driver {
    private String id;
    private String name;
    private double rating;
    private double distance;
    private int duration;
    private boolean verified;
    private double latitude;
    private double longitude;

    public Driver() {}

    public Driver(String id, String name, double rating, double distance, int duration, boolean verified) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.distance = distance;
        this.duration = duration;
        this.verified = verified;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}