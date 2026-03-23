package com.example.evaccesstry4;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class BookingDialogFragment extends DialogFragment {

    private TextView textChargerName, textPrice, textStartTime, textEstimatedCost;
    private RadioGroup radioGroupBookingType;
    private LinearLayout layoutTimePicker;
    private EditText inputDuration;
    private Button btnConfirmBooking;

    private double chargerPricePerKWh = 1.5;
    private int durationHours = 1;
    private boolean isBookNow = true;

    // Selected date & time
    private int startYear = -1, startMonth = -1, startDay = -1;
    private int startHour = -1, startMinute = -1;

    private String chargerName = "";
    private String priceStr = "";
    private String hostId = "";
    private String chargerID = "";
    private String chargerCategory = "";
    private double chargerPower = 7.0;



    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public BookingDialogFragment(String chargerName, String priceStr, String chargerHostId, String chargerID, double chargerPower, String chargerCategory) {
        this.chargerName = chargerName;
        this.priceStr = priceStr;
        this.hostId = chargerHostId;
        this.chargerID = chargerID;
        this.chargerPower = chargerPower;
        this.chargerCategory = chargerCategory;



        if (priceStr != null) {
            try {
                chargerPricePerKWh = Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));
            } catch (Exception e) {
                chargerPricePerKWh = 1.5;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_booking, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        textChargerName = view.findViewById(R.id.textChargerName);
        textPrice = view.findViewById(R.id.textPrice);
        textStartTime = view.findViewById(R.id.textStartTime);
        textEstimatedCost = view.findViewById(R.id.textEstimatedCost);
        radioGroupBookingType = view.findViewById(R.id.radioGroupBookingType);
        layoutTimePicker = view.findViewById(R.id.layoutTimePicker);
        inputDuration = view.findViewById(R.id.inputDuration);
        btnConfirmBooking = view.findViewById(R.id.btnConfirmBooking);

        textChargerName.setText(chargerName);
        textPrice.setText(priceStr);

        // ===== Set initial start time if Book Now is default =====
        if (radioGroupBookingType.getCheckedRadioButtonId() == R.id.radioNow) {
            setCurrentDateTime();
        }

        // Radio buttons listener
        radioGroupBookingType.setOnCheckedChangeListener((group, checkedId) -> {
            isBookNow = (checkedId == R.id.radioNow);
            layoutTimePicker.setVisibility(isBookNow ? LinearLayout.GONE : LinearLayout.VISIBLE);

            if (isBookNow) {
                setCurrentDateTime(); // auto fill current date & time
            } else {
                textStartTime.setText("Select date & time");
                startYear = startMonth = startDay = startHour = startMinute = -1;
            }
        });

        // Duration listener
        inputDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    durationHours = Integer.parseInt(s.toString());
                } catch (Exception e) {
                    durationHours = 1;
                }
                calculateEstimatedCost();
            }
        });

        // Start time picker
        textStartTime.setOnClickListener(v -> {
            if (!isBookNow) {
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                // Date picker
                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                        (view1, y, m, d) -> {
                            startYear = y;
                            startMonth = m;
                            startDay = d;

                            // Time picker after date selected
                            int hour = c.get(Calendar.HOUR_OF_DAY);
                            int minute = c.get(Calendar.MINUTE);

                            TimePickerDialog timePicker = new TimePickerDialog(getContext(),
                                    (view2, h, min) -> {
                                        startHour = h;
                                        startMinute = min;
                                        Calendar selected = Calendar.getInstance();
                                        selected.set(startYear, startMonth, startDay, startHour, startMinute);
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                        textStartTime.setText(sdf.format(selected.getTime()));
                                    }, hour, minute, true);
                            timePicker.show();

                        }, year, month, day);
                datePicker.show();
            }
        });

        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
        calculateEstimatedCost();

        return view;
    }

    private void setCurrentDateTime() {
        Calendar now = Calendar.getInstance();
        startYear = now.get(Calendar.YEAR);
        startMonth = now.get(Calendar.MONTH);
        startDay = now.get(Calendar.DAY_OF_MONTH);
        startHour = now.get(Calendar.HOUR_OF_DAY);
        startMinute = now.get(Calendar.MINUTE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textStartTime.setText(sdf.format(now.getTime()));
    }

    private void calculateEstimatedCost() {
        double total = chargerPower * durationHours * chargerPricePerKWh;
        textEstimatedCost.setText("Estimated Cost: RM " + total);
    }

    private void confirmBooking() {
        if (!isBookNow && (startYear == -1 || startHour == -1)) {
            Toast.makeText(getContext(), "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(startYear, startMonth, startDay, startHour, startMinute);

        long newStartMillis = selectedTime.getTimeInMillis();
        long newEndMillis = newStartMillis + (durationHours * 60 * 60 * 1000);

        String userId = auth.getCurrentUser().getUid();

        // 🔥 Step 1: Check existing bookings
        db.collection("bookings")
                .whereEqualTo("chargerID", chargerID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    boolean isConflict = false;

                    for (var doc : queryDocumentSnapshots) {
                        try {
                            String existingStartStr = doc.getString("startTime");
                            int existingDuration = doc.getLong("duration").intValue();

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            Date existingStartDate = sdf.parse(existingStartStr);

                            long existingStartMillis = existingStartDate.getTime();
                            long existingEndMillis = existingStartMillis + (existingDuration * 60 * 60 * 1000);

                            // 🔥 Overlap check
                            if (existingStartMillis < newEndMillis &&
                                    existingEndMillis > newStartMillis) {
                                isConflict = true;
                                break;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (isConflict) {
                        Toast.makeText(getContext(), "Time slot not available!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ No conflict → proceed booking
                    saveBooking(newStartMillis, userId);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error checking availability", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveBooking(long startMillis, String userId) {

        double totalCost = chargerPower * durationHours * chargerPricePerKWh;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String startTimeStr = sdf.format(new Date(startMillis));

        Map<String, Object> booking = new HashMap<>();
        booking.put("chargerName", chargerName);
        booking.put("category",chargerCategory);
        booking.put("price", priceStr);
        booking.put("duration", durationHours);
        booking.put("startTime", startTimeStr); // ✅ still String
        booking.put("estimatedCost", totalCost);
        booking.put("totalCost", totalCost);
        booking.put("userId", userId);
        booking.put("hostId", hostId);
        booking.put("chargerID", chargerID); // 🔥 IMPORTANT
        booking.put("status", "BOOKED");
        booking.put("timestamp", System.currentTimeMillis());

        db.collection("bookings")
                .add(booking)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Booking successful!", Toast.LENGTH_LONG).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Booking failed", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}