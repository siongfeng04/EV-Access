package com.example.evaccesstry4;

import static androidx.core.content.ContextCompat.startForegroundService;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SessionsFragment extends Fragment {

    private TextView textBookedLabel, textCompletedLabel;
    private TextView textNoBooked, textNoCompleted;
    private RecyclerView recyclerBooked, recyclerCompleted;

    private BookingAdapter bookedAdapter, completedAdapter;
    private List<Booking> bookedList, completedList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private UserViewModel userViewModel;
    private String currentRole = "user";

    private ActivityResultLauncher<Intent> sessionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activity result launcher to refresh fragment after session ends
        sessionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK) {
                        if ("host".equalsIgnoreCase(currentRole)) {
                            loadHostSessions();
                        } else {
                            loadUserSessions();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sessions, container, false);

        // Labels
        textBookedLabel = view.findViewById(R.id.textBookedHeader);
        textCompletedLabel = view.findViewById(R.id.textCompletedHeader);

        // Empty session messages
        textNoBooked = view.findViewById(R.id.textNoBooked);
        textNoCompleted = view.findViewById(R.id.textNoCompleted);

        // RecyclerViews
        recyclerBooked = view.findViewById(R.id.recyclerBooked);
        recyclerCompleted = view.findViewById(R.id.recyclerCompleted);

        // Lists
        bookedList = new ArrayList<>();
        completedList = new ArrayList<>();

        // Adapters
        bookedAdapter = new BookingAdapter(bookedList);
        completedAdapter = new BookingAdapter(completedList);

        recyclerBooked.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerCompleted.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerBooked.setAdapter(bookedAdapter);
        recyclerCompleted.setAdapter(completedAdapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ViewModel for role
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {

            if (role == null) return;

            currentRole = role;

            bookedAdapter.setRole(currentRole);
            completedAdapter.setRole(currentRole);

            if ("host".equalsIgnoreCase(role)) {

                // Host cannot click sessions
                bookedAdapter.setOnItemClickListener(null);
                completedAdapter.setOnItemClickListener(null);

                loadHostSessions();

            } else {

                // User can click booked sessions
                bookedAdapter.setOnItemClickListener(booking -> {

                    if (!"BOOKED".equalsIgnoreCase(booking.getStatus())) {

                        Toast.makeText(getContext(),
                                "This session is already completed",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(getContext(), StartSessionActivity.class);
                    intent.putExtra("extra_name", booking.getChargerName());
                    intent.putExtra("extra_booking_id", booking.getId());
                    long durationMs = (long) (booking.getDuration() * 60 * 60 * 1000);
                    intent.putExtra("extra_duration", durationMs);

                    // Extract numeric price
                    double pricePerKwh = 1.0; // default
                    String priceStr = booking.getPrice(); // e.g. "RM1.00/kWh"
                    if (priceStr != null && !priceStr.isEmpty()) {
                        priceStr = priceStr.replaceAll("[^0-9.]", ""); // keep digits & dot
                        try {
                            pricePerKwh = Double.parseDouble(priceStr);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    intent.putExtra("extra_price", pricePerKwh);


                    sessionLauncher.launch(intent);
                });

                completedAdapter.setOnItemClickListener(null);

                loadUserSessions();
            }
        });

        return view;
    }

    // --------------------------
    // Load bookings for user
    // --------------------------
    private void loadUserSessions() {

        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    bookedList.clear();
                    completedList.clear();

                    queryDocumentSnapshots.forEach(doc -> {

                        Booking booking = doc.toObject(Booking.class);
                        booking.setId(doc.getId());

                        if ("BOOKED".equalsIgnoreCase(booking.getStatus())) {
                            bookedList.add(booking);
                        } else {
                            completedList.add(booking);
                        }
                    });

                    bookedAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();

                    // Show/hide headers & empty messages
                    textBookedLabel.setVisibility(bookedList.isEmpty() ? View.GONE : View.VISIBLE);
                    textNoBooked.setVisibility(bookedList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerBooked.setVisibility(bookedList.isEmpty() ? View.GONE : View.VISIBLE);

                    textCompletedLabel.setVisibility(completedList.isEmpty() ? View.GONE : View.VISIBLE);
                    textNoCompleted.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerCompleted.setVisibility(completedList.isEmpty() ? View.GONE : View.VISIBLE);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load bookings",
                                Toast.LENGTH_SHORT).show());
    }

    // --------------------------
    // Load bookings for host
    // --------------------------
    private void loadHostSessions() {

        if (auth.getCurrentUser() == null) return;

        String hostId = auth.getCurrentUser().getUid();

        db.collection("bookings")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    bookedList.clear();
                    completedList.clear();

                    queryDocumentSnapshots.forEach(doc -> {

                        Booking booking = doc.toObject(Booking.class);
                        booking.setId(doc.getId());

                        if ("BOOKED".equalsIgnoreCase(booking.getStatus())) {
                            bookedList.add(booking);
                        } else {
                            completedList.add(booking);
                        }
                    });

                    bookedAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();

                    // Show/hide headers & empty messages
                    textBookedLabel.setVisibility(bookedList.isEmpty() ? View.GONE : View.VISIBLE);
                    textNoBooked.setVisibility(bookedList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerBooked.setVisibility(bookedList.isEmpty() ? View.GONE : View.VISIBLE);

                    textCompletedLabel.setVisibility(completedList.isEmpty() ? View.GONE : View.VISIBLE);
                    textNoCompleted.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerCompleted.setVisibility(completedList.isEmpty() ? View.GONE : View.VISIBLE);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load sessions",
                                Toast.LENGTH_SHORT).show());
    }
}