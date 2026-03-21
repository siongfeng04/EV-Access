package com.example.evaccesstry4;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.pm.PackageManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewMoreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChargerAdapter adapter;

    private List<Charger> chargerList;
    private List<Charger> filteredList;

    private TextInputEditText searchEditText;
    private Chip chipCheap, chipFast;

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0;
    private double userLng = 0;

    private String category = "all";
    private String title = "All Chargers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_more);

        if (getIntent() != null) {
            String cat = getIntent().getStringExtra("category");
            String t = getIntent().getStringExtra("title");
            if (cat != null) category = cat;
            if (t != null) title = t;
        }

        setTitle(title);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        searchEditText = findViewById(R.id.searchEditText);
        ImageButton backBtn = findViewById(R.id.backBtn);


        recyclerView = findViewById(R.id.stationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        chipCheap = findViewById(R.id.chipCheap);
        chipFast = findViewById(R.id.chipFast);

        chargerList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new ChargerAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        getUserLocation();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipCheap.setOnClickListener(v -> applyFilters());
        chipFast.setOnClickListener(v -> applyFilters());
        backBtn.setOnClickListener(v -> finish());
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            loadChargersFromFirestore();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                    }
                    loadChargersFromFirestore();
                })
                .addOnFailureListener(e -> loadChargersFromFirestore());
    }

    private void loadChargersFromFirestore() {
        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chargerList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Charger charger = doc.toObject(Charger.class);
                        if (charger != null &&
                                ("all".equalsIgnoreCase(category) ||
                                        category.equalsIgnoreCase(charger.getCategory()))) {

                            // 🔹 Calculate distance from user
                            float[] results = new float[1];
                            Location.distanceBetween(userLat, userLng,
                                    charger.getLat(), charger.getLng(), results);
                            double distanceKm = results[0] / 1000f;
                            charger.setDistance(distanceKm);

                            chargerList.add(charger);
                        }
                    }

                    // ⭐ Fetch ratings from bookings
                    db.collection("bookings")
                            .get()
                            .addOnSuccessListener(bookingSnapshots -> {

                                for (Charger charger : chargerList) {
                                    double total = 0;
                                    int count = 0;

                                    for (DocumentSnapshot bookingDoc : bookingSnapshots) {
                                        String cName = bookingDoc.getString("ChargerName");
                                        Double rating = bookingDoc.getDouble("rating");

                                        if (cName != null && cName.equals(charger.getName()) && rating != null) {
                                            total += rating;
                                            count++;
                                        }
                                    }

                                    charger.setRating(count > 0 ? total / count : 0);
                                }

                                applyFilters(); // Apply search/filter and notify adapter
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to fetch ratings", Toast.LENGTH_SHORT).show();
                                applyFilters(); // still show chargers even if ratings fail
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load chargers", Toast.LENGTH_SHORT).show());
    }

    private void applyFilters() {
        String searchText = searchEditText.getText().toString().trim().toLowerCase();
        boolean filterCheap = chipCheap.isChecked();
        boolean filterFast = chipFast.isChecked();

        filteredList.clear();

        for (Charger charger : chargerList) {
            boolean matchesSearch = charger.getName().toLowerCase().contains(searchText);
            boolean matchesCheap = true;
            boolean matchesFast = true;

            if (filterCheap) {
                try {
                    double price = Double.parseDouble(charger.getPrice().replaceAll("[^0-9.]", ""));
                    matchesCheap = price <= 1.0;
                } catch (Exception e) {
                    matchesCheap = false;
                }
            }

            if (filterFast) matchesFast = charger.isFastCharger();

            if (matchesSearch && matchesCheap && matchesFast) filteredList.add(charger);
        }

        // Sort by distance ascending
        Collections.sort(filteredList, Comparator.comparingDouble(Charger::getDistance));

        adapter.notifyDataSetChanged();
    }
}