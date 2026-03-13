package com.example.evaccesstry4;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private TextView textChargerName, textPrice, textStartTime, textEstimatedCost;
    private RadioGroup radioGroupBookingType;
    private LinearLayout layoutTimePicker;
    private EditText inputDuration;
    private Button btnConfirmBooking;

    private double chargerPricePerKWh = 1.5; // Example, parse from charger object later
    private int durationHours = 1;
    private boolean isBookNow = true;
    private int startHour = -1, startMinute = -1;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_booking);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind views
        textChargerName = findViewById(R.id.textChargerName);
        textPrice = findViewById(R.id.textPrice);
        textStartTime = findViewById(R.id.textStartTime);
        textEstimatedCost = findViewById(R.id.textEstimatedCost);
        radioGroupBookingType = findViewById(R.id.radioGroupBookingType);
        layoutTimePicker = findViewById(R.id.layoutTimePicker);
        inputDuration = findViewById(R.id.inputDuration);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // Charger info from Intent
        String chargerName = getIntent().getStringExtra("charger_name");
        String priceStr = getIntent().getStringExtra("charger_price");
        if (chargerName != null) textChargerName.setText(chargerName);
        if (priceStr != null) {
            textPrice.setText(priceStr);
            chargerPricePerKWh = Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));
        }

        // Booking type selection
        radioGroupBookingType.setOnCheckedChangeListener((group, checkedId) -> {
            isBookNow = (checkedId == R.id.radioNow);
            layoutTimePicker.setVisibility(isBookNow ? LinearLayout.GONE : LinearLayout.VISIBLE);
            calculateEstimatedCost();
        });

        // Duration input
        inputDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    durationHours = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    durationHours = 1;
                }
                calculateEstimatedCost();
            }
        });

        // Time picker
        textStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(this,
                    (view, hourOfDay, minute1) -> {
                        startHour = hourOfDay;
                        startMinute = minute1;
                        textStartTime.setText(String.format("%02d:%02d", hourOfDay, minute1));
                    }, hour, minute, true);
            timePicker.show();
        });

        // Confirm booking button
        btnConfirmBooking.setOnClickListener(v -> saveBooking());

        // Show initial estimated cost
        calculateEstimatedCost();
    }

    // Calculate estimated cost
    private void calculateEstimatedCost() {
        double estimatedCost = durationHours * chargerPricePerKWh;
        textEstimatedCost.setText(String.format("Estimated Cost: RM %.2f", estimatedCost));
    }

    // Save booking to Firestore
    private void saveBooking() {
        if (!isBookNow && startHour == -1) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        double estimatedCost = durationHours * chargerPricePerKWh;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("chargerName", textChargerName.getText().toString());
        bookingData.put("durationHours", durationHours);
        bookingData.put("status", "BOOKED");
        bookingData.put("estimatedCost", estimatedCost);

        if (!isBookNow) {
            bookingData.put("startHour", startHour);
            bookingData.put("startMinute", startMinute);
        }

        db.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this,
                            "Booking confirmed! Estimated Cost: RM " + String.format("%.2f", estimatedCost),
                            Toast.LENGTH_LONG).show();
                    // Optional: finish activity or redirect
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save booking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}