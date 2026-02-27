package com.example.evaccesstry4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    // coordinates passed via intent
    private double chargerLat = Double.NaN;
    private double chargerLng = Double.NaN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charger_detail);

        TextView name = findViewById(R.id.detail_name);
        TextView distance = findViewById(R.id.detail_distance);
        TextView price = findViewById(R.id.detail_price);
        TextView status = findViewById(R.id.detail_status);
        Button btnStart = findViewById(R.id.btn_start_session);
        Button btnPay = findViewById(R.id.btn_pay);

        String n = getIntent().getStringExtra(EXTRA_NAME);
        String d = getIntent().getStringExtra(EXTRA_DISTANCE);
        String p = getIntent().getStringExtra(EXTRA_PRICE);
        if (getIntent().hasExtra("extra_lat")) {
            chargerLat = getIntent().getDoubleExtra("extra_lat", Double.NaN);
        }
        if (getIntent().hasExtra("extra_lng")) {
            chargerLng = getIntent().getDoubleExtra("extra_lng", Double.NaN);
        }

        name.setText(n != null ? n : "-");
        distance.setText(d != null ? d : "-");
        price.setText(p != null ? p : "-");

        // Mock availability: available if price exists
        status.setText(p != null ? "Status: Available" : "Status: Unknown");

        // initialize map view
        mapView = findViewById(R.id.map_view);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        btnStart.setOnClickListener(v -> {
            Intent i = new Intent(this, StartSessionActivity.class);
            // pass charger info if needed
            i.putExtra(EXTRA_NAME, n);
            startActivity(i);
        });

        btnPay.setOnClickListener(v -> {
            Intent i = new Intent(this, PaymentActivity.class);
            i.putExtra(EXTRA_NAME, n);
            startActivity(i);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng loc;
        if (!Double.isNaN(chargerLat) && !Double.isNaN(chargerLng)) {
            loc = new LatLng(chargerLat, chargerLng);
        } else {
            // Default location (Kuala Lumpur)
            loc = new LatLng(3.1390, 101.6869);
        }
        googleMap.addMarker(new MarkerOptions().position(loc).title("Charger Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14f));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

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
