package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class NearbyHelpActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private MaterialButton btnMainAction; // Used for "Send Alert" or "Accept"
    private MaterialButton btnSecondaryAction; // Used for "View Map" or "Decline"
    private TextView tvStatus, tvRespondedCount, tvActiveCount;
    private TextView tvTitle;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration responsesListener;
    private ListenerRegistration activeUsersListener;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // State
    private String currentRequestId;
    private boolean isResponderMode = false;
    private String incomingRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_help);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        checkPermissions();
        getLastLocation();

        if (getIntent().hasExtra("nearby_request_id")) {
            incomingRequestId = getIntent().getStringExtra("nearby_request_id");
            setupResponderMode();
        } else {
            setupRequesterMode();
        }
    }

    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        btnMainAction = findViewById(R.id.btnSendAlert);
        btnSecondaryAction = findViewById(R.id.btnViewMap); // Reusing this button
        tvStatus = findViewById(R.id.tvStatus);
        tvRespondedCount = findViewById(R.id.tvRespondedCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        topAppBar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            updateMyStatus(location); // Make myself visible
                            fetchActiveUsersCount(location);
                        }
                    });
        }
    }

    private void updateMyStatus(Location location) {
        if (mAuth.getCurrentUser() == null)
            return;

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> locObj = new HashMap<>();
        locObj.put("latitude", location.getLatitude());
        locObj.put("longitude", location.getLongitude());

        data.put("location", locObj);
        data.put("isOnline", true);
        data.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    // --- REQUESTER MODE (Default) ---

    private void setupRequesterMode() {
        isResponderMode = false;
        topAppBar.setTitle("Request Nearby Help");
        btnMainAction.setText("Request Help (100m Radius)");
        btnMainAction.setEnabled(true);
        // Reset color to theme primary (assuming we can get it or use a default
        // compatible with your app)
        // Here getting colorPrimary from theme if possible, or defaulting to a known
        // safe color
        // For simplicity reusing default button tint behavior by not setting background
        // color explicitly
        // OR setting it to a known primary color if changed previously.
        // Since we change it to green/red, we might need to reset.
        // Let's assume standard button behavior, or set a specific color.
        btnMainAction.setBackgroundColor(getColor(com.google.android.material.R.color.design_default_color_primary));

        btnMainAction.setOnClickListener(v -> createHelpRequest());

        btnSecondaryAction.setVisibility(View.GONE); // Hide initially
    }

    private void createHelpRequest() {
        if (currentLocation == null) {
            Toast.makeText(this, "Trying to get location...", Toast.LENGTH_SHORT).show();
            getLastLocation();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnMainAction.setEnabled(false);
        btnMainAction.setText("Sending Alert...");
        tvStatus.setText("Broadcasting Alert...");

        Map<String, Object> request = new HashMap<>();
        request.put("requesterId", mAuth.getCurrentUser().getUid());
        request.put("status", "active");
        request.put("createdAt", FieldValue.serverTimestamp());
        request.put("radius", 100);

        Map<String, Object> loc = new HashMap<>();
        loc.put("latitude", currentLocation.getLatitude());
        loc.put("longitude", currentLocation.getLongitude());
        request.put("requesterLocation", loc);

        db.collection("help_requests").add(request)
                .addOnSuccessListener(documentReference -> {
                    currentRequestId = documentReference.getId();
                    tvStatus.setText("Alert Active! Waiting for helpers...");

                    // Transformation to Cancel Button
                    btnMainAction.setText("Cancel Request");
                    btnMainAction.setBackgroundColor(getColor(R.color.colorError));
                    btnMainAction.setEnabled(true);
                    btnMainAction.setOnClickListener(v -> cancelHelpRequest());

                    listenForResponses(currentRequestId);
                    Toast.makeText(this, "Help Request Sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnMainAction.setEnabled(true);
                    tvStatus.setText("Failed to send alert");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForResponses(String requestId) {
        responsesListener = db.collection("help_requests").document(requestId)
                .collection("responses")
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    if (value == null)
                        return;

                    int yesCount = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String response = doc.getString("response");
                        if ("yes".equals(response)) {
                            yesCount++;
                        }
                    }
                    tvRespondedCount.setText(String.valueOf(yesCount));

                    if (yesCount > 0) {
                        btnSecondaryAction.setVisibility(View.VISIBLE);
                        btnSecondaryAction.setText("View Live Map (" + yesCount + ")");
                        btnSecondaryAction.setOnClickListener(v -> {
                            // Go to Map Activity
                            Intent intent = new Intent(NearbyHelpActivity.this, NearbyMapActivity.class);
                            intent.putExtra("requestId", requestId);
                            intent.putExtra("isRequester", true);
                            startActivity(intent);
                        });
                    }
                });
    }

    private void cancelHelpRequest() {
        if (currentRequestId == null)
            return;

        db.collection("help_requests").document(currentRequestId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show());
    }

    private void resetUI() {
        currentRequestId = null;
        if (responsesListener != null) {
            responsesListener.remove();
            responsesListener = null;
        }
        tvStatus.setText("Status: Idle");
        tvRespondedCount.setText("0");
        setupRequesterMode();
    }

    // --- RESPONDER MODE ---

    private void setupResponderMode() {
        isResponderMode = true;
        topAppBar.setTitle("Emergency Call!");
        tvStatus.setText("Checking request details...");

        btnMainAction.setText("I CAN HELP (YES)");
        btnMainAction.setBackgroundColor(getColor(R.color.colorSuccess)); // Assuming color exists or use generic green

        btnSecondaryAction.setVisibility(View.VISIBLE);
        btnSecondaryAction.setText("NO, I CAN'T");

        // Load request details
        db.collection("help_requests").document(incomingRequestId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvStatus.setText("Someone nearby needs help!");
                        // Could calculate distance here if needed
                    } else {
                        tvStatus.setText("Request expired or cancelled.");
                        btnMainAction.setEnabled(false);
                    }
                });

        btnMainAction.setOnClickListener(v -> sendResponse("yes"));
        btnSecondaryAction.setOnClickListener(v -> sendResponse("no"));
    }

    private void sendResponse(String response) {
        if (mAuth.getCurrentUser() == null)
            return;

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("response", response);
        responseData.put("respondedAt", FieldValue.serverTimestamp());
        responseData.put("responderId", mAuth.getCurrentUser().getUid());

        db.collection("help_requests").document(incomingRequestId)
                .collection("responses").document(mAuth.getCurrentUser().getUid())
                .set(responseData)
                .addOnSuccessListener(aVoid -> {
                    if ("yes".equals(response)) {
                        Toast.makeText(this, "Response Sent! Opening Map...", Toast.LENGTH_SHORT).show();
                        // Open Map
                        Intent intent = new Intent(NearbyHelpActivity.this, NearbyMapActivity.class);
                        intent.putExtra("requestId", incomingRequestId);
                        intent.putExtra("isRequester", false); // I am the helper
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Request ignored.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void fetchActiveUsersCount(Location currentLocation) {
        if (currentLocation == null)
            return;

        // Use SnapshotListener for real-time updates
        if (activeUsersListener != null)
            activeUsersListener.remove();

        activeUsersListener = db.collection("users")
                .whereEqualTo("isOnline", true) // Query only online users directly
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    if (value == null)
                        return;

                    int count = 0;
                    String myId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            if (doc.getId().equals(myId))
                                continue; // Skip myself

                            Map<String, Object> locationObj = (Map<String, Object>) doc.get("location");
                            if (locationObj != null) {
                                double lat = 0;
                                double lng = 0;

                                if (locationObj.get("latitude") instanceof Double)
                                    lat = (Double) locationObj.get("latitude");
                                else if (locationObj.get("latitude") instanceof Long)
                                    lat = ((Long) locationObj.get("latitude")).doubleValue();

                                if (locationObj.get("longitude") instanceof Double)
                                    lng = (Double) locationObj.get("longitude");
                                else if (locationObj.get("longitude") instanceof Long)
                                    lng = ((Long) locationObj.get("longitude")).doubleValue();

                                float[] results = new float[1];
                                Location.distanceBetween(
                                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                                        lat, lng,
                                        results);

                                float distanceInMeters = results[0];
                                if (distanceInMeters <= 5000) { // 5km Radius
                                    count++;
                                }
                            }
                        } catch (Exception e) {
                            // Ignore malformed data
                        }
                    }

                    int finalCount = count;
                    runOnUiThread(() -> {
                        if (tvActiveCount != null) {
                            tvActiveCount.setText(String.valueOf(finalCount));
                        }

                        // Logic: Only allow request if helpers are > 0
                        // AND we are not currently in an active request state (showing Cancel button)
                        if (currentRequestId == null && !isResponderMode) {
                            if (finalCount > 0) {
                                btnMainAction.setEnabled(true);
                                btnMainAction.setText("Request Help (100m Radius)");
                                btnMainAction.setBackgroundColor(
                                        getColor(com.google.android.material.R.color.design_default_color_primary));
                            } else {
                                btnMainAction.setEnabled(false);
                                btnMainAction.setText("No Helpers Nearby");
                                btnMainAction.setBackgroundColor(getColor(android.R.color.darker_gray));
                            }
                        }
                    });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (responsesListener != null) {
            responsesListener.remove();
        }
        if (activeUsersListener != null) {
            activeUsersListener.remove();
        }
    }
}
