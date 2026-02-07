package com.example.sos;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator; // Good to have for future color animations

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import java.util.concurrent.Executor;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import androidx.annotation.Nullable;

public class HomeActivity extends AppCompatActivity {

    MaterialCardView helpline, btnSafeRoute, btnNearby;

    View showContact, btnSosService, editMessage, registerContact;
    TextView tvGreeting;

    // Shadow Mode UI elements
    TextView tvShadowModeSubtitle;
    View ripple1, ripple2, ripple3;

    MediaPlayer mediaPlayer;
    boolean isSirenPlaying = false;

    // Service & Permissions
    private static final int REQUEST_CODE = 11;
    private static final int REQUEST_CHECK_SETTING = 10;
    String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA
    };
    private boolean isServiceActive = false;
    private com.google.firebase.firestore.ListenerRegistration sessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());

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

        setContentView(R.layout.activity_home);

        // Session Enforcement (Single Device Login)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);

            sessionListener = FirebaseFirestore.getInstance().collection("users").document(uid)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                            if (e != null)
                                return;
                            if (snapshot != null && snapshot.exists()) {
                                String serverDeviceId = snapshot.getString("currentDeviceId");
                                if (serverDeviceId != null && !serverDeviceId.equals(deviceId)) {
                                    // Logout
                                    FirebaseAuth.getInstance().signOut();
                                    Toast.makeText(HomeActivity.this, "Session expired. Logged in from another device.",
                                            Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(HomeActivity.this, WelcomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        }
                    });
        }

        // Initialize Contact Picker
        contactPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Uri contactData = result.getData().getData();
                            Cursor c = getContentResolver().query(contactData, null, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                int nameIndex = c
                                        .getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME);
                                int idIndex = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID);

                                String name = "";
                                String id = "";
                                if (nameIndex >= 0)
                                    name = c.getString(nameIndex);
                                if (idIndex >= 0)
                                    id = c.getString(idIndex);

                                String number = "";
                                if (Integer.parseInt(c.getString(c.getColumnIndex(
                                        android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                                    Cursor phones = getContentResolver().query(
                                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                            null,
                                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
                                                    + id,
                                            null, null);
                                    if (phones != null && phones.moveToFirst()) {
                                        int numberIndex = phones.getColumnIndex(
                                                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                                        if (numberIndex >= 0)
                                            number = phones.getString(numberIndex);
                                        phones.close();
                                    }
                                }
                                c.close();

                                if (!name.isEmpty() && !number.isEmpty()) {
                                    // Add Direct
                                    DatabaseHelper db = new DatabaseHelper(HomeActivity.this);
                                    if (db.insertDataFunc(name, number, name)) {
                                        Toast.makeText(HomeActivity.this, "Contact Added Successfully",
                                                Toast.LENGTH_SHORT).show();
                                        if (addContactDialog != null && addContactDialog.isShowing()) {
                                            addContactDialog.dismiss();
                                        }
                                    } else {
                                        Toast.makeText(HomeActivity.this, "Failed to add Contact", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(HomeActivity.this, "Error picking contact", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        checkFirstTimeUser();
        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        updateGreeting();

        // Initialize entrance animations for a smooth landing experience
        initEntranceAnimations();

        // Check service status on creation
        isServiceActive = isMyServiceRunning(ServiceMine.class);
        updateShadowModeUI(isServiceActive);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        // Refresh service status when returning to app
        boolean running = isMyServiceRunning(ServiceMine.class);
        if (running != isServiceActive) {
            isServiceActive = running;
            updateShadowModeUI(isServiceActive);
        }
        checkOnboarding();
    }

    private void checkOnboarding() {
        // 1. Permissions - Request directly on Home Dashboard
        if (!arePermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
            return;
        }

        // 2. Contacts - (Skipped as per user request to not show separate screen)
        /*
         * if (new DatabaseHelper(this).count() == 0) {
         * Intent intent = new Intent(this, RegisterNumberActivity.class);
         * intent.putExtra("ONBOARDING", true);
         * startActivity(intent);
         * return;
         * }
         */

        // 3. Profile
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("ProfileCompleted", false)) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("ONBOARDING", true);
            startActivity(intent);
            return;
        }

        // 4. Medical Info
        SharedPreferences medPrefs = getSharedPreferences("MedicalInfo", MODE_PRIVATE);
        // Check a critical field like 'bloodType'
        if (!medPrefs.contains("bloodType")) {
            Intent intent = new Intent(this, MedicalInfoActivity.class);
            intent.putExtra("ONBOARDING", true);
            startActivity(intent);
            return;
        }
    }

    private boolean arePermissionsGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initializeViews() {
        editMessage = findViewById(R.id.editMessage);
        btnSosService = findViewById(R.id.btnSosService);
        helpline = findViewById(R.id.helpline);
        showContact = findViewById(R.id.showContact);
        btnSafeRoute = findViewById(R.id.btnSafeRoute);
        btnNearby = findViewById(R.id.btnNearby);
        registerContact = findViewById(R.id.registerContact);

        // Shadow Mode Logic
        tvShadowModeSubtitle = findViewById(R.id.tvShadowSubtitle);
        tvGreeting = findViewById(R.id.tvGreeting);

        // Ripple animations
        ripple1 = findViewById(R.id.ripple1);
        ripple2 = findViewById(R.id.ripple2);
        ripple3 = findViewById(R.id.ripple3);
    }

    // Contact Dialog globals
    AlertDialog addContactDialog;
    TextInputEditText etDialogName, etDialogNumber;
    ActivityResultLauncher<Intent> contactPickerLauncher;

    private void setupClickListeners() {

        editMessage.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsHubActivity.class);
            startActivity(intent);
        });

        // SHADOW MODE TOGGLE LOGIC
        btnSosService.setOnClickListener(v -> {
            if (new DatabaseHelper(this).count() == 0) {
                Toast.makeText(this, "Please add an emergency contact first!", Toast.LENGTH_SHORT).show();
                // Optionally proceed or return. For SOS, contacts are key, so we warn.
                // return;
            }

            if (isServiceActive) {
                authenticateToStopService();
            } else {
                startServiceV();
            }
        });

        helpline.setOnClickListener(view -> startActivity(new Intent(HomeActivity.this, SosCall.class)));
        showContact.setOnClickListener(view -> startActivity(new Intent(HomeActivity.this, ShowContact.class)));
        registerContact.setOnClickListener(v -> showLinkWithParentsDialog());

        btnSafeRoute.setOnClickListener(v -> {
            if (new DatabaseHelper(this).count() == 0) {
                Toast.makeText(this, "Please add an emergency contact first!", Toast.LENGTH_SHORT).show();
            }
            startActivity(new Intent(HomeActivity.this, RouteActivity.class));
        });

        btnNearby.setOnClickListener(v -> {
            // Nearby help might not need contacts strictly, but good practice to seek help
            startActivity(new Intent(HomeActivity.this, NearbyHelpActivity.class));
        });

        MaterialCardView btnGallery = findViewById(R.id.btnGallery);
        if (btnGallery != null)
            btnGallery.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, GalleryActivity.class)));

        MaterialCardView btnSafetyCheck = findViewById(R.id.btnSafetyCheck);
        if (btnSafetyCheck != null)
            btnSafetyCheck.setOnClickListener(
                    v -> startActivity(new Intent(HomeActivity.this, SafetyAnalysisActivity.class)));

        MaterialCardView btnDeviceConnect = findViewById(R.id.btnDeviceConnect);
        if (btnDeviceConnect != null)
            btnDeviceConnect
                    .setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DeviceConnectActivity.class)));

    }

    /* showAddContactDialog removed as requested */

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_smart_defence) {
                showSmartDefenceBottomSheet();
                return true;
            } else if (itemId == R.id.nav_parents) {
                startActivity(new Intent(getApplicationContext(), ParentsControlActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Smart Defense FAB Listener
        View btnSmartDefenseFAB = findViewById(R.id.btnSmartDefenseFAB);
        if (btnSmartDefenseFAB != null) {
            btnSmartDefenseFAB.setOnClickListener(v -> showSmartDefenceBottomSheet());
        }
    }

    private void showSmartDefenceBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_smart_defence, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Get test button and status views
        com.google.android.material.card.MaterialCardView btnTestSignal = bottomSheetView
                .findViewById(R.id.btnTestSignal);
        TextView tvTestStatus = bottomSheetView.findViewById(R.id.tvTestStatus);

        // Handle test button click
        btnTestSignal.setOnClickListener(v -> {
            SharedPreferences defensePrefs = getSharedPreferences("SmartDefensePrefs", MODE_PRIVATE);
            String esp32Ip = defensePrefs.getString("esp_ip", "");

            if (esp32Ip.isEmpty()) {
                tvTestStatus.setText("⚠️ Please configure ESP32 IP in Settings first");
                tvTestStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                Toast.makeText(this, "ESP32 IP not configured", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show sending status
            tvTestStatus.setText("📡 Sending test signal...");
            tvTestStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));

            // Send test signal
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String urlString = "http://" + esp32Ip + "/TEST";
                    java.net.URL url = new java.net.URL(urlString);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int responseCode = conn.getResponseCode();
                    conn.disconnect();

                    runOnUiThread(() -> {
                        if (responseCode == 200) {
                            tvTestStatus.setText("✅ Test signal sent successfully!");
                            tvTestStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                            Toast.makeText(this, "ESP32 Connected!", Toast.LENGTH_SHORT).show();
                        } else {
                            tvTestStatus.setText("❌ Connection failed (Error: " + responseCode + ")");
                            tvTestStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        tvTestStatus.setText("❌ Cannot reach ESP32. Check IP and network.");
                        tvTestStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                        Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        bottomSheetDialog.show();
    }

    private void authenticateToStopService() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // If biometric is not set up or not available, fallback to simple stop
            stopServiceV();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(HomeActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // User cancelled or hardware error
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(getApplicationContext(), "Authentication error: " + errString,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        stopServiceV();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Stop Shadow Mode")
                .setSubtitle("Confirm it's you to stop monitoring")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // --- SERVICE CONTROL LOGIC ---

    private void startServiceV() {
        List<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[0]), REQUEST_CODE);
        } else {
            checkLocationAndStart();
        }
    }

    private void checkLocationAndStart() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager != null
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isLocationEnabled) {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build();
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);

            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnFailureListener(this, e -> {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTING);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } else {
            startServiceIntent();
        }
    }

    private void startServiceIntent() {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("Start");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(notificationIntent);

            SharedPreferences prefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
            prefs.edit().putBoolean("isActive", true).apply();

            isServiceActive = true;
            updateShadowModeUI(true);
            Toast.makeText(this, "Shadow Mode Activated", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopServiceV() {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("stop");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(notificationIntent);

            SharedPreferences prefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
            prefs.edit().putBoolean("isActive", false).apply();

            isServiceActive = false;
            updateShadowModeUI(false);
            Toast.makeText(this, "Shadow Mode Deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateShadowModeUI(boolean active) {
        if (tvShadowModeSubtitle == null)
            return; // Views not initialized

        if (active) {
            // Active State
            tvShadowModeSubtitle.setText("Monitoring Active - Tap to Deactivate");
            tvShadowModeSubtitle.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess)); // Green if available
            // Optional: visual feedback on button
            btnSosService.setAlpha(0.8f);

            // Start ripple animation
            startRippleAnimation();
        } else {
            // Inactive State
            tvShadowModeSubtitle.setText("Your Live location is Shared");
            tvShadowModeSubtitle.setTextColor(android.graphics.Color.parseColor("#333333"));
            btnSosService.setAlpha(1.0f);

            // Stop ripple animation
            stopRippleAnimation();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Permissions granted, but DO NOT auto-start service.
                // User must tap the button to start Shadow Mode.
                Toast.makeText(this, "Permissions Granted. You can now use Shadow Mode.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required for Shadow Mode", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkFirstTimeUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstLogin = sharedPreferences.getBoolean("IsFirstLogin", true);
        if (isFirstLogin) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("IsFirstLogin", false);
            editor.apply();
            // Start User Guide for new users
            startActivity(new Intent(HomeActivity.this, HowToUseActivity.class));
        }
    }

    private void updateGreeting() {
        if (tvGreeting == null)
            return;

        String nameToDisplay = "Guardian";

        // Try fetching from Local Profile Prefs first (most recent)
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String localName = prefs.getString("name", "");

        if (!localName.isEmpty()) {
            nameToDisplay = localName;
        } else {
            // Fallback to Firebase User
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .getCurrentUser();
            if (user != null) {
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    nameToDisplay = user.getDisplayName();
                }
            }
        }

        // Show ONLY First Name
        String firstName = nameToDisplay.trim().split("\\s+")[0];
        tvGreeting.setText("Hello, " + firstName);
    }

    private void showLinkWithParentsDialog() {
        String deviceId;
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            deviceId = user.getUid();
        } else {
            deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);
        builder.setView(view);

        android.widget.ImageView ivQR = view.findViewById(R.id.ivQRCode);
        android.widget.TextView tvCode = view.findViewById(R.id.tvCode);
        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btnClose);
        RecyclerView recyclerParents = view.findViewById(R.id.recyclerLinkedParents);

        tvCode.setText("ID: " + deviceId);

        // Generate QR using public API
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + deviceId;

        com.bumptech.glide.Glide.with(this)
                .load(qrUrl)
                .placeholder(R.drawable.ic_menu_camera)
                .into(ivQR);

        // --- Fetch and Show Connected Parents ---
        recyclerParents.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> parentsList = new ArrayList<>();
        ConnectedParentsAdapter adapter = new ConnectedParentsAdapter(this, parentsList, parentUid -> {
            // Logout Logic
            FirebaseFirestore.getInstance()
                    .collection("users").document(deviceId)
                    .collection("connected_parents").document(parentUid)
                    .delete()
                    .addOnSuccessListener(
                            aVoid -> Toast.makeText(HomeActivity.this, "Parent Removed", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast
                            .makeText(HomeActivity.this, "Error removing: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show());
        });
        recyclerParents.setAdapter(adapter);

        // Listen for Realtime Updates
        FirebaseFirestore.getInstance()
                .collection("users").document(deviceId)
                .collection("connected_parents")
                .addSnapshotListener((value, error) -> {
                    if (error != null)
                        return;
                    parentsList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            parentsList.add(doc.getData());
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void checkAndTriggerEsp32(String command) {
        // Get ESP32 IP from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SmartDefensePrefs", MODE_PRIVATE);
        String esp32Ip = prefs.getString("esp_ip", "");

        if (esp32Ip.isEmpty()) {
            // No IP configured, silently skip
            return;
        }

        // Send command to ESP32 in background thread
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String urlString = "http://" + esp32Ip + "/" + command;
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                int responseCode = conn.getResponseCode();
                // Silent operation - no UI feedback needed for automatic triggers
                conn.disconnect();
            } catch (Exception e) {
                // Fail silently - ESP32 might not be available
                e.printStackTrace();
            }
        });
    }

    private void initEntranceAnimations() {
        // 1. Get references to views that need entrance animation
        View headerSection = findViewById(R.id.imageView).getParent() instanceof View
                ? (View) findViewById(R.id.imageView).getParent()
                : null;
        View shadowModeContainer = findViewById(R.id.btnSosService).getParent() instanceof View
                ? (View) findViewById(R.id.btnSosService).getParent()
                : null;
        View tvShadowSubtitle = findViewById(R.id.tvShadowSubtitle);
        View tvProtectionToolsHeader = findViewById(R.id.tvProtectionToolsHeader);
        GridLayout toolsGrid = findViewById(R.id.btnNearby).getParent() instanceof GridLayout
                ? (GridLayout) findViewById(R.id.btnNearby).getParent()
                : null;
        View cardShowContact = findViewById(R.id.cardShowContact);
        View cardRegisterContact = findViewById(R.id.cardRegisterContact);
        View navContainer = findViewById(R.id.nav_container);
        View smartDefenseFAB = findViewById(R.id.btnSmartDefenseFAB);

        // Set initial states (hidden/offset)
        if (headerSection != null) {
            headerSection.setAlpha(0f);
            headerSection.setTranslationY(-50f);
        }

        if (shadowModeContainer != null) {
            shadowModeContainer.setAlpha(0f);
            shadowModeContainer.setScaleX(0.8f);
            shadowModeContainer.setScaleY(0.8f);
        }

        if (tvShadowSubtitle != null)
            tvShadowSubtitle.setAlpha(0f);
        if (tvProtectionToolsHeader != null)
            tvProtectionToolsHeader.setAlpha(0f);

        if (navContainer != null) {
            navContainer.setAlpha(0f);
            navContainer.setTranslationY(200f);
        }

        if (smartDefenseFAB != null) {
            smartDefenseFAB.setAlpha(0f);
            smartDefenseFAB.setTranslationY(200f);
            smartDefenseFAB.setScaleX(0.5f);
            smartDefenseFAB.setScaleY(0.5f);
        }

        // Animate Header
        if (headerSection != null) {
            headerSection.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Animate Shadow Mode Section
        if (shadowModeContainer != null) {
            shadowModeContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setStartDelay(200)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        if (tvShadowSubtitle != null) {
            tvShadowSubtitle.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(600)
                    .start();
        }

        if (tvProtectionToolsHeader != null) {
            tvProtectionToolsHeader.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(700)
                    .start();
        }

        // Animate Bottom Navigation
        if (navContainer != null) {
            navContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(400)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();
        }

        if (smartDefenseFAB != null) {
            smartDefenseFAB.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setStartDelay(500)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .withEndAction(() -> {
                        // Add subtle continuous pulse to the FAB
                        ObjectAnimator pulseX = ObjectAnimator.ofFloat(smartDefenseFAB, "scaleX", 1f, 1.1f, 1f);
                        ObjectAnimator pulseY = ObjectAnimator.ofFloat(smartDefenseFAB, "scaleY", 1f, 1.1f, 1f);
                        pulseX.setDuration(2000);
                        pulseY.setDuration(2000);
                        pulseX.setRepeatCount(ValueAnimator.INFINITE);
                        pulseY.setRepeatCount(ValueAnimator.INFINITE);
                        pulseX.start();
                        pulseY.start();
                    })
                    .start();
        }

        // 6. Staggered Entrance for Tools Grid Items
        if (toolsGrid != null) {
            for (int i = 0; i < toolsGrid.getChildCount(); i++) {
                View child = toolsGrid.getChildAt(i);
                child.setAlpha(0f);
                child.setTranslationY(100f);
                child.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(600)
                        .setStartDelay(800 + (i * 100))
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }

        // Animate remaining cards
        if (cardShowContact != null) {
            cardShowContact.setAlpha(0f);
            cardShowContact.setTranslationY(100f);
            cardShowContact.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(1400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (cardRegisterContact != null) {
            cardRegisterContact.setAlpha(0f);
            cardRegisterContact.setTranslationY(100f);
            cardRegisterContact.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(1500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void startRippleAnimation() {
        if (ripple1 != null) {
            ripple1.setVisibility(View.VISIBLE);
            ripple2.setVisibility(View.VISIBLE);
            ripple3.setVisibility(View.VISIBLE);

            ripple1.startAnimation(createRippleAnim(0));
            ripple2.startAnimation(createRippleAnim(500));
            ripple3.startAnimation(createRippleAnim(1000));
        }
    }

    private void stopRippleAnimation() {
        if (ripple1 != null) {
            ripple1.clearAnimation();
            ripple2.clearAnimation();
            ripple3.clearAnimation();

            ripple1.setVisibility(View.INVISIBLE);
            ripple2.setVisibility(View.INVISIBLE);
            ripple3.setVisibility(View.INVISIBLE);
        }
    }

    private Animation createRippleAnim(long startOffset) {
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);

        ScaleAnimation scale = new ScaleAnimation(1.0f, 1.3f, 1.0f, 1.3f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(1500);
        scale.setRepeatCount(Animation.INFINITE);

        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5f, 0.0f);
        alpha.setDuration(1500);
        alpha.setRepeatCount(Animation.INFINITE);

        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setStartOffset(startOffset);
        return set;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}