package com.educarpool;

import java.util.List;

public class Agreement {
    private String agreementId;
    private String matchId;
    private String proposedBy;
    private String pickupTime;
    private String departureTime;
    private String pickupLocation;
    private String dropoffLocation;
    private double price;
    private List<String> daysOfWeek;
    private String notes;
    private String status; // "proposed", "accepted", "rejected", "active", "expired"
    private String createdAt;
    private String expiresAt;
    private String paymentMethod;
    private String pickupNotes;
    private String tripFrequency;
    private String emergencyContact;
    private int passengerCount;

    public Agreement() {}

    public Agreement(String matchId, String proposedBy, String pickupTime, String departureTime,
                     String pickupLocation, String dropoffLocation, double price,
                     List<String> daysOfWeek, String notes) {
        this.matchId = matchId;
        this.proposedBy = proposedBy;
        this.pickupTime = pickupTime;
        this.departureTime = departureTime;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.price = price;
        this.daysOfWeek = daysOfWeek;
        this.notes = notes;
        this.status = "proposed";
        this.tripFrequency = "One-time";
        this.passengerCount = 1;
    }

    // Getters and setters
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }

    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPickupNotes() { return pickupNotes; }
    public void setPickupNotes(String pickupNotes) { this.pickupNotes = pickupNotes; }

    public String getTripFrequency() { return tripFrequency; }
    public void setTripFrequency(String tripFrequency) { this.tripFrequency = tripFrequency; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
}