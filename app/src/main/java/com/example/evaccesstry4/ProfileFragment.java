package com.example.evaccesstry4;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private Button btnSwitchRole, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentRole = "user";
    private UserViewModel userViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
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
            getActivity().finish();
        });

        return view;
    }

    private void loadUserData() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        currentRole = document.getString("role");

                        tvName.setText("Name: " + name);
                        tvEmail.setText("Email: " + email);
                        tvRole.setText("Role: " + currentRole);

                        btnSwitchRole.setText(currentRole.equals("host") ? "Switch to User" : "Switch to Host");

                        // Update ViewModel
                        userViewModel.setRole(currentRole);
                    }
                });
    }

    private void switchRole() {
        String uid = mAuth.getCurrentUser().getUid();
        String newRole = currentRole.equals("host") ? "user" : "host";

        db.collection("users").document(uid)
                .update("role", newRole)
                .addOnSuccessListener(unused -> {
                    currentRole = newRole;
                    tvRole.setText("Role: " + currentRole);
                    btnSwitchRole.setText(currentRole.equals("host") ? "Switch to User" : "Switch to Host");

                    // Update ViewModel
                    userViewModel.setRole(currentRole);

                    Toast.makeText(getActivity(), "Role updated to " + currentRole, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Role update failed", Toast.LENGTH_SHORT).show());
    }
}