package com.example.evaccesstry4;

public class Charger {

    private String id;        // for Firestore doc ID
    private String name;
    private String distance;
    private String price;
    private double lat;
    private double lng;

    // Empty constructor required for Firestore
    public Charger() {}

    // Constructor
    public Charger(String name, String distance, String price, double lat, double lng) {
        this.name = name;
        this.distance = distance;
        this.price = price;
        this.lat = lat;
        this.lng = lng;
    }

    // -------------------
    // Getter methods
    // -------------------
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDistance() { return distance; }
    public String getPrice() { return price; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }

    // -------------------
    // Setter methods
    // -------------------
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDistance(String distance) { this.distance = distance; }
    public void setPrice(String price) { this.price = price; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }

}