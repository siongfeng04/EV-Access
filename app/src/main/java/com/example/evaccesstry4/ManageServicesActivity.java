package com.example.evaccesstry4;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageServicesActivity extends AppCompatActivity {

    private RecyclerView recyclerHostServices;
    private Button btnAddService;

    private FirebaseFirestore db;
    private List<Charger> hostServices;
    private ChargerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Find views
        recyclerHostServices = findViewById(R.id.recycler_host_services);
        btnAddService = findViewById(R.id.btn_add_service);

        // Setup RecyclerView
        hostServices = new ArrayList<>();
        adapter = new ChargerAdapter(this, hostServices);
        recyclerHostServices.setLayoutManager(new LinearLayoutManager(this));
        recyclerHostServices.setAdapter(adapter);

        // Load host services from Firestore
        loadHostServices();

        // Add service button
        btnAddService.setOnClickListener(view -> {
            // TODO: open AddServiceActivity to create new service
            Toast.makeText(this, "Open Add Service Page", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadHostServices() {
        String hostId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("services")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hostServices.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Charger charger = doc.toObject(Charger.class);
                        charger.setId(doc.getId()); // store docId for update/delete
                        hostServices.add(charger);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load services", Toast.LENGTH_SHORT).show());
    }
}