package com.example.evaccesstry4;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole, tvPhone;
    private Button btnSwitchRole, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentRole = "driver";   // default role
    private boolean isSubscribed = false;

    private UserViewModel userViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvRole = view.findViewById(R.id.tvRole);
        btnSwitchRole = view.findViewById(R.id.btnSwitchRole);
        btnLogout = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        loadUserData();

        btnSwitchRole.setOnClickListener(v -> switchRole());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    // =============================
    // LOAD USER DATA
    // =============================
    private void loadUserData() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {

                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");

                        currentRole = document.getString("role");

                        Boolean sub = document.getBoolean("isSubscribed");
                        isSubscribed = sub != null && sub;

                        tvName.setText("Name: " + name);
                        tvEmail.setText("Email: " + email);
                        tvPhone.setText("Phone: " + phone);
                        tvRole.setText("Role: " + currentRole);

                        btnSwitchRole.setText(
                                currentRole.equals("host") ?
                                        "Switch to User" : "Switch to Host"
                        );

                        userViewModel.setRole(currentRole);
                    }
                });
    }

    // =============================
    // SWITCH ROLE (UPDATED)
    // =============================
    private void switchRole() {

        // ⭐ CASE 1: driver → host (FIRST TIME)
        if (currentRole.equals("driver")) {

            if (!isSubscribed) {
                showSubscriptionDialog(); // require payment
            } else {
                updateRole("host");
            }
            return;
        }

        // ⭐ CASE 2: user → host
        if (currentRole.equals("user")) {

            if (!isSubscribed) {
                showSubscriptionDialog();
            } else {
                updateRole("host");
            }
            return;
        }

        // ⭐ CASE 3: host → user
        if (currentRole.equals("host")) {
            updateRole("user");
        }
    }

    // =============================
    // UPDATE ROLE
    // =============================
    private void updateRole(String newRole) {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("role", newRole)
                .addOnSuccessListener(unused -> {

                    currentRole = newRole;

                    tvRole.setText("Role: " + currentRole);
                    btnSwitchRole.setText(
                            currentRole.equals("host") ?
                                    "Switch to User" : "Switch to Host"
                    );

                    userViewModel.setRole(currentRole);

                    Toast.makeText(getActivity(),
                            "Role updated to " + currentRole,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(),
                                "Role update failed",
                                Toast.LENGTH_SHORT).show());
    }

    // =============================
    // SUBSCRIPTION DIALOG
    // =============================
    private void showSubscriptionDialog() {

        new AlertDialog.Builder(requireContext())
                .setTitle("Become a Host")
                .setMessage("To become a host, you need to pay RM10 subscription fee.")
                .setPositiveButton("Pay Now", (dialog, which) -> {

                    // simulate payment success
                    completeSubscription();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =============================
    // COMPLETE SUBSCRIPTION
    // =============================
    private void completeSubscription() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    Double walletObj = document.getDouble("wallet");
                    double wallet = walletObj != null ? walletObj : 0;

                    double fee = 10.0;

                    // ❌ Not enough balance
                    if (wallet < fee) {
                        Toast.makeText(getActivity(),
                                "Insufficient balance. Please top up.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ Deduct balance
                    double newBalance = wallet - fee;

                    db.collection("users").document(uid)
                            .update(
                                    "wallet", newBalance,
                                    "isSubscribed", true,
                                    "role", "host"
                            )
                            .addOnSuccessListener(unused -> {

                                isSubscribed = true;
                                currentRole = "host";

                                tvRole.setText("Role: host");
                                btnSwitchRole.setText("Switch to User");

                                userViewModel.setRole("host");

                                Toast.makeText(getActivity(),
                                        "🎉 Subscription activated! RM10 deducted.",
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }
}