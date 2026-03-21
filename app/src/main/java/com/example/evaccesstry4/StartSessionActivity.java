package com.example.evaccesstry4;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class StartSessionActivity extends AppCompatActivity {

    private TextView textChargerName, textSessionStatus, textCountdown;
    private TextView textBattery, textEnergy, textCost, textBookingDetails;

    private ProgressBar progressBarSession;
    private Button btnStartCharging, btnEndSession;
    private ImageView qrCodeImage;


    private CountDownTimer timer;
    private long sessionDurationMs = 60 * 60 * 1000;
    private long timeRemaining;

    private String chargerName;
    private String bookingId;
    private String chargerId;

    private FirebaseFirestore db;

    private double chargingPower = 7.0;
    private double energyDelivered = 0;
    private double cost = 0;
    private double pricePerKwh;

    private int batteryStart = 40;
    private int batteryCurrent = 40;
    private int batteryTarget = 80;

    private long sessionStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        textChargerName = findViewById(R.id.textChargerName);
        textSessionStatus = findViewById(R.id.textSessionStatus);
        textCountdown = findViewById(R.id.textCountdown);
        textBattery = findViewById(R.id.textBattery);
        textEnergy = findViewById(R.id.textEnergy);
        textCost = findViewById(R.id.textCost);
        textBookingDetails = findViewById(R.id.textBookingDetails);


        progressBarSession = findViewById(R.id.progressBarSession);
        btnStartCharging = findViewById(R.id.btnStartCharging);
        btnEndSession = findViewById(R.id.btnEndSession);
        ImageButton btnBack = findViewById(R.id.btnBack);
        qrCodeImage = findViewById(R.id.qrCodeImage);


        db = FirebaseFirestore.getInstance();

        chargerName = getIntent().getStringExtra("extra_name");
        bookingId = getIntent().getStringExtra("extra_booking_id");
        sessionDurationMs = getIntent().getLongExtra("extra_duration", sessionDurationMs);
        pricePerKwh = getIntent().getDoubleExtra("extra_price", 1.5);
        chargerId = getIntent().getStringExtra("extra_id");

        textChargerName.setText(chargerName != null ? chargerName : "EV Charger");
        textBookingDetails.setText("Duration: " + (sessionDurationMs/3600000) + "h | PricePerKwh: RM "+ pricePerKwh);

// Generate QR
        if (bookingId != null) {
            try {
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.encodeBitmap(bookingId, BarcodeFormat.QR_CODE, 400,400);
                qrCodeImage.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }

        btnStartCharging.setOnClickListener(v -> startChargingSession());

        btnEndSession.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("End Charging Session")
                    .setMessage("Are you sure you want to end the charging session?")
                    .setPositiveButton("Yes", (dialog, which) -> endSession())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnBack.setOnClickListener(v -> onBackPressed());

        btnEndSession.setEnabled(false);
    }

    private void startChargingSession() {
        btnStartCharging.setEnabled(false);
        btnEndSession.setEnabled(true);

        textSessionStatus.setText("Status: IN PROGRESS");
        findViewById(R.id.circleContainer).setVisibility(View.VISIBLE);

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

                batteryCurrent = batteryStart + progress * (batteryTarget - batteryStart) / 100;
                textBattery.setText("Battery: " + batteryCurrent + "%");

                energyDelivered += chargingPower / 3600;
                textEnergy.setText(String.format("Energy: %.2f kWh", energyDelivered));

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

                showRatingDialog(); // ⭐
            }
        }.start();
    }

    private void endSession() {
        if (timer != null) timer.cancel();

        long elapsedMs = sessionDurationMs - timeRemaining;
        saveSessionToFirestore(elapsedMs);

        btnEndSession.setEnabled(false);
        btnStartCharging.setEnabled(false);

        textSessionStatus.setText("Status: COMPLETED");
        progressBarSession.setProgress(0);
        textCountdown.setText("Session ended");

        showRatingDialog(); // ⭐
    }

    private void showRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText textFeedback = dialogView.findViewById(R.id.textFeedback);

        new AlertDialog.Builder(this)
                .setTitle("Rate Your Charging Session")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Submit", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    String feedback = textFeedback.getText().toString().trim();
                    saveRatingToFirestore(rating, feedback);

                    updateServiceRatingByName(chargerName, chargerId);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("charger_id", chargerId);
                    resultIntent.putExtra("new_rating", rating);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    dialog.dismiss();
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    // ================= SAVE RATING =================
    private void saveRatingToFirestore(float rating, String feedback) {

        if (bookingId == null) {
            Toast.makeText(this, "Booking ID missing, cannot save rating", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("rating", rating);
        ratingData.put("feedback", feedback);

        // Only add chargerName if it exists
        if (chargerName != null) {
            ratingData.put("chargerName", chargerName);
        }

        db.collection("bookings")
                .document(bookingId)
                .update(ratingData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save rating: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateServiceRatingByName(String chargerName, String serviceId) {
        if (chargerName == null || serviceId == null) return;

        db.collection("bookings")
                .whereEqualTo("chargerName", chargerName)
                .get()
                .addOnSuccessListener(query -> {
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot doc : query) {
                        Double r = doc.getDouble("rating");
                        if (r != null) {
                            total += r;
                            count++;
                        }
                    }
                    double finalRating = (count > 0) ? (total / count) : 0;

                    // Update service rating
                    db.collection("services")
                            .document(serviceId)
                            .update("rating", finalRating);
                });
    }

    private void saveSessionToFirestore(long elapsedMs) {
        int elapsedMinutes = (int) (elapsedMs / (1000 * 60));
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("duration", elapsedMinutes / 60.0);
        sessionData.put("energyDelivered", energyDelivered);
        sessionData.put("totalCost", cost);
        sessionData.put("finalBattery", batteryCurrent);
        sessionData.put("chargingPower", chargingPower);
        sessionData.put("status", "COMPLETED");
        sessionData.put("startTimestamp", sessionStartTime);
        sessionData.put("endTimestamp", System.currentTimeMillis());
        sessionData.put("charger ID", chargerId);

        if (bookingId != null) {

            db.collection("bookings")
                    .document(bookingId)
                    .update(sessionData)
                    .addOnSuccessListener(aVoid -> {

                        // ⭐ UPDATE WALLET HERE
                        updateWallets(cost);

                        Toast.makeText(this,
                                "Session completed\nEnergy: "
                                        + String.format("%.2f", energyDelivered)
                                        + " kWh\nTotal Cost: RM "
                                        + String.format("%.2f", cost),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void updateWallets(double cost) {

        if (bookingId == null) return;

        db.collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    String userId = doc.getString("userId");
                    String hostId = doc.getString("hostId");

                    if (userId == null || hostId == null) return;

                    // 🔻 Deduct from USER
                    db.collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(userDoc -> {

                                double balance = userDoc.getDouble("wallet") != null
                                        ? userDoc.getDouble("wallet") : 0;

                                db.collection("users")
                                        .document(userId)
                                        .update("wallet", balance - cost);
                            });

                    // 🔺 Add to HOST
                    db.collection("users")
                            .document(hostId)
                            .get()
                            .addOnSuccessListener(hostDoc -> {

                                double balance = hostDoc.getDouble("wallet") != null
                                        ? hostDoc.getDouble("wallet") : 0;

                                db.collection("users")
                                        .document(hostId)
                                        .update("wallet", balance + cost);
                            });
                });
    }
}