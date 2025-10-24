package com.educarpool;

import java.util.UUID;

public class User {
    private UUID id;
    private String email;
    private String password;
    private String name;
    private String studentId;
    private String phone;
    private String homeAddress;
    private Double latitude;
    private Double longitude;
    private String role;
    private Integer detourRange;
    private String licenseUrl;
    private String carRegUrl;
    private Boolean verified;
    private Double rating;
    private String createdAt;
    private String updatedAt;

    // Constructors
    public User() {}

    public User(String email, String password, String name, String studentId, String phone,
                String homeAddress, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.studentId = studentId;
        this.phone = phone;
        this.homeAddress = homeAddress;
        this.role = role;
        this.verified = false; // Default to false, will be set based on role
        this.rating = 5.0; // Default rating
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getDetourRange() { return detourRange; }
    public void setDetourRange(Integer detourRange) { this.detourRange = detourRange; }

    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }

    public String getCarRegUrl() { return carRegUrl; }
    public void setCarRegUrl(String carRegUrl) { this.carRegUrl = carRegUrl; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Helper method to check if coordinates exist
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", studentId='" + studentId + '\'' +
                ", role='" + role + '\'' +
                ", verified=" + verified +
                ", rating=" + rating +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}