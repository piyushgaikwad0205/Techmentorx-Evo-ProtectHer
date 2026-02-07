package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import android.content.SharedPreferences;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentDashboardActivity extends AppCompatActivity {

    private MapView mapView;
    private View sosOverlay;

    // Top Status Bar
    private TextView tvChildName, tvLastUpdated, tvBattery, tvNetwork, tvSpeed, tvStatus;
    private ImageView ivNetworkStatus, ivStatusIcon, ivPermissionStatus;

    // Quick Actions
    private MaterialButton btnCallChild, btnNavigate;

    // ... (rest of fields)

    // Health Info
    private TextView tvBloodGroup, tvAllergies, tvConditions, tvMedications, tvHeartRate;

    // Device Status
    private TextView tvAppStatus, tvLastActivity, tvLastLocation;

    // Permission
    private TextView tvPermissionStatus;
    private ImageView ivPermissionIcon;

    // Bottom Sheet
    private BottomSheetBehavior bottomSheetBehavior;

    private FirebaseFirestore db;
    private String targetUserId;
    private Marker childMarker;

    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_parent_dashboard);

        // Get Target User ID
        if (getIntent().hasExtra("TARGET_CHILD_ID")) {
            targetUserId = getIntent().getStringExtra("TARGET_CHILD_ID");
        } else {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                targetUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                Toast.makeText(this, "Demo Mode: Tracking Self", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No Child ID provided!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupMap();
        setupBottomSheet();
        startTracking();

        startAutoUpdate();

        // Header Settings Logic
        findViewById(R.id.editMessage).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsHubActivity.class));
        });

    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        sosOverlay = findViewById(R.id.sosOverlay);

        // Top Status
        tvChildName = findViewById(R.id.tvChildName);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvBattery = findViewById(R.id.tvBattery);
        tvNetwork = findViewById(R.id.tvNetwork);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvStatus = findViewById(R.id.tvStatus);
        ivNetworkStatus = findViewById(R.id.ivNetworkStatus);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        ivPermissionStatus = findViewById(R.id.ivPermissionStatus);

        // Quick Actions
        btnCallChild = findViewById(R.id.btnCallChild);
        btnNavigate = findViewById(R.id.btnNavigate);

        btnCallChild.setOnClickListener(v -> callChild());
        btnNavigate.setOnClickListener(v -> navigateToChild());

        // Health Info
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvConditions = findViewById(R.id.tvConditions);
        tvMedications = findViewById(R.id.tvMedications);
        tvHeartRate = findViewById(R.id.tvHeartRate);

        // Device Status
        tvAppStatus = findViewById(R.id.tvAppStatus);
        tvLastActivity = findViewById(R.id.tvLastActivity);
        tvLastLocation = findViewById(R.id.tvLastLocation);

        // Permission
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        ivPermissionIcon = findViewById(R.id.ivPermissionIcon);

        loadHealthInfo();
    }



    private double lastKnownLat = 0.0;
    private double lastKnownLon = 0.0;

    private void navigateToChild() {
        if (lastKnownLat != 0.0 && lastKnownLon != 0.0) {
            launchMapIntent(lastKnownLat, lastKnownLon);
            return;
        }

        Toast.makeText(this, "Fetching latest location...", Toast.LENGTH_SHORT).show();
        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("latitude") && doc.contains("longitude")) {
                        double lat = doc.getDouble("latitude");
                        double lon = doc.getDouble("longitude");

                        // Update local
                        this.lastKnownLat = lat;
                        this.lastKnownLon = lon;

                        launchMapIntent(lat, lon);
                    } else {
                        Toast.makeText(this, "Child location not available on server yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed to fetch location.", Toast.LENGTH_SHORT).show());
    }

    private void launchMapIntent(double lat, double lon) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            // Fallback
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + ","
                            + lon)));
        }
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);

        // Set default center (will be updated when location arrives)
        mapView.getController().setCenter(new GeoPoint(28.6139, 77.2090)); // Delhi default

        android.util.Log.d("ParentDashboard", "Map initialized");
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(400);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void startTracking() {
        android.util.Log.d("ParentDashboard", "Starting tracking for user: " + targetUserId);

        db.collection("users").document(targetUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("ParentDashboard", "Listen failed: " + e.getMessage());
                        Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        android.util.Log.d("ParentDashboard", "Document received, updating dashboard");
                        updateDashboard(documentSnapshot);
                    } else {
                        android.util.Log.w("ParentDashboard", "Document does not exist");
                        tvStatus.setText("OFFLINE");
                        tvStatus.setTextColor(Color.GRAY);
                    }
                });
    }

    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateLastUpdatedTime();
                updateHandler.postDelayed(this, 3000); // Update every 3 seconds
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void updateDashboard(DocumentSnapshot doc) {
        // Location
        if (doc.contains("latitude") && doc.contains("longitude")) {
            double lat = doc.getDouble("latitude");
            double lon = doc.getDouble("longitude");

            // Debug log
            android.util.Log.d("ParentDashboard", "Location received: " + lat + ", " + lon);

            GeoPoint childPos = new GeoPoint(lat, lon);

            // Store for navigation
            this.lastKnownLat = lat;
            this.lastKnownLon = lon;

            if (childMarker == null) {
                childMarker = new Marker(mapView);
                childMarker.setTitle("Child Location");
                childMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                childMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_person));
                mapView.getOverlays().add(childMarker);

                android.util.Log.d("ParentDashboard", "Marker created and added to map");
            }
            childMarker.setPosition(childPos);
            mapView.getController().animateTo(childPos);

            mapView.invalidate();

            // Update location text
            tvLastLocation.setText(String.format("%.4f, %.4f", lat, lon));

            android.util.Log.d("ParentDashboard", "Map updated with location");
        } else {
            android.util.Log.w("ParentDashboard", "No location data in document");
            tvLastLocation.setText("Location not available");
        }

        // Name Update
        if (doc.contains("name")) {
            String name = doc.getString("name");
            if (name != null && !name.isEmpty())
                tvChildName.setText(name);
        } else if (doc.contains("email")) {
            String email = doc.getString("email");
            if (email != null)
                tvChildName.setText(email);
        }

        // Fetch detailed profile if name is missing or for health info
        db.collection("users").document(targetUserId).collection("personal_info").document("profile")
                .get()
                .addOnSuccessListener(profileDoc -> {
                    if (profileDoc.exists()) {
                        if (profileDoc.contains("name")) {
                            tvChildName.setText(profileDoc.getString("name"));
                        } else if (profileDoc.contains("fullName")) {
                            tvChildName.setText(profileDoc.getString("fullName"));
                        }
                    }
                });

        // Load specific health info
        db.collection("users").document(targetUserId).collection("medical_info").document("details")
                .get()
                .addOnSuccessListener(medDoc -> {
                    if (medDoc.exists()) {
                        if (medDoc.contains("bloodType"))
                            tvBloodGroup.setText(medDoc.getString("bloodType"));
                        if (medDoc.contains("allergies"))
                            tvAllergies.setText(medDoc.getString("allergies"));
                        if (medDoc.contains("conditions"))
                            tvConditions.setText(medDoc.getString("conditions"));
                        if (medDoc.contains("medications"))
                            tvMedications.setText(medDoc.getString("medications"));
                    }
                });

        // SOS Status
        boolean isSos = doc.contains("is_sos_active") && doc.getBoolean("is_sos_active");
        if (isSos) {
            tvStatus.setText("SOS ALERT!");
            tvStatus.setTextColor(Color.RED);
            ivStatusIcon.setColorFilter(Color.RED);
            sosOverlay.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("Safe");
            tvStatus.setTextColor(getResources().getColor(R.color.colorSuccess));
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.colorSuccess));
            sosOverlay.setVisibility(View.GONE);
        }

        // Battery
        if (doc.contains("battery_level")) {
            long batt = doc.getLong("battery_level");
            tvBattery.setText(batt + "%");
        }

        // Speed
        if (doc.contains("speed")) {
            double speed = doc.getDouble("speed");
            tvSpeed.setText(String.format("%.1f km/h", speed * 3.6)); // m/s to km/h
        }

        // Heart Rate
        if (doc.contains("heart_rate")) {
            long hr = doc.getLong("heart_rate");
            tvHeartRate.setText(hr + " BPM");
        }

        // Network Status
        if (doc.contains("is_online")) {
            boolean isOnline = doc.getBoolean("is_online");
            if (isOnline) {
                tvNetwork.setText("Online");
                ivNetworkStatus.setColorFilter(getResources().getColor(R.color.colorSuccess));
                tvNetwork.setTextColor(getResources().getColor(R.color.colorSuccess));
            } else {
                tvNetwork.setText("Offline");
                ivNetworkStatus.setColorFilter(Color.GRAY);
                tvNetwork.setTextColor(Color.GRAY);
            }
        } else {
            // Default if missing
            tvNetwork.setText("Unknown");
            ivNetworkStatus.setColorFilter(Color.GRAY);
        }

        // App Status
        tvAppStatus.setText("Active");
        tvAppStatus.setTextColor(getResources().getColor(R.color.colorSuccess));

        // Last Activity
        tvLastActivity.setText("Just now");

        // Permission Status
        tvPermissionStatus.setText("Granted by child");
        tvPermissionStatus.setTextColor(getResources().getColor(R.color.colorSuccess));
        ivPermissionIcon.setColorFilter(getResources().getColor(R.color.colorSuccess));
    }

    private void updateLastUpdatedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        tvLastUpdated.setText("Updated: " + sdf.format(new Date()));
    }

    private void callChild() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CALL_PHONE }, 100);
            return;
        }

        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String phone = null;
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("mobile")) {
                            phone = documentSnapshot.getString("mobile");
                        } else if (documentSnapshot.contains("phone")) {
                            phone = documentSnapshot.getString("phone");
                        }
                    }

                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } else {
                        // Try fetching from sub-collection profile as fallback
                        db.collection("users").document(targetUserId).collection("personal_info").document("profile")
                                .get().addOnSuccessListener(pDoc -> {
                                    String pPhone = null;
                                    if (pDoc.exists() && pDoc.contains("mobile")) {
                                        pPhone = pDoc.getString("mobile");
                                    }

                                    if (pPhone != null && !pPhone.isEmpty()) {
                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                        intent.setData(Uri.parse("tel:" + pPhone));
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(this, "Child's phone number not found.", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching number", Toast.LENGTH_SHORT).show());
    }

    private void loadHealthInfo() {
        // Load from Firestore or SharedPreferences
        // For now, showing placeholder data
        tvBloodGroup.setText("O+");
        tvAllergies.setText("None");
        tvConditions.setText("None");
        tvMedications.setText("None");
        tvHeartRate.setText("-- BPM");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}