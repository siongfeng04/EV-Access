package com.example.evaccesstry4;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StartSessionActivity extends AppCompatActivity {

    private TextView textChargerName, textSessionStatus, textCountdown;
    private TextView textBattery, textEnergy, textCost;

    private ProgressBar progressBarSession;
    private Button btnStartCharging, btnEndSession;

    private CountDownTimer timer;
    private long sessionDurationMs = 60 * 60 * 1000; // 1 hour mock
    private long timeRemaining;

    private String chargerName;
    private String bookingId;

    private FirebaseFirestore db;

    // Charging simulation variables
    private double chargingPower = 7.0;   // 7 kW charger
    private double energyDelivered = 0;
    private double cost = 0;
    private double pricePerKwh = 1.5;

    private int batteryStart = 40;
    private int batteryCurrent = 40;
    private int batteryTarget = 80;

    private long sessionStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        // Bind views
        textChargerName = findViewById(R.id.textChargerName);
        textSessionStatus = findViewById(R.id.textSessionStatus);
        textCountdown = findViewById(R.id.textCountdown);
        textBattery = findViewById(R.id.textBattery);
        textEnergy = findViewById(R.id.textEnergy);
        textCost = findViewById(R.id.textCost);

        progressBarSession = findViewById(R.id.progressBarSession);
        btnStartCharging = findViewById(R.id.btnStartCharging);
        btnEndSession = findViewById(R.id.btnEndSession);

        db = FirebaseFirestore.getInstance();

        // Get booking info from Intent
        chargerName = getIntent().getStringExtra("extra_name");
        bookingId = getIntent().getStringExtra("extra_booking_id");

        textChargerName.setText(chargerName != null ? chargerName : "EV Charger");

        btnStartCharging.setOnClickListener(v -> startChargingSession());
        btnEndSession.setOnClickListener(v -> endSession());

        btnEndSession.setEnabled(false);
    }

    private void startChargingSession() {
        btnStartCharging.setEnabled(false);
        btnEndSession.setEnabled(true);

        textSessionStatus.setText("Status: IN PROGRESS");
        textCountdown.setVisibility(TextView.VISIBLE);
        progressBarSession.setVisibility(ProgressBar.VISIBLE);

        sessionStartTime = System.currentTimeMillis();
        timeRemaining = sessionDurationMs;
        energyDelivered = 0;
        cost = 0;
        batteryCurrent = batteryStart;

        timer = new CountDownTimer(sessionDurationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;

                int seconds = (int) (millisUntilFinished / 1000) % 60;
                int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                int hours = (int) ((millisUntilFinished / (1000 * 60 * 60)) % 24);

                textCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                int progress = (int) ((sessionDurationMs - millisUntilFinished) * 100 / sessionDurationMs);
                progressBarSession.setProgress(progress);

                // Battery simulation
                batteryCurrent = batteryStart + progress * (batteryTarget - batteryStart) / 100;
                textBattery.setText("Battery: " + batteryCurrent + "%");

                // Energy delivered simulation
                energyDelivered += chargingPower / 3600; // kWh per second
                textEnergy.setText(String.format("Energy: %.2f kWh", energyDelivered));

                // Cost calculation
                cost = energyDelivered * pricePerKwh;
                textCost.setText(String.format("Cost: RM %.2f", cost));
            }

            @Override
            public void onFinish() {
                textCountdown.setText("00:00:00");
                progressBarSession.setProgress(100);
                btnEndSession.setEnabled(false);

                saveSessionToFirestore(sessionDurationMs);
                textSessionStatus.setText("Status: COMPLETED");

                setResult(RESULT_OK);
                finish();
            }
        }.start();
    }

    private void endSession() {
        if (timer != null) {
            timer.cancel();
        }

        long elapsedMs = sessionDurationMs - timeRemaining;
        saveSessionToFirestore(elapsedMs);

        btnEndSession.setEnabled(false);
        btnStartCharging.setEnabled(false);

        textSessionStatus.setText("Status: COMPLETED");
        progressBarSession.setProgress(0);
        textCountdown.setText("Session ended");

        setResult(RESULT_OK);
        finish();
    }

    private void saveSessionToFirestore(long elapsedMs) {
        int elapsedMinutes = (int) (elapsedMs / (1000 * 60));

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("duration", elapsedMinutes / 60.0); // duration in hours
        sessionData.put("energyDelivered", energyDelivered);
        sessionData.put("totalCost", cost);                // updated field for Booking model
        sessionData.put("finalBattery", batteryCurrent);
        sessionData.put("chargingPower", chargingPower);
        sessionData.put("status", "COMPLETED");
        sessionData.put("startTimestamp", sessionStartTime);
        sessionData.put("endTimestamp", System.currentTimeMillis());

        if (bookingId != null) {
            db.collection("bookings")
                    .document(bookingId)
                    .update(sessionData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this,
                                    "Session completed\nEnergy: " + String.format("%.2f", energyDelivered)
                                            + " kWh\nTotal Cost: RM " + String.format("%.2f", cost),
                                    Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save session", Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this, "No booking ID provided", Toast.LENGTH_SHORT).show();
        }
    }
}