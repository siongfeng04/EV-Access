package com.example.evaccesstry4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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

public class ChargerDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";
    public static final String EXTRA_HOST_ID = "extra_host_id";

    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private double chargerLat = Double.NaN;
    private double chargerLng = Double.NaN;

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

        Button btnBook = findViewById(R.id.btn_book);
        Button btnStart = findViewById(R.id.btn_start_session);
        Button btnPay = findViewById(R.id.btn_pay);
        TextView backBtn = findViewById(R.id.backBtn);

        // Receive data from intent
        chargerName = getIntent().getStringExtra(EXTRA_NAME);
        chargerHostId = getIntent().getStringExtra(EXTRA_HOST_ID);
        String chargerPrice = getIntent().getStringExtra(EXTRA_PRICE);

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

        // BOOK BUTTON

        btnBook.setOnClickListener(v -> {
            BookingDialogFragment bookingDialog = new BookingDialogFragment(
                    chargerName,   // from your activity
                    chargerPrice,   // from your activity
                    chargerHostId
            );
            bookingDialog.show(getSupportFragmentManager(), "BookingDialog");
        });


        // START SESSION
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartSessionActivity.class);
            intent.putExtra(EXTRA_NAME, chargerName);
            startActivity(intent);
        });

        // PAYMENT
        btnPay.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(EXTRA_NAME, chargerName);
            startActivity(intent);
        });
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
}