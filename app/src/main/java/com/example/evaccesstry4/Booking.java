package com.example.evaccesstry4;

public class Booking {

    private String id;             // Firestore document ID
    private String chargerName;    // Name of the charger
    private String price;          // Price per kWh or per hour as string
    private int duration;          // Duration in hours
    private String startTime;      // Start time of booking (formatted as HH:mm)

    private double estimatedCost;  // Estimated cost before charging
    private double totalCost;      // Actual cost after charging

    private String userId;         // User who booked
    private String hostId;         // Host/charger owner
    private String status;         // "BOOKED" or "COMPLETED"
    private long timestamp;        // Booking creation time in millis

    // Empty constructor required for Firestore
    public Booking() {}

    // ----------------
    // Getters
    // ----------------
    public String getId() {
        return id;
    }

    public String getChargerName() {
        return chargerName;
    }

    public String getPrice() {
        return price;
    }

    public int getDuration() {
        return duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public String getUserId() {
        return userId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ----------------
    // Setters
    // ----------------
    public void setId(String id) {
        this.id = id;
    }

    public void setChargerName(String chargerName) {
        this.chargerName = chargerName;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}