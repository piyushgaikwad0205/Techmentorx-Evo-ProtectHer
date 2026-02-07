package com.example.sos;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText emailEt, passwordEt;
    MaterialButton btnLogin;
    TextView tvSignup;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar and navigation bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
            window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.white));

            // Set dark status bar icons (since background is white)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            // Set dark navigation bar icons (since background is white)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decorView = window.getDecorView();
                // Combine flags to keep status bar flag
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEt.getText().toString();
                String password = passwordEt.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                checkProfileAndNavigate();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
            }
        });

        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }
    }

    private void checkProfileAndNavigate() {
        String uid = mAuth.getCurrentUser().getUid();

        // Update Device ID for Session Management (Single Device Login)
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("currentDeviceId", deviceId);
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("name")
                            && documentSnapshot.contains("phone") && documentSnapshot.contains("age")) {
                        // Profile exists, now check Medical Info
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .collection("medical_info").document("details")
                                .get()
                                .addOnSuccessListener(medSnap -> {
                                    if (medSnap.exists() && medSnap.contains("bloodType")) {
                                        // Both Complete -> Home
                                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("ProfileCompleted", true)
                                                .apply();
                                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                    } else {
                                        // Medical Incomplete -> Medical Activity
                                        Intent intent = new Intent(LoginActivity.this, MedicalInfoActivity.class);
                                        intent.putExtra("ONBOARDING", true);
                                        startActivity(intent);
                                    }
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Fallback
                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                    finish();
                                });
                    } else {
                        // Profile incomplete
                        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                        intent.putExtra("ONBOARDING", true);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to Home or retry? Better to allow access if error or assume
                    // incomplete?
                    // Safe bet: Go to Home, let Home handle missing data or retry.
                    // But user specifically wants profile form first.
                    Toast.makeText(LoginActivity.this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                });
    }
}
