package com.example.evaccesstry4;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int REQUEST_CAMERA = 1001;
    private RecyclerView recyclerNearest;
    private TextView textBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        ImageButton btnQr = v.findViewById(R.id.btn_qr_scan);
        textBalance = v.findViewById(R.id.text_balance);
        TextView viewMore = v.findViewById(R.id.text_view_more);
        recyclerNearest = v.findViewById(R.id.recycler_nearest);

        // QR scan
        btnQr.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                startScan();
            }
        });

        // Set balance (could be from ViewModel / prefs)
        textBalance.setText("RM 120.50");

        // Setup recycler
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerNearest.setLayoutManager(lm);

        List<Charger> sample = new ArrayList<>();
        // Sample coordinates near Kuala Lumpur
        sample.add(new Charger("Home A", "0.3 km", "RM 0.50/kWh", 3.1390, 101.6869));
        sample.add(new Charger("Home B", "0.8 km", "RM 0.45/kWh", 3.1405, 101.6880));
        sample.add(new Charger("Home C", "1.2 km", "RM 0.60/kWh", 3.1375, 101.6850));

        ChargerAdapter adapter = new ChargerAdapter(requireContext(), sample);
        recyclerNearest.setAdapter(adapter);

        viewMore.setOnClickListener(view -> startActivity(new Intent(requireContext(), ViewMoreActivity.class)));

        return v;
    }

    private void startScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(CaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String contents = result.getContents();
            if (contents != null) {
                // Handle scanned QR content — for example, if QR contains charger id or URL
                Toast.makeText(requireContext(), "Scanned: " + contents, Toast.LENGTH_LONG).show();
                // TODO: parse contents and act (e.g., open ChargerDetailActivity)
            } else {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
