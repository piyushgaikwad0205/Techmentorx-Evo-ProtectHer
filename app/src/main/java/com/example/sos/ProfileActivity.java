package com.example.sos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    ImageView profileImage;
    TextView chooseImageText, emergencyContactName, emergencyContactNumber;
    EditText nameEt, phoneEt, ageEt;
    MaterialCardView emergencyContactCard, addMoreCard;
    MaterialButton btnSubmit;
    MaterialButtonToggleGroup genderToggleGroup;
    String selectedGender = "";

    Uri selectedImageUri;
    ActivityResultLauncher<Intent> imagePickerLauncher;
    ActivityResultLauncher<Intent> contactPickerLauncher;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar and navigation bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
            window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.white));

            // Set dark status bar icons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            // Set dark nav bar icons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }

        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        profileImage = findViewById(R.id.imageView4);
        chooseImageText = findViewById(R.id.chooseImageText);
        nameEt = findViewById(R.id.nameEt);
        phoneEt = findViewById(R.id.phoneEt);
        ageEt = findViewById(R.id.ageEt);
        genderToggleGroup = findViewById(R.id.genderToggleGroup);
        emergencyContactCard = findViewById(R.id.emergencyContactCard);
        addMoreCard = findViewById(R.id.addMoreCard);
        btnSubmit = findViewById(R.id.btnSubmit);
        emergencyContactName = findViewById(R.id.emergencyContactName);
        emergencyContactNumber = findViewById(R.id.emergencyContactNumber);

        // Gender Selection
        genderToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFemale)
                    selectedGender = "Female";
                else if (checkedId == R.id.btnMale)
                    selectedGender = "Male";
                else if (checkedId == R.id.btnOthers)
                    selectedGender = "Others";
            }
        });

        // Initialize Image Picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        profileImage.setImageURI(selectedImageUri);
                        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        profileImage.setPadding(0, 0, 0, 0); // remove padding
                        profileImage.clearColorFilter(); // Remove legacy filter
                        profileImage.setImageTintList(null); // Remove app:tint
                    }
                });

        // Initialize Contact Picker
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        String[] projection = {
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        };

                        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int nameIndex = cursor
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                                String contactName = cursor.getString(nameIndex);
                                String contactNumber = cursor.getString(numberIndex);

                                // Update UI
                                // emergencyContactCard.setVisibility(android.view.View.VISIBLE);
                                emergencyContactName.setText(contactName);
                                emergencyContactNumber.setText(contactNumber);

                                Toast.makeText(this, "Contact selected: " + contactName, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to read contact", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Choose image click
        chooseImageText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Import from contacts button
        addMoreCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            contactPickerLauncher.launch(intent);
        });

        // Also allow clicking the contact card to change
        emergencyContactCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            contactPickerLauncher.launch(intent);
        });

        // Submit button
        btnSubmit.setOnClickListener(v -> saveProfile());

        // Load existing data if available
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            String name = prefs.getString("name", "");
            String phone = prefs.getString("phone", "");
            String age = prefs.getString("age", "");
            String gender = prefs.getString("gender", "");

            if (!name.isEmpty())
                nameEt.setText(name);
            if (!phone.isEmpty())
                phoneEt.setText(phone);
            if (!age.isEmpty())
                ageEt.setText(age);

            if (!gender.isEmpty()) {
                selectedGender = gender;
                if (gender.equals("Female"))
                    genderToggleGroup.check(R.id.btnFemale);
                else if (gender.equals("Male"))
                    genderToggleGroup.check(R.id.btnMale);
                else if (gender.equals("Others"))
                    genderToggleGroup.check(R.id.btnOthers);
            }
        }
    }

    private void saveProfile() {
        String name = nameEt.getText().toString().trim();
        String phone = phoneEt.getText().toString().trim();
        String age = ageEt.getText().toString().trim();

        if (name.isEmpty()) {
            nameEt.setError("Name required");
            return;
        }
        if (phone.isEmpty()) {
            phoneEt.setError("Phone required");
            return;
        }
        if (age.isEmpty()) {
            ageEt.setError("Age required");
            return;
        }
        if (selectedGender.isEmpty()) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Saving...");

            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("name", name);
            editor.putString("phone", phone);
            editor.putString("age", age);
            editor.putString("gender", selectedGender);
            editor.putString("owner_uid", user.getUid());
            editor.apply();

            // Mark profile as completed
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("ProfileCompleted", true)
                    .apply();

            // Save to Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("phone", phone);
            userData.put("age", age);
            userData.put("gender", selectedGender);
            userData.put("email", user.getEmail());

            db.collection("users").document(user.getUid())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");

                        // Navigate to Medical Info as next step
                        Intent intent = new Intent(ProfileActivity.this, MedicalInfoActivity.class);
                        intent.putExtra("ONBOARDING", true);
                        startActivity(intent);
                        finish(); // Don't finishAffinity here so we can go back if needed, or finish to prevent
                                  // back?
                        // Usually onboarding is forward only. "finish()" is better.
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                    });
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
