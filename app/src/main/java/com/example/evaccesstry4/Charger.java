package com.example.evaccesstry4;

public class Charger {
    public String name;
    public String distance;
    public String price;
    public double lat;
    public double lng;

    // New constructor with coordinates
    public Charger(String name, String distance, String price, double lat, double lng) {
        this.name = name;
        this.distance = distance;
        this.price = price;
        this.lat = lat;
        this.lng = lng;
    }

    // Legacy constructor (keeps compatibility) — sets coordinates to NaN
    public Charger(String name, String distance, String price) {
        this(name, distance, price, Double.NaN, Double.NaN);
    }
}
