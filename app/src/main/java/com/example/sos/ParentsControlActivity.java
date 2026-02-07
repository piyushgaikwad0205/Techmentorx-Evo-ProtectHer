package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class ParentsControlActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private LinearLayout cardScanQR;
    private RecyclerView recyclerLinkedDevices;
    private LinearLayout emptyStateLinkedDevices;
    private ArrayList<String> linkedDeviceIds;
    private Map<String, String> linkedDeviceNames = new HashMap<>();

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedDeviceId = result.getContents();
                    handleScannedQR(scannedDeviceId);
                } else {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parents_control);

        initViews();

        loadLinkedDevices();
        setSystemBarColors();
    }

    private void setSystemBarColors() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(getColor(android.R.color.white));
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void initViews() {
        cardScanQR = findViewById(R.id.cardScanQR);
        recyclerLinkedDevices = findViewById(R.id.recyclerLinkedDevices);
        emptyStateLinkedDevices = findViewById(R.id.emptyStateLinkedDevices);

        cardScanQR.setOnClickListener(v -> checkCameraPermissionAndScan());

        recyclerLinkedDevices.setLayoutManager(new LinearLayoutManager(this));
        linkedDeviceIds = new ArrayList<>();
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQRScanner();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        }
    }

    private void startQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan the QR code from your child's device");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivityPortrait.class);
        qrCodeLauncher.launch(options);
    }

    private void handleScannedQR(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalDeviceId = deviceId.trim(); // Normalize and make final

        // Prevent Self-Linking
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(finalDeviceId)) {
            Toast.makeText(this, "You cannot track your own device! Please scan a different child device.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check if already linked
        if (linkedDeviceIds.contains(finalDeviceId)) {
            Toast.makeText(this, "Device already linked", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Fetching Child Profile...", Toast.LENGTH_SHORT).show();

        // Fetch name from Firestore
        FirebaseFirestore.getInstance().collection("users").document(finalDeviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = "Child Device";
                    boolean found = false;
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("name")) {
                            name = documentSnapshot.getString("name");
                            found = true;
                        } else if (documentSnapshot.contains("email")) {
                            name = documentSnapshot.getString("email");
                            found = true;
                        }
                    }

                    if (found) {
                        saveLinkedDevice(finalDeviceId, name);
                        Toast.makeText(this, "Successfully Linked: " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        // Document missing or name missing
                        saveLinkedDevice(finalDeviceId, "Child Device");
                        Toast.makeText(this, "Linked, but Child Profile Name not found. ID: " + finalDeviceId,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    saveLinkedDevice(finalDeviceId, "Child Device"); // Fallback
                    Toast.makeText(this, "Linked without Name. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getPrefsName() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            return "LinkedDevices_" + user.getUid();
        }
        return "LinkedDevices_Guest";
    }

    private void saveLinkedDevice(String deviceId, String name) {
        int index = linkedDeviceIds.size();
        getSharedPreferences(getPrefsName(), MODE_PRIVATE)
                .edit()
                .putString("device_" + index, deviceId)
                .putString("name_" + index, name)
                .putInt("device_count", index + 1)
                .apply();

        // Open Parent Dashboard for this device
        Intent intent = new Intent(this, ParentDashboardActivity.class);
        intent.putExtra("TARGET_CHILD_ID", deviceId);
        startActivity(intent);

        Toast.makeText(this, "Linked with " + name, Toast.LENGTH_SHORT).show();
        loadLinkedDevices();

        // SYNC: Add Parent to Child's "connected_parents" collection
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (me != null) {
            FirebaseFirestore.getInstance().collection("users").document(me.getUid())
                    .collection("personal_info").document("profile")
                    .get()
                    .addOnSuccessListener(profileSnap -> {
                        String pName = "Parent";
                        if (profileSnap.exists() && profileSnap.contains("fullName")) {
                            pName = profileSnap.getString("fullName");
                        } else {
                            // Fallback to Auth
                            pName = me.getDisplayName();
                            if (pName == null || pName.isEmpty()) {
                                pName = (me.getEmail() != null) ? me.getEmail().split("@")[0] : "Parent";
                            }
                        }

                        Map<String, Object> parentInfo = new HashMap<>();
                        parentInfo.put("uid", me.getUid());
                        parentInfo.put("email", me.getEmail());
                        parentInfo.put("name", pName);
                        parentInfo.put("timestamp", System.currentTimeMillis());

                        FirebaseFirestore.getInstance()
                                .collection("users").document(deviceId)
                                .collection("connected_parents").document(me.getUid())
                                .set(parentInfo)
                                .addOnFailureListener(e -> Toast
                                        .makeText(this, "Failed to sync link cloud: " + e.getMessage(),
                                                Toast.LENGTH_SHORT)
                                        .show());
                    })
                    .addOnFailureListener(e -> {
                        // Fallback if fetch fails
                        Map<String, Object> parentInfo = new HashMap<>();
                        parentInfo.put("uid", me.getUid());
                        parentInfo.put("email", me.getEmail());
                        String pName = (me.getEmail() != null) ? me.getEmail().split("@")[0] : "Parent";
                        parentInfo.put("name", pName);
                        parentInfo.put("timestamp", System.currentTimeMillis());

                        FirebaseFirestore.getInstance()
                                .collection("users").document(deviceId)
                                .collection("connected_parents").document(me.getUid())
                                .set(parentInfo);
                    });
        }
    }

    private void loadLinkedDevices() {
        linkedDeviceIds.clear();
        linkedDeviceNames.clear();
        int deviceCount = getSharedPreferences(getPrefsName(), MODE_PRIVATE)
                .getInt("device_count", 0);

        for (int i = 0; i < deviceCount; i++) {
            String deviceId = getSharedPreferences(getPrefsName(), MODE_PRIVATE)
                    .getString("device_" + i, null);
            String name = getSharedPreferences(getPrefsName(), MODE_PRIVATE)
                    .getString("name_" + i, "Child Device");
            if (deviceId != null) {
                linkedDeviceIds.add(deviceId);
                linkedDeviceNames.put(deviceId, name);

                // Auto-refresh name in background
                refreshDeviceName(deviceId, i);
            }
        }

        updateUI();
    }

    private void refreshDeviceName(String deviceId, int index) {
        FirebaseFirestore.getInstance().collection("users").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String newName = null;
                        if (documentSnapshot.contains("name")) {
                            newName = documentSnapshot.getString("name");
                        } else if (documentSnapshot.contains("email")) {
                            newName = documentSnapshot.getString("email");
                        }

                        if (newName != null) {
                            // Update Request
                            String currentName = linkedDeviceNames.get(deviceId);
                            if (!newName.equals(currentName)) {
                                linkedDeviceNames.put(deviceId, newName);

                                // Update Local Storage
                                getSharedPreferences(getPrefsName(), MODE_PRIVATE)
                                        .edit()
                                        .putString("name_" + index, newName)
                                        .apply();

                                // Refresh Adapter
                                if (recyclerLinkedDevices.getAdapter() != null) {
                                    recyclerLinkedDevices.getAdapter().notifyItemChanged(index);
                                }
                            }
                        }
                    }
                });
    }

    private void updateUI() {
        if (linkedDeviceIds.isEmpty()) {
            emptyStateLinkedDevices.setVisibility(View.VISIBLE);
            recyclerLinkedDevices.setVisibility(View.GONE);
        } else {
            emptyStateLinkedDevices.setVisibility(View.GONE);
            recyclerLinkedDevices.setVisibility(View.VISIBLE);
            LinkedDevicesAdapter adapter = new LinkedDevicesAdapter(this, linkedDeviceIds, linkedDeviceNames,
                    this::removeLinkedDevice);
            recyclerLinkedDevices.setAdapter(adapter);
        }
    }

    private void removeLinkedDevice(String deviceId) {
        // Remove from memory
        linkedDeviceIds.remove(deviceId);
        linkedDeviceNames.remove(deviceId);

        // Rewrite SharedPreferences
        getSharedPreferences(getPrefsName(), MODE_PRIVATE).edit().clear().apply();

        android.content.SharedPreferences.Editor editor = getSharedPreferences(getPrefsName(), MODE_PRIVATE).edit();
        editor.putInt("device_count", linkedDeviceIds.size());

        for (int i = 0; i < linkedDeviceIds.size(); i++) {
            String id = linkedDeviceIds.get(i);
            String name = linkedDeviceNames.get(id);
            editor.putString("device_" + i, id);
            editor.putString("name_" + i, name);
        }
        editor.apply();

        Toast.makeText(this, "Device removed", Toast.LENGTH_SHORT).show();
        loadLinkedDevices();

        // SYNC: Remove Parent from Child's "connected_parents" collection
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (me != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(deviceId)
                    .collection("connected_parents").document(me.getUid())
                    .delete();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
