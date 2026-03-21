package com.example.evaccesstry4;

public class Charger {

    private String id;
    private String name;
    private String price;
    private double chargerPower;
    private double lat;
    private double lng;
    private String imageUrl;
    private boolean fastCharger;
    private String category;
    private String hostId;
    private double rating;

    // distance calculated locally (not from Firestore)
    private double distance = -1;

    public Charger() {}

    // Getters
    public String getId() { return id; }
    public double getChargerPower() { return chargerPower; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getImageUrl() { return imageUrl; }
    public boolean isFastCharger() { return fastCharger; }
    public String getCategory() { return category; }
    public String getHostId() { return hostId; }
    public double getDistance() { return distance; }
    public double getRating() { return rating; }


    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(String price) { this.price = price; }
    public void setChargerPower(double chargerPower) { this.chargerPower = chargerPower; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setFastCharger(boolean fastCharger) { this.fastCharger = fastCharger; }
    public void setCategory(String category) { this.category = category; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public void setDistance(double distance) { this.distance = distance; }
    public void setRating(double rating) { this.rating = rating; }
}