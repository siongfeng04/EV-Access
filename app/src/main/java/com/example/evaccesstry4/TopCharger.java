package com.example.evaccesstry4;

public class TopCharger {
    private String name;
    private int totalBookings;
    private double totalRevenue;

    public TopCharger(String name, int totalBookings, double totalRevenue) {
        this.name = name;
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
    }

    public String getName() { return name; }
    public int getTotalBookings() { return totalBookings; }
    public double getTotalRevenue() { return totalRevenue; }
}
