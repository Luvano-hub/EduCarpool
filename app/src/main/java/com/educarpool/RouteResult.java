package com.educarpool;

public class RouteResult {
    private String encodedPolyline;
    private double distance;
    private int duration;

    public RouteResult(String encodedPolyline, double distance, int duration) {
        this.encodedPolyline = encodedPolyline;
        this.distance = distance;
        this.duration = duration;
    }

    // Getters and setters
    public String getEncodedPolyline() { return encodedPolyline; }
    public void setEncodedPolyline(String encodedPolyline) { this.encodedPolyline = encodedPolyline; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}