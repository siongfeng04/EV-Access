package com.example.evaccesstry4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_LOCATION = 1002;

    private RecyclerView recyclerNearest, recyclerHostServices, recyclerTopChargers;
    private List<TopCharger> topList = new ArrayList<>();
    private TextView textBalance;
    private ImageButton btnQr, btnManageServices;

    private LinearLayout layoutDriver, layoutHost;
    private LinearLayout catHome, catMall, catAirbnb, catOffice;

    private TextView textEarningWeek, textEarningMonth, textEarningYear;

    private UserViewModel userViewModel;
    private FirebaseFirestore db;

    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0;
    private double userLng = 0;

    private PieChart pieChart;
    MaterialCardView cardBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnQr = v.findViewById(R.id.btn_qr_scan);
        btnManageServices = v.findViewById(R.id.btn_manage_services);
        textBalance = v.findViewById(R.id.text_balance);
        cardBalance = v.findViewById(R.id.card_balance);

        layoutDriver = v.findViewById(R.id.layout_driver_section);
        layoutHost = v.findViewById(R.id.layout_host_section);

        recyclerNearest = v.findViewById(R.id.recycler_nearest);
        recyclerHostServices = v.findViewById(R.id.recycler_host_services);

        recyclerTopChargers = v.findViewById(R.id.recycler_top_chargers);
        recyclerTopChargers.setLayoutManager(new LinearLayoutManager(getContext()));

        textEarningWeek = v.findViewById(R.id.text_earning_week);
        textEarningMonth = v.findViewById(R.id.text_earning_month);
        textEarningYear = v.findViewById(R.id.text_earning_year);

        pieChart = v.findViewById(R.id.piechart_revenue_breakdown);

        catHome = v.findViewById(R.id.cat_home);
        catMall = v.findViewById(R.id.cat_mall);
        catAirbnb = v.findViewById(R.id.cat_airbnb);
        catOffice = v.findViewById(R.id.cat_office);

        recyclerNearest.setLayoutManager(
                new LinearLayoutManager(requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false));

        recyclerHostServices.setLayoutManager(
                new LinearLayoutManager(requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false));

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        getUserLocation();

        btnQr.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA);

            } else {
                startScan();
            }
        });

        cardBalance.setOnClickListener(view -> {
            startActivity(new Intent(requireContext(), TopUpActivity.class));
        });

        btnManageServices.setOnClickListener(view -> {
            startActivity(new Intent(requireContext(), ManageServicesActivity.class));
        });

        TextView viewMore = v.findViewById(R.id.text_view_more);
        viewMore.setOnClickListener(view -> {

            Intent intent = new Intent(requireContext(), ViewMoreActivity.class);
            intent.putExtra("category", "all");
            intent.putExtra("title", "All Chargers");

            startActivity(intent);
        });

        catHome.setOnClickListener(v1 -> openCategory("home", "Home Chargers"));
        catMall.setOnClickListener(v1 -> openCategory("mall", "Mall Chargers"));
        catAirbnb.setOnClickListener(v1 -> openCategory("airbnb", "Airbnb Chargers"));
        catOffice.setOnClickListener(v1 -> openCategory("office", "Office Chargers"));

        return v;
    }

    // =============================
    // LOCATION
    // =============================
    private void getUserLocation() {

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                    }

                    loadHomeUI();
                })
                .addOnFailureListener(e -> loadHomeUI());
    }

    private void loadHomeUI() {

        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {

            if (role == null) return;

            if ("host".equalsIgnoreCase(role)) {
                showHostHome();
            } else {
                showUserHome();
            }
        });
    }

    // =============================
    // DISTANCE
    // =============================
    private double calculateDistance(double chargerLat, double chargerLng) {

        if (userLat == 0 || userLng == 0) return -1;

        float[] results = new float[1];

        Location.distanceBetween(
                userLat,
                userLng,
                chargerLat,
                chargerLng,
                results
        );

        return results[0] / 1000.0;
    }

    private void openCategory(String category, String title) {

        Intent intent = new Intent(requireContext(), ViewMoreActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("title", title);

        startActivity(intent);
    }

    // =============================
    // DRIVER HOME (UPDATED)
    // =============================
    private void showUserHome() {

        btnQr.setVisibility(View.VISIBLE);
        btnManageServices.setVisibility(View.GONE);

        layoutDriver.setVisibility(View.VISIBLE);
        layoutHost.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .addSnapshotListener((doc, e) -> {

                    if (doc != null && doc.exists()) {
                        Double wallet = doc.getDouble("wallet");
                        textBalance.setText("RM " + String.format("%.2f", wallet != null ? wallet : 0));
                    }
                });


        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Charger> userChargers = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Charger charger = doc.toObject(Charger.class);
                        if (charger != null) {
                            double lat = charger.getLat();
                            double lng = charger.getLng();
                            double distance = calculateDistance(lat, lng);

                            if (distance >= 0 && distance <= 30) {
                                charger.setId(doc.getId());
                                charger.setDistance(distance);
                                userChargers.add(charger);
                            }
                        }
                    }

                    // ⭐ Fetch ratings from bookings by chargerName
                    db.collection("bookings")
                            .get()
                            .addOnSuccessListener(bookingSnapshots -> {

                                for (Charger charger : userChargers) {
                                    double total = 0;
                                    int count = 0;

                                    for (DocumentSnapshot bookingDoc : bookingSnapshots) {
                                        String cName = bookingDoc.getString("chargerName");
                                        Double rating = bookingDoc.getDouble("rating");

                                        if (cName != null && cName.equals(charger.getName()) && rating != null) {
                                            total += rating;
                                            count++;
                                        }
                                    }

                                    charger.setRating(count > 0 ? total / count : 0);
                                }

                                // Sort by distance
                                userChargers.sort((c1, c2) ->
                                        Double.compare(c1.getDistance(), c2.getDistance()));

                                ChargerAdapter adapter =
                                        new ChargerAdapter(requireContext(), userChargers);
                                recyclerNearest.setAdapter(adapter);
                            });
                });
    }

    // =============================
    // HOST HOME
    // =============================
    private void showHostHome() {

        // -------------------
        // Layout visibility
        // -------------------
        layoutDriver.setVisibility(View.GONE);      // hide driver UI
        layoutHost.setVisibility(View.VISIBLE);     // show host UI
        btnQr.setVisibility(View.GONE);             // hide QR button
        btnManageServices.setVisibility(View.VISIBLE); // show manage services button

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String hostId = user.getUid();

        db.collection("users")
                .document(hostId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        Double wallet = doc.getDouble("wallet");
                        textBalance.setText("Host Balance: RM " +
                                String.format("%.2f", wallet != null ? wallet : 0));
                    }
                });

        // -------------------
        // Load host services
        // -------------------
        db.collection("services")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Charger> hostServices = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Charger charger = doc.toObject(Charger.class);
                        if (charger != null) {
                            charger.setId(doc.getId()); // ⭐ IMPORTANT

                            double distance = calculateDistance(
                                    charger.getLat(),
                                    charger.getLng()
                            );

                            charger.setDistance(distance); // ⭐ ADD THIS

                            hostServices.add(charger);
                        }
                    }

                    // ⭐ ADD THIS (same as driver logic)
                    db.collection("bookings")
                            .get()
                            .addOnSuccessListener(bookingSnapshots -> {

                                for (Charger charger : hostServices) {
                                    double total = 0;
                                    int count = 0;

                                    for (DocumentSnapshot bookingDoc : bookingSnapshots) {
                                        String cName = bookingDoc.getString("chargerName");
                                        Double rating = bookingDoc.getDouble("rating");

                                        if (cName != null &&
                                                cName.equals(charger.getName()) &&
                                                rating != null) {

                                            total += rating;
                                            count++;
                                        }
                                    }

                                    charger.setRating(count > 0 ? total / count : 0);
                                }

                                recyclerHostServices.setAdapter(
                                        new ChargerAdapter(requireContext(), hostServices));
                            });
                });

        // -------------------
        // Load host bookings & calculate earnings
        // -------------------
        db.collection("bookings")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    double weekEarning = 0;
                    double monthEarning = 0;
                    double yearEarning = 0;
                    double totalRating = 0;
                    int ratingCount = 0;

                    long now = System.currentTimeMillis();
                    long oneWeek = 7L * 24 * 60 * 60 * 1000;
                    long oneMonth = 30L * 24 * 60 * 60 * 1000;
                    long oneYear = 365L * 24 * 60 * 60 * 1000;

                    for (DocumentSnapshot doc : querySnapshot) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking == null) continue;

                        long ts = booking.getTimestamp();
                        double totalCost = booking.getTotalCost();

                        // Only count completed bookings
                        if ("COMPLETED".equalsIgnoreCase(booking.getStatus())) {
                            if (now - ts <= oneWeek) weekEarning += totalCost;
                            if (now - ts <= oneMonth) monthEarning += totalCost;
                            if (now - ts <= oneYear) yearEarning += totalCost;
                        }

                        // Ratings
                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            totalRating += rating;
                            ratingCount++;
                        }
                    }

                    textEarningWeek.setText("RM " + String.format("%.2f", weekEarning));
                    textEarningMonth.setText("RM " + String.format("%.2f", monthEarning));
                    textEarningYear.setText("RM " + String.format("%.2f", yearEarning));

                    // Optional: show average rating in a TextView if you add one
                    // TextView textAvgRating = v.findViewById(R.id.text_avg_rating);
                    // textAvgRating.setText("⭐ " + String.format("%.1f", ratingCount > 0 ? totalRating / ratingCount : 0));
                });

        loadTopChargers(hostId);
        loadRevenueBreakdown(hostId);
    }

    // =============================
    // QR
    // =============================
    private void startScan() {

        IntentIntegrator integrator =
                IntentIntegrator.forSupportFragment(this);

        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(CaptureActivity.class);

        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode,
                        resultCode,
                        data);

        if (result != null) {

            String contents = result.getContents();

            if (contents != null) {
                Toast.makeText(requireContext(),
                        "Scanned: " + contents,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // =============================
    // PERMISSIONS
    // =============================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startScan();
            }
        }

        else if (requestCode == REQUEST_LOCATION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getUserLocation();

            } else {
                loadHomeUI();
            }
        }
    }

    private void loadTopChargers(String hostId) {

        db.collection("bookings")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(snapshots -> {

                    HashMap<String, TopCharger> map = new HashMap<>();

                    for (DocumentSnapshot doc : snapshots) {

                        String name = doc.getString("chargerName");
                        Double revenue = doc.getDouble("totalCost");

                        if (name == null) continue;

                        TopCharger existing = map.get(name);

                        if (existing == null) {
                            existing = new TopCharger(name, 0, 0);
                        }

                        int bookings = existing.getTotalBookings() + 1;
                        double total = existing.getTotalRevenue() + (revenue != null ? revenue : 0);

                        map.put(name, new TopCharger(name, bookings, total));
                    }

                    topList.clear();
                    topList.addAll(map.values());

                    // Sort by revenue DESC
                    topList.sort((a, b) ->
                            Double.compare(b.getTotalRevenue(), a.getTotalRevenue()));

                    recyclerTopChargers.setAdapter(new TopChargerAdapter(topList));
                });
    }

    private void loadRevenueBreakdown(String hostId) {

        db.collection("bookings")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(snapshots -> {

                    double home = 0, mall = 0, airbnb = 0, office = 0;

                    for (DocumentSnapshot doc : snapshots) {

                        String category = doc.getString("category");
                        Double revenue = doc.getDouble("totalCost");

                        if (category == null || revenue == null) continue;

                        switch (category.toLowerCase()) {
                            case "home": home += revenue; break;
                            case "mall": mall += revenue; break;
                            case "airbnb": airbnb += revenue; break;
                            case "office": office += revenue; break;
                        }
                    }

                    List<PieEntry> entries = new ArrayList<>();

                    if (home > 0) entries.add(new PieEntry((float) home, "Home"));
                    if (mall > 0) entries.add(new PieEntry((float) mall, "Mall"));
                    if (airbnb > 0) entries.add(new PieEntry((float) airbnb, "Airbnb"));
                    if (office > 0) entries.add(new PieEntry((float) office, "Office"));

                    // ✅ ADD HERE
                    if (entries.isEmpty()) {
                        pieChart.clear();
                        pieChart.setNoDataText("No revenue data");
                        pieChart.invalidate();
                        return;
                    }

                    PieDataSet dataSet = new PieDataSet(entries, "");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                    PieData data = new PieData(dataSet);
                    data.setValueTextSize(12f);

                    pieChart.setData(data);
                    pieChart.setUsePercentValues(true);
                    pieChart.getDescription().setEnabled(false);
                    pieChart.animateY(1000);
                    pieChart.invalidate();
                });
    }
}