package com.example.evaccesstry4;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ViewMoreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChargerAdapter adapter;

    private List<Charger> chargerList;      // original data
    private List<Charger> filteredList;     // filtered results

    private TextInputEditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_more);

        // 🔍 Search bar
        searchEditText = findViewById(R.id.searchEditText);

        // 📋 RecyclerView
        recyclerView = findViewById(R.id.stationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 📦 Initialize lists
        chargerList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // ⚡ Sample chargers (replace with Firebase later)
        loadSampleData();

        // 🔗 Adapter uses filtered list
        adapter = new ChargerAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        // 🔍 Enable search filtering
        setupSearch();
    }

    // ===============================
    // 🔋 Sample Data
    // ===============================
    private void loadSampleData() {

        chargerList.add(new Charger(
                "Home Charger - Ahmad",
                "2.3 km away",
                "RM 0.80 / kWh",
                4.5975, 114.0769
        ));

        chargerList.add(new Charger(
                "Fast DC Station",
                "1.1 km away",
                "RM 1.20 / kWh",
                4.5990, 114.0780
        ));

        chargerList.add(new Charger(
                "Condo Charger",
                "3.8 km away",
                "RM 0.65 / kWh",
                4.5932, 114.0721
        ));

        chargerList.add(new Charger(
                "Mall Charging Hub",
                "5.0 km away",
                "RM 1.00 / kWh",
                4.6021, 114.0825
        ));

        // initially show all items
        filteredList.addAll(chargerList);
    }

    // ===============================
    // 🔎 Search Logic
    // ===============================
    private void setupSearch() {

        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {

        filteredList.clear();

        if (text.isEmpty()) {
            filteredList.addAll(chargerList);
        } else {
            for (Charger charger : chargerList) {

                if (charger.getName().toLowerCase()
                        .contains(text.toLowerCase())) {

                    filteredList.add(charger);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}
