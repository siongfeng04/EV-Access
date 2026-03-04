package com.example.evaccesstry4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.google.android.material.card.MaterialCardView;
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

    // UI elements
    private RecyclerView recyclerNearest, recyclerHostServices;
    private TextView textBalance;
    private ImageButton btnQr, btnManageServices;
    private LinearLayout layoutDriver, layoutHost;
    private TextView textEarningWeek, textEarningMonth, textEarningYear;

    private UserViewModel userViewModel;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Find views
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

        // Setup recyclers
        recyclerNearest.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        recyclerHostServices.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));

        // QR scan button for driver
        btnQr.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                startScan();
            }
        });

        // Manage services button for host
        btnManageServices.setOnClickListener(view -> {
            // Open Manage Services activity
            startActivity(new Intent(requireContext(), ManageServicesActivity.class));
        });

        // Initialize ViewModel
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Observe role changes
        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
            if (role == null) return;

            if ("host".equalsIgnoreCase(role)) {
                showHostHome();
            } else {
                showUserHome();
            }
        });

        return v;
    }

    // -------------------------
    // Show driver UI
    // -------------------------
    private void showUserHome() {
        layoutDriver.setVisibility(View.VISIBLE);
        layoutHost.setVisibility(View.GONE);

        textBalance.setText("RM 120.50");

        List<Charger> userChargers = new ArrayList<>();
        userChargers.add(new Charger("Home A", "0.3 km", "RM 0.50/kWh", 3.1390, 101.6869));
        userChargers.add(new Charger("Home B", "0.8 km", "RM 0.45/kWh", 3.1405, 101.6880));
        userChargers.add(new Charger("Home C", "1.2 km", "RM 0.60/kWh", 3.1375, 101.6850));

        ChargerAdapter adapter = new ChargerAdapter(requireContext(), userChargers);
        recyclerNearest.setAdapter(adapter);
    }

    // -------------------------
    // Show host UI
    // -------------------------
    private void showHostHome() {
        layoutDriver.setVisibility(View.GONE);
        layoutHost.setVisibility(View.VISIBLE);

        textBalance.setText("Host Balance: RM 0.00");

        // Load host services from Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("services")
                    .whereEqualTo("hostId", user.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Charger> hostServices = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Charger charger = doc.toObject(Charger.class);
                            hostServices.add(charger);
                        }
                        ChargerAdapter adapter = new ChargerAdapter(requireContext(), hostServices);
                        recyclerHostServices.setAdapter(adapter);
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(),
                            "Failed to load services", Toast.LENGTH_SHORT).show());

            // Load earnings
            db.collection("earnings")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            textEarningWeek.setText("RM " + doc.getDouble("week"));
                            textEarningMonth.setText("RM " + doc.getDouble("month"));
                            textEarningYear.setText("RM " + doc.getDouble("year"));
                        }
                    });
        }
    }

    // -------------------------
    // QR Scanner
    // -------------------------
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
                Toast.makeText(requireContext(), "Scanned: " + contents, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(requireContext(),
                        "Camera permission is required to scan QR codes",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}