package com.example.evaccesstry4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword, editConfirm;
    private RadioGroup roleGroup;
    private Button btnRegister, btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        editName = findViewById(R.id.editRegName);
        editEmail = findViewById(R.id.editRegEmail);
        editPassword = findViewById(R.id.editRegPassword);
        editConfirm = findViewById(R.id.editRegConfirm);
        //roleGroup = findViewById(R.id.radioGroupRole); // Radio buttons: Driver, Host
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBackToLogin);

        // Register button
        btnRegister.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String confirm = editConfirm.getText().toString().trim();

            //int selectedRoleId = roleGroup.getCheckedRadioButtonId();
            //if (selectedRoleId == -1) {
            //    Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            //    return;
            //}
            //RadioButton selectedRoleBtn = findViewById(selectedRoleId);
            //String role = selectedRoleBtn.getText().toString().toLowerCase(); // driver or host
            String role = "driver";

            // Basic validations
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase Auth registration
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                saveUserToFirestore(firebaseUser.getUid(), name, email,role);
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Back to login
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // Save user profile to Firestore
    private void saveUserToFirestore(String uid, String name, String email, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role", role);
        user.put("wallet", 120.0);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                });
    }
}