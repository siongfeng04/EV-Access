package com.example.evaccesstry4;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TopUpActivity extends AppCompatActivity {

    private TextView tvCurrentBalance;
    private EditText editAmount;
    private Button btnTopUp;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double currentBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);

        tvCurrentBalance = findViewById(R.id.tv_current_balance);
        editAmount = findViewById(R.id.edit_amount);
        btnTopUp = findViewById(R.id.btn_confirm_topup);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadBalance();

        btnTopUp.setOnClickListener(v -> topUpWallet());
    }

    private void loadBalance() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double wallet = doc.getDouble("wallet");
                        currentBalance = wallet != null ? wallet : 0;

                        tvCurrentBalance.setText("Current Balance: RM " +
                                String.format("%.2f", currentBalance));
                    }
                });
    }

    private void topUpWallet() {

        String amountStr = editAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (amount <= 0) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double newBalance = currentBalance + amount;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("wallet", newBalance)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Top-up successful!", Toast.LENGTH_SHORT).show();
                    finish(); // go back to Home
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Top-up failed", Toast.LENGTH_SHORT).show());
    }

    public void onQuickAmountClick(android.view.View view) {
        Button btn = (Button) view;
        String text = btn.getText().toString(); // RM 10
        String amount = text.replace("RM ", "");
        editAmount.setText(amount);
    }
}