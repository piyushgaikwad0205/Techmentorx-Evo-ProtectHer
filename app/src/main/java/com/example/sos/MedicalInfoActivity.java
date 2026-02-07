package com.example.sos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MedicalInfoActivity extends AppCompatActivity {

    private EditText etName, etConditions, etAllergies, etMedications, etBloodType, etNotes, etAddress, etOrganDonor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_info);

        // Initialize Views
        etConditions = findViewById(R.id.etConditions);
        etAllergies = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        etBloodType = findViewById(R.id.etBloodType);
        etNotes = findViewById(R.id.etNotes);
        etOrganDonor = findViewById(R.id.etOrganDonor);
        etAddress = findViewById(R.id.etAddress);

        // Load saved data
        loadData();

        // Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> saveData());
    }

    private void loadData() {
        SharedPreferences prefs = getSharedPreferences("MedicalInfo", MODE_PRIVATE);
        etConditions.setText(prefs.getString("conditions", ""));
        etAllergies.setText(prefs.getString("allergies", ""));
        etMedications.setText(prefs.getString("medications", ""));
        etBloodType.setText(prefs.getString("bloodType", ""));
        etNotes.setText(prefs.getString("notes", ""));
        etOrganDonor.setText(prefs.getString("organDonor", ""));
        etAddress.setText(prefs.getString("address", ""));
    }

    private void saveData() {
        if (etBloodType.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill in Blood Type at minimum.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("MedicalInfo", MODE_PRIVATE).edit();
        editor.putString("conditions", etConditions.getText().toString());
        editor.putString("allergies", etAllergies.getText().toString());
        editor.putString("medications", etMedications.getText().toString());
        editor.putString("bloodType", etBloodType.getText().toString());
        editor.putString("notes", etNotes.getText().toString());
        editor.putString("organDonor", etOrganDonor.getText().toString());
        editor.putString("address", etAddress.getText().toString());
        editor.apply();

        // Sync to Firestore
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            java.util.Map<String, Object> medicalData = new java.util.HashMap<>();
            medicalData.put("conditions", etConditions.getText().toString());
            medicalData.put("allergies", etAllergies.getText().toString());
            medicalData.put("medications", etMedications.getText().toString());
            medicalData.put("bloodType", etBloodType.getText().toString());
            medicalData.put("notes", etNotes.getText().toString());
            medicalData.put("organDonor", etOrganDonor.getText().toString());
            medicalData.put("address", etAddress.getText().toString());

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .collection("medical_info").document("details")
                    .set(medicalData)
                    .addOnFailureListener(e -> Toast.makeText(MedicalInfoActivity.this,
                            "Cloud Sync Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        Toast.makeText(this, "Medical Info Saved", Toast.LENGTH_SHORT).show();

        if (getIntent().getBooleanExtra("ONBOARDING", false)) {
            android.content.Intent intent = new android.content.Intent(this, HomeActivity.class);
            startActivity(intent);
            finishAffinity();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("ONBOARDING", false)) {
            // Block back press or minimize app
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}
