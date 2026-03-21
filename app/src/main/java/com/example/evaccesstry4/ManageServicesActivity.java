package com.example.evaccesstry4;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManageServicesActivity extends AppCompatActivity {

    private EditText inputName, inputDistance, inputPrice;
    private Spinner spinnerCategory;
    private Switch switchFastCharger;
    private ImageView imgPreview;
    private Button btnUploadImage, btnSaveService, btnSelectLocation;
    private TextView txtSelectedLocation;

    private Uri imageUri;

    private double selectedLat = 0;
    private double selectedLng = 0;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int MAP_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        inputName = findViewById(R.id.input_name);
        inputDistance = findViewById(R.id.input_distance);
        inputPrice = findViewById(R.id.input_price);
        spinnerCategory = findViewById(R.id.spinner_category);
        switchFastCharger = findViewById(R.id.switch_fast_charger);
        imgPreview = findViewById(R.id.img_preview);
        btnUploadImage = findViewById(R.id.btn_upload_image);
        btnSaveService = findViewById(R.id.btn_save_service);

        btnSelectLocation = findViewById(R.id.btn_select_location);
        txtSelectedLocation = findViewById(R.id.txt_selected_location);

        // Spinner categories
        String[] categories = {"Home", "Mall", "Airbnb", "Office"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Upload image
        btnUploadImage.setOnClickListener(v -> checkPermissionAndPickImage());

        // Save service
        btnSaveService.setOnClickListener(v -> saveService());

        // Open map picker
        btnSelectLocation.setOnClickListener(v -> {

            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);

        });
    }

    // ================= IMAGE PERMISSION =================

    private void checkPermissionAndPickImage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);

            } else {
                openImagePicker();
            }

        } else {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);

            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // ================= ACTIVITY RESULT =================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // IMAGE PICK
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {

            imageUri = data.getData();
            imgPreview.setImageURI(imageUri);

        }

        // MAP PICK
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {

            selectedLat = data.getDoubleExtra("lat", 0);
            selectedLng = data.getDoubleExtra("lng", 0);

            txtSelectedLocation.setText("Lat: " + selectedLat + " , Lng: " + selectedLng);
        }
    }

    // ================= PERMISSION RESULT =================

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openImagePicker();

            } else {

                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================= SAVE SERVICE =================

    private void saveService() {

        String name = inputName.getText().toString().trim();
        String distance = inputDistance.getText().toString().trim();
        String price = inputPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        boolean fastCharger = switchFastCharger.isChecked();

        if (name.isEmpty() || distance.isEmpty() || price.isEmpty()) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLat == 0 || selectedLng == 0) {

            Toast.makeText(this, "Please select charger location on map", Toast.LENGTH_SHORT).show();
            return;
        }

        String hostId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (imageUri != null) {

            uploadImageAndSave(name, distance, price, hostId, fastCharger, category);

        } else {

            saveToFirestore(name, distance, price, hostId, "", fastCharger, category);
        }
    }

    // ================= UPLOAD IMAGE =================

    private void uploadImageAndSave(String name, String distance, String price,
                                    String hostId, boolean fastCharger, String category) {

        String imageName = UUID.randomUUID().toString();

        StorageReference ref = storage.getReference()
                .child("charger_images/" + imageName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {

                            String imageUrl = uri.toString();

                            saveToFirestore(name, distance, price,
                                    hostId, imageUrl, fastCharger, category);

                        })
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Image upload failed",
                            Toast.LENGTH_SHORT).show();

                });
    }

    // ================= SAVE FIRESTORE =================

    private void saveToFirestore(String name, String distance, String price,
                                 String hostId, String imageUrl,
                                 boolean fastCharger, String category) {

        Map<String, Object> service = new HashMap<>();

        service.put("name", name);
        service.put("distance", distance);
        service.put("price", price);
        service.put("lat", selectedLat);
        service.put("lng", selectedLng);
        service.put("hostId", hostId);
        service.put("imageUrl", imageUrl);
        service.put("fastCharger", fastCharger);
        service.put("category", category);

        double power;
        if (fastCharger) {
            power = 22.0;   // fast charger
        } else {
            power = 7.0;    // normal charger
        }
        service.put("chargerPower", power);

        db.collection("services")
                .add(service)
                .addOnSuccessListener(docRef -> {

                    Toast.makeText(this,
                            "Service added successfully!",
                            Toast.LENGTH_SHORT).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Log.e("ManageServices", "Save failed", e);

                    Toast.makeText(this,
                            "Failed to save service",
                            Toast.LENGTH_SHORT).show();
                });
    }
}