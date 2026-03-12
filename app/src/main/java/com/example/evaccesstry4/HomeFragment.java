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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_LOCATION = 1002;

    private RecyclerView recyclerNearest, recyclerHostServices;
    private TextView textBalance;
    private ImageButton btnQr, btnManageServices;

    private LinearLayout layoutDriver, layoutHost;
    private LinearLayout catHome, catMall, catAirbnb, catOffice;

    private TextView textEarningWeek, textEarningMonth, textEarningYear;

    private UserViewModel userViewModel;
    private FirebaseFirestore db;

    // LOCATION
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0;
    private double userLng = 0;

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

        layoutDriver = v.findViewById(R.id.layout_driver_section);
        layoutHost = v.findViewById(R.id.layout_host_section);

        recyclerNearest = v.findViewById(R.id.recycler_nearest);
        recyclerHostServices = v.findViewById(R.id.recycler_host_services);

        textEarningWeek = v.findViewById(R.id.text_earning_week);
        textEarningMonth = v.findViewById(R.id.text_earning_month);
        textEarningYear = v.findViewById(R.id.text_earning_year);

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

        // QR Scan
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

        // Manage Services
        btnManageServices.setOnClickListener(view -> {
            startActivity(new Intent(requireContext(), ManageServicesActivity.class));
        });

        // View More
        TextView viewMore = v.findViewById(R.id.text_view_more);
        viewMore.setOnClickListener(view -> {

            Intent intent = new Intent(requireContext(), ViewMoreActivity.class);
            intent.putExtra("category", "all");
            intent.putExtra("title", "All Chargers");

            startActivity(intent);
        });

        // Category filters
        catHome.setOnClickListener(v1 -> openCategory("home", "Home Chargers"));
        catMall.setOnClickListener(v1 -> openCategory("mall", "Mall Chargers"));
        catAirbnb.setOnClickListener(v1 -> openCategory("airbnb", "Airbnb Chargers"));
        catOffice.setOnClickListener(v1 -> openCategory("office", "Office Chargers"));

        return v;
    }

    // =============================
    // GET USER LOCATION
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

    // =============================
    // LOAD UI
    // =============================
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
    // DISTANCE CALCULATION
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

    // =============================
    // CATEGORY
    // =============================
    private void openCategory(String category, String title) {

        Intent intent = new Intent(requireContext(), ViewMoreActivity.class);

        intent.putExtra("category", category);
        intent.putExtra("title", title);

        startActivity(intent);
    }

    // =============================
    // DRIVER HOME
    // =============================
    private void showUserHome() {

        btnQr.setVisibility(View.VISIBLE);
        btnManageServices.setVisibility(View.GONE);

        layoutDriver.setVisibility(View.VISIBLE);
        layoutHost.setVisibility(View.GONE);

        textBalance.setText("RM 120.50");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

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

                            charger.setDistance(distance);

                            // Only chargers within 30 km
                            if (distance >= 0 && distance <= 30) {

                                charger.setId(doc.getId());
                                userChargers.add(charger);
                            }
                        }
                    }

                    // Sort nearest first
                    userChargers.sort((c1, c2) ->
                            Double.compare(c1.getDistance(), c2.getDistance()));

                    ChargerAdapter adapter =
                            new ChargerAdapter(requireContext(), userChargers);

                    recyclerNearest.setAdapter(adapter);

                    if (userChargers.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "No chargers within 30km",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load chargers",
                                Toast.LENGTH_SHORT).show());
    }

    // =============================
    // HOST HOME
    // =============================
    private void showHostHome() {

        layoutDriver.setVisibility(View.GONE);

        btnQr.setVisibility(View.GONE);
        btnManageServices.setVisibility(View.VISIBLE);

        layoutHost.setVisibility(View.VISIBLE);

        textBalance.setText("Host Balance: RM 0.00");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("services")
                .whereEqualTo("hostId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Charger> hostServices = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

                        Charger charger = doc.toObject(Charger.class);

                        if (charger != null) {
                            hostServices.add(charger);
                        }
                    }

                    ChargerAdapter adapter =
                            new ChargerAdapter(requireContext(), hostServices);

                    recyclerHostServices.setAdapter(adapter);
                });

        db.collection("earnings")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        Double week = doc.getDouble("week");
                        Double month = doc.getDouble("month");
                        Double year = doc.getDouble("year");

                        textEarningWeek.setText("RM " + (week != null ? week : 0));
                        textEarningMonth.setText("RM " + (month != null ? month : 0));
                        textEarningYear.setText("RM " + (year != null ? year : 0));
                    }
                });
    }

    // =============================
    // QR SCAN
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

            } else {

                Toast.makeText(requireContext(),
                        "Cancelled",
                        Toast.LENGTH_SHORT).show();
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

            } else {

                Toast.makeText(requireContext(),
                        "Camera permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }

        else if (requestCode == REQUEST_LOCATION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getUserLocation();

            } else {

                Toast.makeText(requireContext(),
                        "Location permission required",
                        Toast.LENGTH_SHORT).show();

                loadHomeUI();
            }
        }
    }
}