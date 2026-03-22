package com.example.evaccesstry4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChargerDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";
    public static final String EXTRA_HOST_ID = "extra_host_id";
    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_POWER = "extra_power";



    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private double chargerLat = Double.NaN;
    private double chargerLng = Double.NaN;

    private Button btnBook, btnDelete;
    private String currentUserId;

    private String chargerName;
    private String chargerHostId;

    private FusedLocationProviderClient fusedLocationClient;

    private TextView distanceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charger_detail);

        TextView name = findViewById(R.id.detail_name);
        distanceText = findViewById(R.id.detail_distance);
        TextView price = findViewById(R.id.detail_price);
        TextView status = findViewById(R.id.detail_status);
        TextView hostName = findViewById(R.id.detail_host_name);
        TextView hostPhone = findViewById(R.id.detail_host_phone);

        Button btnBook = findViewById(R.id.btn_book);
        Button btnDelete = findViewById(R.id.btn_delete);

        TextView backBtn = findViewById(R.id.backBtn);

        // Receive data from intent
        chargerName = getIntent().getStringExtra(EXTRA_NAME);
        chargerHostId = getIntent().getStringExtra(EXTRA_HOST_ID);
        String chargerPrice = getIntent().getStringExtra(EXTRA_PRICE);
        String chargerID = getIntent().getStringExtra(EXTRA_ID);
        double chargerPower = getIntent().getDoubleExtra(EXTRA_POWER, Double.NaN);

        chargerLat = getIntent().getDoubleExtra(EXTRA_LAT, Double.NaN);
        chargerLng = getIntent().getDoubleExtra(EXTRA_LNG, Double.NaN);

        // Set text values
        name.setText(chargerName != null ? chargerName : "Unknown Charger");
        price.setText(chargerPrice != null ? chargerPrice : "Price unavailable");

        // Status
        if (chargerPrice != null) {
            status.setText("Status: Available");
            status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            status.setText("Status: Not Available");
            status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(chargerHostId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String host_name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");

                        hostName.setText("Host: " + (host_name != null ? host_name : "N/A"));
                        hostPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
                    }
                })
                .addOnFailureListener(e -> {
                    hostName.setText("Host: N/A");
                    hostPhone.setText("Phone: N/A");
                });

        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getUserLocation();

        // Initialize MapView
        mapView = findViewById(R.id.map_view);
        Bundle mapViewBundle = null;

        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        backBtn.setOnClickListener(v -> finish());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

// Check if current user is host (owner of charger)
        if (currentUserId.equals(chargerHostId)) {

            btnBook.setText("Edit Charger"); // Change text

            btnBook.setOnClickListener(v -> openEditDialog());

            btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(ChargerDetailActivity.this)
                        .setTitle("Delete Charger")
                        .setMessage("Are you sure you want to delete this charger? This action cannot be undone.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            deleteCharger(); // call your existing delete function
                        })
                        .setNegativeButton("No", null) // just dismiss
                        .show();
            });

        } else {
            // Keep existing booking functionality
            btnBook.setText("Book Charger");
            btnDelete.setVisibility(View.GONE);

            btnBook.setOnClickListener(v -> {
                BookingDialogFragment bookingDialog = new BookingDialogFragment(
                        chargerName,
                        getIntent().getStringExtra(EXTRA_PRICE),
                        chargerHostId,
                        getIntent().getStringExtra(EXTRA_ID),
                        getIntent().getDoubleExtra(EXTRA_POWER, Double.NaN)
                );
                bookingDialog.show(getSupportFragmentManager(), "BookingDialog");
            });
        }

    }

    private void getUserLocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);

            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {

            if (location != null && !Double.isNaN(chargerLat) && !Double.isNaN(chargerLng)) {

                float[] results = new float[1];

                Location.distanceBetween(
                        location.getLatitude(),
                        location.getLongitude(),
                        chargerLat,
                        chargerLng,
                        results
                );

                float distanceKm = results[0] / 1000;

                distanceText.setText(String.format("%.2f km away", distanceKm));

            } else {
                distanceText.setText("Distance unavailable");
            }

        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng location;

        if (!Double.isNaN(chargerLat) && !Double.isNaN(chargerLng)) {
            location = new LatLng(chargerLat, chargerLng);
        } else {
            location = new LatLng(3.1390, 101.6869);
        }

        googleMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .title(chargerName != null ? chargerName : "EV Charger")
        );

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
    }

    // Map lifecycle
    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    protected void onPause() { mapView.onPause(); super.onPause(); }

    @Override
    protected void onStop() { mapView.onStop(); super.onStop(); }

    @Override
    protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }

    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);

        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    private void openEditDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_charger, null);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editPrice = dialogView.findViewById(R.id.edit_price);

        // pre-fill existing data
        editName.setText(chargerName);
        editPrice.setText(getIntent().getStringExtra(EXTRA_PRICE));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Charger")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = editName.getText().toString();
                    String newPrice = editPrice.getText().toString();
                    updateCharger(newName, newPrice);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void updateCharger(String name, String price) {
        String chargerId = getIntent().getStringExtra(EXTRA_ID);

        FirebaseFirestore.getInstance()
                .collection("services")
                .document(chargerId)
                .update(
                        "name", name,
                        "price", price
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Charger updated!", Toast.LENGTH_SHORT).show();
                    finish(); // refresh activity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteCharger() {
        String chargerId = getIntent().getStringExtra(EXTRA_ID);

        FirebaseFirestore.getInstance()
                .collection("services")
                .document(chargerId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Charger deleted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

}