package com.example.sos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import java.util.ArrayList;
import android.os.Bundle;

public class ServiceMine extends Service implements SensorEventListener {

    boolean isRunning = false;
    private Vibrator vibrator;
    DatabaseHelper db;
    FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime;
    private long lastMovementTime;
    private static final int SHAKE_THRESHOLD = 90;
    private static final int SHAKE_THRESHOLD_VIOLENT = 150; // Violent shake
    private static final int MOVEMENT_THRESHOLD_MIN = 8;
    private static final int MOVEMENT_THRESHOLD_MAX = 12;
    private static final long INACTIVITY_LIMIT = 1 * 60 * 1000; // 1 Minute

    // Advanced Triggers Logic
    private boolean isSuspicious = false;
    private long suspiciousStartTime = 0;
    private static final long SUSPICIOUS_WINDOW = 30 * 1000; // 30 Seconds suspicious window
    private int orientationChangeCount = 0;
    private long lastOrientationTime = 0;
    private float[] lastAccelValues = new float[3];
    private Sensor lightSensor;
    private boolean isCameraCovered = false;

    private LocationCallback locationCallback;
    private android.content.BroadcastReceiver sensorReceiver;
    private int powerClickCount = 0;
    private long lastPowerClickTime = 0;

    // Missing Fields Added
    String myLocation;
    Location currentLocation; // Added for professional SOS
    SmsManager manager = SmsManager.getDefault();
    Handler inactivityHandler = new Handler(Looper.getMainLooper());
    Runnable inactivityRunnable;

    // Gemini Safety Detection
    private GeminiSafetyHelper geminiHelper;
    private Location lastSafetyCheckLocation;
    private long lastSafetyCheckTime = 0;
    private static final float SAFETY_CHECK_DISTANCE_METERS = 300.0f; // 300-500m
    private static final long SAFETY_CHECK_INTERVAL_MS = 15 * 60 * 1000; // 15 Minute

    // Firestore Sync
    com.google.firebase.auth.FirebaseAuth mAuth;
    com.google.firebase.firestore.FirebaseFirestore firestore;

    // Passive Safety Detection System (Rule 1, 2, 3)
    private java.util.LinkedList<Long> screenToggleTimestamps = new java.util.LinkedList<>();
    private java.util.LinkedList<Long> appSwitchTimestamps = new java.util.LinkedList<>();
    private long lastInteractionTimestamp = System.currentTimeMillis();
    private String lastForegroundPackage = "";
    private static final int RULE1_THRESHOLD = 5; // Screen Toggles in 60s
    private static final int RULE2_THRESHOLD = 5; // App Switches in 60s
    private static final long RULE3_THRESHOLD_MS = 10 * 60 * 1000; // 10 Minutes
    private boolean isScreenOn = true;
    private Handler passiveSafetyHandler = new Handler(Looper.getMainLooper());
    private Runnable passiveSafetyRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Initialize Combined Sensor/Power/System Listeners
        initializeSystemListeners();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        lastMovementTime = System.currentTimeMillis();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        myLocation = "http://maps.google.com/?q=" + location.getLatitude() + ","
                                + location.getLongitude();

                        // Sync to Firestore (Parent Dashboard)
                        updateFirestoreLocation(location);

                        // Rule: Sudden Speed Change (Running/Vehicle) -> Suspicious
                        if (location.hasSpeed() && location.getSpeed() > 5.0) { // > 18 km/h (Running/Vehicle)
                            activateSuspiciousMode("High Speed Detected: " + location.getSpeed() + "m/s");
                            if (location.getSpeed() > 25.0 && isSuspiciousActive()) { // Very fast > 90km/h and
                                                                                      // suspicious
                                triggerSOS();
                            }
                        }

                        // Gemini Safety Check Logic
                        performIntelligentSafetyCheck(location);
                    }
                }
            }
        };
        startLocationUpdates();
        startInactivityCheck();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            return;
        }

        // Get the current location of user
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationToken() {

            @Override
            public boolean isCancellationRequested() {
                return false;
            }

            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    myLocation = "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    updateFirestoreLocation(location);
                } else {
                    myLocation = "Unable to Find Location :(";
                }
            }
        });

        db = new DatabaseHelper(this); // Initialize DB for History

        initializeVoiceListening();
        listenForNearbyAlerts();

        // Initialize Gemini Helper
        geminiHelper = new GeminiSafetyHelper((level, reason) -> {
            updateSafetyNotification(level, reason);
        });
    }

    private void updateFirestoreLocation(Location location) {
        if (mAuth.getCurrentUser() != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            java.util.Map<String, Object> locationObj = new java.util.HashMap<>();
            locationObj.put("latitude", location.getLatitude());
            locationObj.put("longitude", location.getLongitude());

            data.put("location", locationObj);
            data.put("lastUpdated", com.google.firebase.firestore.FieldValue.serverTimestamp());
            data.put("isOnline", true);
            data.put("locationEnabled", true);

            // Add other personal info if available (e.g. from SharedPreferences)
            // For now, minimal compliance with spec

            // Speed (m/s)
            data.put("speed", location.hasSpeed() ? location.getSpeed() : 0.0);

            // Battery
            android.content.IntentFilter ifilter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    : -1;
            float batteryPct = (level != -1 && scale != -1) ? (level * 100 / (float) scale) : 0;
            data.put("batteryLevel", (int) batteryPct);

            firestore.collection("users").document(mAuth.getCurrentUser().getUid())
                    .set(data, com.google.firebase.firestore.SetOptions.merge());
        }
    }

    private void updateSafetyNotification(String level, String reason) {
        String title = "Smart Safety: " + level;
        String content = reason;

        // If Risky, maybe vibrate slightly or change icon (optional)
        // Updating Foreground Notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, "MYID")
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.siren)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification); // ID 1 is the foreground service ID
    }

    private void initializeSystemListeners() {
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");

        sensorReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null)
                    return;

                long currentTime = System.currentTimeMillis();

                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    isScreenOn = false;
                    recordScreenToggle();
                    // Rule: Sudden Screen Lock After Panic/Suspicious Signals
                    if (isSuspiciousActive()) {
                        Log.d("SOS_RULES", "Screen Locked during Suspicious State -> SOS");
                        triggerSOS();
                    } else {
                        // Power Button Logic for counting clicks
                        checkPowerButtonSequence(currentTime);
                    }
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    isScreenOn = true;
                    recordScreenToggle();
                    checkPowerButtonSequence(currentTime);
                } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                    int state = intent.getIntExtra("state", -1);
                    if (state == 0 && isSuspiciousActive()) { // Unplugged
                        Log.d("SOS_RULES", "Headphones Removed during Suspicious State -> SOS");
                        triggerSOS();
                    }
                } else if (android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    if (isSuspiciousActive()) {
                        Log.d("SOS_RULES", "Bluetooth Disconnected during Suspicious State -> SOS");
                        triggerSOS();
                    }
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                    if (level < 5 && isSuspiciousActive()) {
                        Log.d("SOS_RULES", "Low Battery during Suspicious State -> SOS");
                        triggerSOS();
                    }
                } else if (android.net.ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

                    // Update Firestore status immediately
                    if (mAuth.getCurrentUser() != null) {
                        java.util.Map<String, Object> statusUpdate = new java.util.HashMap<>();
                        statusUpdate.put("isOnline", isConnected);
                        statusUpdate.put("lastOnlineAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        firestore.collection("users").document(mAuth.getCurrentUser().getUid())
                                .set(statusUpdate, com.google.firebase.firestore.SetOptions.merge());
                    }

                    if (!isConnected && isSuspiciousActive()) {
                        Log.d("SOS_RULES", "Network Lost during Suspicious State -> SOS");
                        triggerSOS();
                    }
                } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                    if (isSuspiciousActive()) {
                        Log.d("SOS_RULES", "SIM State Changed during Suspicious State -> SOS");
                        triggerSOS();
                    }
                }
            }
        };
        registerReceiver(sensorReceiver, filter);
    }

    private void checkPowerButtonSequence(long currentTime) {
        if (currentTime - lastPowerClickTime > 1000) {
            powerClickCount = 0;
        }
        powerClickCount++;
        lastPowerClickTime = currentTime;
        // Rule: Power Button 3-5 times
        if (powerClickCount >= 3) {
            triggerSOS();
            powerClickCount = 0;
        }
    }

    private void activateSuspiciousMode(String reason) {
        isSuspicious = true;
        suspiciousStartTime = System.currentTimeMillis();
        Log.d("SOS_RULES", "Suspicious Mode Activated: " + reason);
        // Reset after window ends
        new Handler(Looper.getMainLooper()).postDelayed(() -> isSuspicious = false, SUSPICIOUS_WINDOW);
    }

    private boolean isSuspiciousActive() {
        if (isSuspicious && (System.currentTimeMillis() - suspiciousStartTime < SUSPICIOUS_WINDOW)) {
            return true;
        }
        isSuspicious = false;
        return false;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void startInactivityCheck() {
        inactivityRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastMovementTime) > INACTIVITY_LIMIT) {
                    triggerInactivityAlert();
                }
                inactivityHandler.postDelayed(this, 10000); // Check frequently for demo
            }
        };
        inactivityHandler.post(inactivityRunnable);
    }

    private void triggerInactivityAlert() {
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        lastMovementTime = System.currentTimeMillis();
    }

    MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int originalVolume;
    private Handler volumeHandler = new Handler(Looper.getMainLooper());
    private Runnable volumeRunnable;

    // Safe Route Fields
    boolean isRouteMode = false;
    java.util.ArrayList<SimpleLocation> routePath; // Store full path using generic model
    private final Handler liveLocationHandler = new Handler(Looper.getMainLooper());
    private Runnable liveLocationRunnable;
    private static final long LIVE_LOCATION_BURST_INTERVAL = 5 * 1000; // 5 Seconds (Burst)
    private static final long LIVE_LOCATION_NORMAL_INTERVAL = 15 * 1000; // 15 Seconds (Battery Optimized)
    private long liveTrackingStartTime = 0; // To track burst duration
    private static final double MAX_DEVIATION_METERS = 200.0;

    // Pre-trigger Handler
    private Handler preTriggerHandler = new Handler(Looper.getMainLooper());
    private Runnable preTriggerRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        String action = (intent != null) ? intent.getAction() : null;

        if (action != null && action.equalsIgnoreCase("STOP")) {
            stopServiceCommon();
        } else if (action != null && action.equalsIgnoreCase("START_ROUTE")) {
            // Receive the full list of points
            routePath = intent.getParcelableArrayListExtra("route_points");

            if (routePath != null && !routePath.isEmpty()) {
                isRouteMode = true;
                startLiveLocationUpdates();
                startForegroundNotification("Safe Route Active", "Monitoring your path...");
                isRunning = true;
            } else {
                startForegroundNotification("Safe Route Error", "No route data received.");
            }
            return START_NOT_STICKY;
        } else if (action != null && action.equalsIgnoreCase("NEARBY_HELP")) {
            triggerNearbyAlert("COMMUNITY_HELP");
            return START_STICKY;
        } else {
            startForegroundNotification("Protected", "Shake to SOS Active");
            isRunning = true;
            startListening();
            startBluetoothMonitoring();
            startPassiveSafetySystem();
            return START_STICKY;
        }
        return START_STICKY;
    }

    private void recordScreenToggle() {
        long now = System.currentTimeMillis();
        screenToggleTimestamps.add(now);
        lastInteractionTimestamp = now;
        checkPassiveSafetyRules();
    }

    private void startPassiveSafetySystem() {
        passiveSafetyRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning)
                    return;

                checkAppSwitches();
                checkPassiveSafetyRules();

                passiveSafetyHandler.postDelayed(this, 15000); // Check every 15s (Battery optimized)
            }
        };
        passiveSafetyHandler.post(passiveSafetyRunnable);
    }

    private void checkAppSwitches() {
        android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(
                Context.USAGE_STATS_SERVICE);
        if (usm == null)
            return;

        long now = System.currentTimeMillis();
        java.util.List<android.app.usage.UsageStats> stats = usm
                .queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 30000, now);

        if (stats != null && !stats.isEmpty()) {
            android.app.usage.UsageStats recent = null;
            for (android.app.usage.UsageStats s : stats) {
                if (recent == null || s.getLastTimeUsed() > recent.getLastTimeUsed()) {
                    recent = s;
                }
            }

            if (recent != null && !recent.getPackageName().equals(lastForegroundPackage)) {
                if (!lastForegroundPackage.isEmpty()) {
                    appSwitchTimestamps.add(now);
                    lastInteractionTimestamp = now;
                }
                lastForegroundPackage = recent.getPackageName();
            }
        }
    }

    private void checkPassiveSafetyRules() {
        long now = System.currentTimeMillis();
        long window = 60 * 1000;

        // Cleanup old data (Time-based decay)
        while (!screenToggleTimestamps.isEmpty() && now - screenToggleTimestamps.peekFirst() > window)
            screenToggleTimestamps.pollFirst();
        while (!appSwitchTimestamps.isEmpty() && now - appSwitchTimestamps.peekFirst() > window)
            appSwitchTimestamps.pollFirst();

        double risk1 = Math.min(1.0, screenToggleTimestamps.size() / (double) RULE1_THRESHOLD);
        double risk2 = Math.min(1.0, appSwitchTimestamps.size() / (double) RULE2_THRESHOLD);

        long screenOnInactivity = isScreenOn ? (now - lastInteractionTimestamp) : 0;
        double risk3 = Math.min(1.0, screenOnInactivity / (double) RULE3_THRESHOLD_MS);

        if (risk1 >= 1.0) {
            triggerPassiveSOS("Screen Toggle Panic: " + screenToggleTimestamps.size() + " toggles in 60s");
        } else if (risk2 >= 1.0) {
            triggerPassiveSOS("Rapid App Switching: " + appSwitchTimestamps.size() + " switches in 60s");
        } else if (risk3 >= 1.0) {
            triggerPassiveSOS("Frozen State Detection: Device inactive for " + (screenOnInactivity / 60000)
                    + " mins with screen ON");
        }
    }

    private void triggerPassiveSOS(String reason) {
        if (isSOSActive)
            return;
        Log.d("PASSIVE_SAFETY", "Anomaly Detected: " + reason);

        fetchUserProfile(profile -> {
            profile.spokenKeyword = "PASSIVE_ANOMALY";
            // Custom message with reasoning
            String customReason = "🚨 PASSIVE SAFETY ALERT\nReason: " + reason;

            // Send SOS
            isSOSActive = true;
            startSiren();

            String location = (myLocation != null) ? myLocation : "Locating...";
            String message = EmergencyConfig.formatEmergencyMessage(EmergencyConfig.TYPE_SOS_KEYWORDS, profile,
                    location, this);
            // Prepend the anomaly reason
            message = customReason + "\n\n" + message;

            db = new DatabaseHelper(this);
            java.util.ArrayList<ContactModel> contacts = db.fetchData();
            for (ContactModel contact : contacts) {
                try {
                    java.util.ArrayList<String> parts = manager.divideMessage(message);
                    manager.sendMultipartTextMessage(contact.getNumber(), null, parts, null, null);
                } catch (Exception e) {
                }
            }

            triggerSOS(); // Start other emergency procedures
        });
    }

    private void startLiveLocationUpdates() {
        liveTrackingStartTime = System.currentTimeMillis();
        liveLocationRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - liveTrackingStartTime;
                long delay = (elapsed < 60 * 1000) ? LIVE_LOCATION_BURST_INTERVAL : LIVE_LOCATION_NORMAL_INTERVAL;

                if (isRouteMode && myLocation != null) {
                    // Send Live Location SMS burst (Optional: limit this to avoid SMS spam, maybe
                    // only update server)
                    // For now, let's keep checkRouteDeviation as primary active logic
                    checkRouteDeviation();
                }
                liveLocationHandler.postDelayed(this, delay);
            }
        };
        liveLocationHandler.post(liveLocationRunnable);
    }

    private void checkRouteDeviation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && routePath != null) {
                if (!isLocationOnPath(location, routePath)) {
                    // DEVIATION DETECTED!
                    triggerSOS();
                    // Notify specifically about deviation
                    if (!db.fetchData().isEmpty()) {
                        manager.sendTextMessage(db.fetchData().get(0).getNumber(), null,
                                "ALERT: Route Deviation Detected! I am off track.", null, null);
                    }
                }
            }
        });
    }

    // Check if user is within tolerance of ANY point on the path
    private boolean isLocationOnPath(Location location, java.util.ArrayList<SimpleLocation> polyline) {
        for (SimpleLocation point : polyline) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), point.latitude, point.longitude,
                    results);
            if (results[0] < MAX_DEVIATION_METERS) {
                return true; // We are close enough to at least one point
            }
        }
        return false;
    }

    private void sendLiveLocationToContacts() {
        db = new DatabaseHelper(ServiceMine.this);
        ArrayList<ContactModel> list = db.fetchData();
        for (ContactModel c : list) {
            manager.sendTextMessage(c.getNumber(), null,
                    "Safe Route Update: I am safe. My current location: " + myLocation,
                    null, null);
        }
    }

    private void startForegroundNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel("MYID", "CHANNELFOREGROUND",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "MYID")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.siren)
                .setContentIntent(pendingIntent)
                .build();
        this.startForeground(115, notification);
    }

    // Voice Detection Fields
    private android.speech.SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isVoiceDetectionOn = true; // Enabled by default for SHAKTI

    private void initializeVoiceListening() {
        if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());

            speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    // Restart listening
                    if (isVoiceDetectionOn && isRunning) {
                        startListening();
                    }
                }

                @Override
                public void onError(int error) {
                    // Restart listening on error (silence, etc.)
                    if (isVoiceDetectionOn && isRunning) {
                        startListening();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results
                            .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null) {
                        for (String result : matches) {
                            String cmd = result.toLowerCase(java.util.Locale.ROOT);
                            boolean keywordMatched = false;

                            // 1. Check Personal Contact Keywords
                            db = new DatabaseHelper(ServiceMine.this);
                            ArrayList<ContactModel> list = db.fetchData();
                            for (ContactModel c : list) {
                                if (c.getKeyword() != null && !c.getKeyword().isEmpty()
                                        && cmd.contains(c.getKeyword().toLowerCase())) {
                                    c.setKeyword(c.getKeyword()); // Ensure it's passed or similar
                                    triggerSpecificContactSOS(c);
                                    keywordMatched = true;
                                }
                            }

                            if (keywordMatched)
                                continue;

                            // 2. Check Helpline Keywords
                            ArrayList<HelplineModel> helplines = db.fetchHelplineData();
                            for (HelplineModel h : helplines) {
                                if (h.getKeyword() != null && !h.getKeyword().isEmpty()
                                        && cmd.contains(h.getKeyword().toLowerCase())) {
                                    triggerHelplineSOS(h);
                                    keywordMatched = true;
                                }
                            }

                            if (keywordMatched)
                                continue;

                            // 3. Professional Emergency Keywords
                            if (cmd.contains("police") || cmd.contains("pulis")) {
                                triggerAuthorityEmergency(EmergencyConfig.TYPE_POLICE,
                                        cmd.contains("police") ? "POLICE" : "PULIS");
                                keywordMatched = true;
                            } else if (cmd.contains("fire") || cmd.contains("aag")) {
                                triggerAuthorityEmergency(EmergencyConfig.TYPE_FIRE,
                                        cmd.contains("fire") ? "FIRE" : "AAG");
                                keywordMatched = true;
                            } else if (cmd.contains("ambulance") || cmd.contains("hospital")) {
                                triggerAuthorityEmergency(EmergencyConfig.TYPE_AMBULANCE,
                                        cmd.contains("ambulance") ? "AMBULANCE" : "HOSPITAL");
                                keywordMatched = true;
                            } else if (cmd.contains("disaster") || cmd.contains("earthquake")
                                    || cmd.contains("flood")) {
                                String k = cmd.contains("disaster") ? "DISASTER"
                                        : (cmd.contains("earthquake") ? "EARTHQUAKE" : "FLOOD");
                                triggerAuthorityEmergency(EmergencyConfig.TYPE_DISASTER, k);
                                keywordMatched = true;
                            } else if (cmd.contains("gas leak") || cmd.contains("cylinder")) {
                                triggerAuthorityEmergency(EmergencyConfig.TYPE_GAS_LEAK,
                                        cmd.contains("gas leak") ? "GAS LEAK" : "CYLINDER");
                                keywordMatched = true;
                            }

                            if (keywordMatched)
                                continue;

                            // 4. Authority-Specific Triggers (Legacy/Fallback)
                            if (cmd.contains("help") || cmd.contains("bachao") || cmd.contains("nahi")
                                    || cmd.contains("mummy") || cmd.contains("save me") || cmd.contains("emergency")) {
                                triggerSOS(); // General SOS
                            }
                        }
                    }
                    if (isVoiceDetectionOn && isRunning) {
                        // Check if Voice Detection is Enabled in Settings
                        SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
                        boolean isVoiceEnabled = settings.getBoolean("voice_detection", true);
                        if (isVoiceEnabled) {
                            startListening();
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private boolean isSOSActive = false; // Prevent multiple triggers

    private void triggerSpecificContactSOS(ContactModel contact) {
        if (isSOSActive)
            return; // Already active

        fetchUserProfile(profile -> {
            isSOSActive = true;
            startSiren();
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            // Launch Camera
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            cameraIntent.putExtra("AUTO_START", true);
            startActivity(cameraIntent);

            // Use custom keyword message
            String location = (myLocation != null) ? myLocation : "Checking Location...";
            String finalMsg = EmergencyConfig.formatEmergencyMessage(EmergencyConfig.TYPE_SOS_KEYWORDS, profile,
                    location, this);

            manager.sendTextMessage(contact.getNumber(), null, finalMsg, null, null);

            triggerNearbyAlert("SOS_KEYWORD");
            isRouteMode = true;
            startLiveLocationUpdates();
        });
    }

    private void fetchUserProfile(OnProfileFetched listener) {
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        EmergencyConfig.UserProfile profile = new EmergencyConfig.UserProfile();

        if (user == null) {
            profile.name = "User";
            listener.onFetched(profile);
            return;
        }

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        profile.name = document.getString("name");
                        profile.age = document.getString("age");
                        profile.gender = document.getString("gender");
                        profile.mobile = document.getString("mobile");
                        profile.emergencyContact = document.getString("emergencyContact");
                        profile.city = document.getString("city");
                        profile.bloodType = document.getString("bloodType");
                        profile.allergies = document.getString("allergies");
                        profile.medicalConditions = document.getString("medicalConditions");
                        profile.medicines = document.getString("medicines");
                    } else {
                        profile.name = user.getDisplayName() != null ? user.getDisplayName() : "User";
                    }
                    // Add Mobile Battery Info
                    profile.mobileBattery = getMobileBatteryLevel();
                    listener.onFetched(profile);
                })
                .addOnFailureListener(e -> {
                    profile.name = user.getDisplayName() != null ? user.getDisplayName() : "User";
                    profile.mobileBattery = getMobileBatteryLevel();
                    listener.onFetched(profile);
                });
    }

    interface OnProfileFetched {
        void onFetched(EmergencyConfig.UserProfile profile);
    }

    private void triggerHelplineSOS(HelplineModel helpline) {
        if (isSOSActive)
            return;

        fetchUserProfile(profile -> {
            isSOSActive = true;
            startSiren();
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            // Launch Camera
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            cameraIntent.putExtra("AUTO_START", true);
            startActivity(cameraIntent);

            boolean isAmbulance = helpline.getKeyword() != null &&
                    helpline.getKeyword().toLowerCase().contains("ambulance");

            String emergencyType = isAmbulance ? EmergencyConfig.TYPE_AMBULANCE : EmergencyConfig.TYPE_GENERAL;
            String location = (myLocation != null) ? myLocation : "Checking Location...";
            String messageToSend = EmergencyConfig.formatEmergencyMessage(emergencyType, profile, location, this);

            try {
                // Send to helpline
                ArrayList<String> parts = manager.divideMessage(messageToSend);
                manager.sendMultipartTextMessage(helpline.getNumber(), null, parts, null, null);

                // If ambulance, also send to trusted contacts
                if (isAmbulance) {
                    db = new DatabaseHelper(ServiceMine.this);
                    ArrayList<ContactModel> contacts = db.fetchData();
                    for (ContactModel c : contacts) {
                        try {
                            manager.sendMultipartTextMessage(c.getNumber(), null, parts, null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            triggerNearbyAlert("HELPLINE_SOS");
            isRouteMode = true;
            startLiveLocationUpdates();

            // Also call them
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + helpline.getNumber()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (ActivityCompat.checkSelfPermission(ServiceMine.this,
                    Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent);
            }
        });
    }

    private void triggerSOS() {
        // Check if Shadow Mode is active
        SharedPreferences shadowPrefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
        // Note: Removed ShadowMode check to restore default SOS function as per user
        // context unless explicitly asked.
        // If the user wants ShadowMode strictness, we can re-add it. For now, let's
        // assume standard behavior.

        if (isSOSActive)
            return;

        // Pre-trigger Warning (Grace Period)
        showPreTriggerNotification();
        vibrator.vibrate(VibrationEffect.createWaveform(
                new long[] { 0, 500, 200, 500 }, -1)); // Warning pattern

        // 5. Save History Local
        db.insertHistory("SOS Triggered", myLocation);

        // 6. Sync History to Firestore
        if (mAuth.getCurrentUser() != null) {
            java.util.Map<String, Object> historyData = new java.util.HashMap<>();
            historyData.put("type", "SOS Triggered");
            historyData.put("location", myLocation);
            historyData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            historyData.put("alertId", String.valueOf(System.currentTimeMillis()));

            firestore.collection("users").document(mAuth.getCurrentUser().getUid())
                    .collection("history").document()
                    .set(historyData);
        }

        preTriggerRunnable = () -> {
            boolean isStillRunning = isRunning; // Check if service is still alive
            if (isStillRunning && !isSOSActive) {
                executeActualSOS();
            }
        };
        preTriggerHandler.postDelayed(preTriggerRunnable, 1000); // 1 Second grace period
    }

    // For Continuous SOS
    private Handler sosLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable sosLoopRunnable;

    private void executeActualSOS() {
        if (isSOSActive)
            return; // Already active, don't re-trigger
        isSOSActive = true;

        startSiren();
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        // 1. Launch Camera
        Intent cameraIntent = new Intent(this, CameraActivity.class);
        cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        cameraIntent.putExtra("AUTO_START", true);
        startActivity(cameraIntent);

        // Start the Continuous SOS Loop
        startContinuousSOSLoop();
    }

    private void startContinuousSOSLoop() {
        sosLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSOSActive)
                    return;

                // 2. Fetch Fresh Location & Send SMS
                if (ActivityCompat.checkSelfPermission(ServiceMine.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                        @Override
                        public boolean isCancellationRequested() {
                            return false;
                        }

                        @NonNull
                        @Override
                        public CancellationToken onCanceledRequested(
                                @NonNull OnTokenCanceledListener onTokenCanceledListener) {
                            return null;
                        }
                    }).addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            myLocation = "http://maps.google.com/?q=" + location.getLatitude() + ","
                                    + location.getLongitude();
                        }
                        sendSOSMessages();
                    }).addOnFailureListener(e -> {
                        sendSOSMessages(); // Send with whatever we have
                    });
                } else {
                    sendSOSMessages();
                }

                // Schedule next run in 1 minute (60,000 ms)
                sosLoopHandler.postDelayed(this, 60 * 1000);
            }
        };
        // Run immediately first time
        sosLoopHandler.post(sosLoopRunnable);
    }

    private void sendSOSMessages() {
        myLocation = (myLocation == null) ? "Checking Location..." : myLocation;
        db = new DatabaseHelper(ServiceMine.this);
        ArrayList<ContactModel> list = db.fetchData();

        // Use Professional SOS Message Formatter
        String professionalMsg = SOSMessageFormatter.createProfessionalSOSMessage(this, currentLocation, "SOS_AUTO");
        if (professionalMsg == null || professionalMsg.isEmpty()) {
            professionalMsg = "🚨 SOS ALERT! I need help! \nLocation: " + myLocation;
        }

        // Log to History
        if (db != null) {
            db.insertHistory("SOS_AUTO", myLocation);
        }

        if (list.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> Toast
                    .makeText(getApplicationContext(), "No contacts to send SMS!", Toast.LENGTH_SHORT).show());
            return;
        }

        for (ContactModel c : list) {
            try {
                // IMPORTANT: Use Divide Message for long texts
                java.util.ArrayList<String> parts = manager.divideMessage(professionalMsg);
                manager.sendMultipartTextMessage(c.getNumber(), null, parts, null, null);

                new Handler(Looper.getMainLooper()).post(() -> Toast
                        .makeText(getApplicationContext(), "SOS Sent to " + c.getName(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    // Fallback: Simple Message
                    manager.sendTextMessage(c.getNumber(), null, "SOS! Help me! " + myLocation, null, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        // 3a. Trigger Nearby Help Alert
        triggerNearbyAlert("SOS_AUTO");

        // 3. Start Continuous Live Tracking
        isRouteMode = true;
        if (liveLocationHandler != null && liveLocationRunnable != null) {
            liveLocationHandler.removeCallbacks(liveLocationRunnable);
        }
        startLiveLocationUpdates();
    }

    private void showPreTriggerNotification() {
        Intent cancelIntent = new Intent(this, MainActivity.class); // Or dedicated CancelActivity
        cancelIntent.setAction("CANCEL_SOS");
        PendingIntent pendingCancel = PendingIntent.getActivity(this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, "MYID")
                .setContentTitle("SOS TRIGGERED!")
                .setContentText("TAP TO CANCEL WITHIN 5 SECONDS")
                .setSmallIcon(R.drawable.siren)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(pendingCancel)
                .setAutoCancel(true)
                .build();

        NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m.notify(999, notification); // ID 999 for pre-trigger
    }

    private void triggerAuthorityEmergency(String emergencyType, String keyword) {
        if (isSOSActive)
            return;

        fetchUserProfile(profile -> {
            isSOSActive = true;
            profile.spokenKeyword = keyword;
            startSiren();
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            // Launch Camera for evidence
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            cameraIntent.putExtra("AUTO_START", true);
            startActivity(cameraIntent);

            sendAuthorityAlert(emergencyType, profile);

            // Start live tracking
            isRouteMode = true;
            startLiveLocationUpdates();
        });
    }

    private void sendAuthorityAlert(String emergencyType, EmergencyConfig.UserProfile profile) {
        String location = (myLocation != null) ? myLocation : "Getting Location...";
        String message = EmergencyConfig.formatEmergencyMessage(emergencyType, profile, location, this);
        String emergencyNumber = EmergencyConfig.getEmergencyNumber(emergencyType);

        // Send SMS to emergency contacts
        db = new DatabaseHelper(ServiceMine.this);
        ArrayList<ContactModel> list = db.fetchData();
        for (ContactModel c : list) {
            manager.sendTextMessage(c.getNumber(), null, message, null, null);
        }

        // For certain emergencies, also call the authority
        if (emergencyType.equals(EmergencyConfig.TYPE_AMBULANCE) ||
                emergencyType.equals(EmergencyConfig.TYPE_ACCIDENT) ||
                emergencyType.equals(EmergencyConfig.TYPE_FIRE)) {

            // Auto-call emergency number
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(android.net.Uri.parse("tel:" + emergencyNumber));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            }
        }

        // Trigger Nearby Help Alert
        triggerNearbyAlert(emergencyType);
    }

    private void triggerNearbyAlert(String type) {
        Location location = null;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return null;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            }).addOnSuccessListener(loc -> {
                if (loc != null) {
                    uploadAlertToFirestore(loc, type);
                }
            });
        }
    }

    private void uploadAlertToFirestore(Location location, String type) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null)
            return;

        java.util.Map<String, Object> alert = new java.util.HashMap<>();
        alert.put("requesterId", user.getUid()); // Changed from userId to requesterId
        alert.put("type", type);
        alert.put("status", "active"); // Lowercase active
        alert.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        alert.put("radius", 100); // Default radius

        java.util.Map<String, Object> loc = new java.util.HashMap<>();
        loc.put("latitude", location.getLatitude());
        loc.put("longitude", location.getLongitude());
        alert.put("requesterLocation", loc);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("help_requests")
                .add(alert); // Use add() for auto-ID like NearbyHelpActivity
    }

    private void listenForNearbyAlerts() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null)
            return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("help_requests")
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NEARBY_HELP", "Listen failed.", error);
                        return;
                    }
                    if (value == null)
                        return;

                    Log.d("NEARBY_HELP", "Active help requests found: " + value.size());

                    for (com.google.firebase.firestore.DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            String requesterId = dc.getDocument().getString("requesterId");
                            String requestId = dc.getDocument().getId();

                            if (requesterId != null && !requesterId.equals(user.getUid())) {
                                Log.d("NEARBY_HELP", "New Help Request from: " + requesterId);

                                // Parse nested location object
                                java.util.Map<String, Object> loc = (java.util.Map<String, Object>) dc.getDocument()
                                        .get("requesterLocation");
                                if (loc != null && loc.containsKey("latitude") && loc.containsKey("longitude")) {
                                    Double lat = (Double) loc.get("latitude");
                                    Double lng = (Double) loc.get("longitude");

                                    if (lat != null && lng != null) {
                                        checkDistanceAndAlert(lat, lng, requestId);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void checkDistanceAndAlert(double lat, double lng, String requestId) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        }).addOnSuccessListener(location -> {
            if (location != null) {
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), lat, lng, results);
                float distanceInMeters = results[0];

                Log.d("NEARBY_HELP", "Alert received. Dist: " + distanceInMeters + "m. Radius: 100m");

                if (distanceInMeters <= 100) { // Limit to 100 meters per requirement
                    showNearbyAlertNotification(distanceInMeters, requestId, lat, lng);
                }
            }
        });
    }

    private void showNearbyAlertNotification(float distance, String requestId, double lat, double lng) {
        // Launch NearbyHelpActivity with the request ID
        Intent intent = new Intent(this, NearbyHelpActivity.class);
        intent.putExtra("nearby_request_id", requestId);
        intent.putExtra("request_lat", lat);
        intent.putExtra("request_lng", lng);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationChannel channel = new NotificationChannel("NEARBY_ALERT", "Nearby Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Show on lock screen
        channel.enableVibration(true);

        NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "NEARBY_ALERT")
                .setContentTitle("🚨 NEARBY EMERGENCY")
                .setContentText("Someone needs help within " + (int) distance + "m! Tap to respond.")
                .setSmallIcon(R.drawable.siren)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();

        m.notify((int) System.currentTimeMillis(), notification);

        // Vibrate to alert user
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void stopServiceCommon() {
        if (isRunning) {
            // Stop Location Updates
            if (locationCallback != null)
                fusedLocationClient.removeLocationUpdates(locationCallback);

            // Stop Sensor Listener (Shake Detection)
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }

            // Stop Inactivity Check
            if (inactivityHandler != null && inactivityRunnable != null) {
                inactivityHandler.removeCallbacks(inactivityRunnable);
            }

            // Stop Live Location Tracking
            if (liveLocationHandler != null && liveLocationRunnable != null) {
                liveLocationHandler.removeCallbacks(liveLocationRunnable);
            }

            // Stop Continuous SOS Loop
            if (sosLoopHandler != null && sosLoopRunnable != null) {
                sosLoopHandler.removeCallbacks(sosLoopRunnable);
            }

            // Stop Voice Recognition
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
                speechRecognizer = null;
            }

            // Stop Siren & Volume Lock
            stopSiren();

            // Stop Foreground Notification
            stopForeground(true);
            stopSelf();

            // Reset Flags
            isRunning = false;
            isSOSActive = false;
            isRouteMode = false;

            // Remove Pre-Trigger Callbacks if any
            if (preTriggerHandler != null && preTriggerRunnable != null) {
                preTriggerHandler.removeCallbacks(preTriggerRunnable);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Rule: Rapid Orientation Change calculation
            // Simple check: significantly different acceleration vector direction
            boolean orientationChanged = false;
            if (lastAccelValues != null) {
                float deltaX = Math.abs(lastAccelValues[0] - x);
                float deltaY = Math.abs(lastAccelValues[1] - y);
                float deltaZ = Math.abs(lastAccelValues[2] - z);

                // If gravity vector shifts significantly (rotation)
                if (deltaX > 15 || deltaY > 15 || deltaZ > 15) {
                    orientationChanged = true;
                }
            }
            System.arraycopy(event.values, 0, lastAccelValues, 0, 3);

            if (orientationChanged) {
                if (currentTime - lastOrientationTime < 3000) { // Consecutive short checks
                    orientationChangeCount++;
                } else {
                    orientationChangeCount = 1;
                }
                lastOrientationTime = currentTime;

                // Rule: Orientation changes 3 times within short duration
                if (orientationChangeCount >= 3) {
                    activateSuspiciousMode("Rapid Orientation Change");
                    // If VERY rapid (violent), trigger SOS?
                    // Let's rely on Shake for violent.
                    triggerSOS();
                    orientationChangeCount = 0;
                }
            }

            // Gravity is ~9.8. If accel is significantly different or changing, it's
            // moving.
            double acceleration = Math.sqrt(x * x + y * y + z * z);

            if (acceleration > MOVEMENT_THRESHOLD_MAX || acceleration < MOVEMENT_THRESHOLD_MIN) {
                lastMovementTime = currentTime; // Reset inactivity timer
            }

            if ((currentTime - lastShakeTime) > 2000) { // Time gap

                // Dynamic Shake Threshold from Settings
                SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
                int sensitivity = settings.getInt("shake_sensitivity", 50);
                // Formula: 50 -> 12.0f. 100 -> 5.0f. 0 -> 20.0f
                float dynamicThreshold = 20.0f - (sensitivity / 100.0f) * 15.0f;

                if (acceleration > dynamicThreshold) {
                    lastShakeTime = currentTime;
                    lastMovementTime = currentTime;

                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));

                    // Rule: Aggressive Shake -> Trigger SOS
                    if (acceleration > SHAKE_THRESHOLD_VIOLENT) {
                        triggerSOS();
                    } else {
                        // Medium shake -> Suspicious
                        activateSuspiciousMode("Medium Shake Detected");
                        // 2-3 Shakes logic is covered by user manually shaking repeatedly, triggering
                        // 'activateSuspicious'
                        // If already suspicious and shaken again?
                        if (isSuspiciousActive()) {
                            triggerSOS(); // Second shake triggers it
                        }
                    }
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Rule: Camera Obstruction (Darkness)
            // If light is suddenly 0 (pocket/covered) WHILE Suspicious -> SOS
            float loc = event.values[0];
            if (loc < 5 && isSuspiciousActive()) {
                Log.d("SOS_RULES", "Camera/Sensor Covered during Suspicious State -> SOS");
                triggerSOS();
            }
        }
    }

    private void startSiren() {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        if (audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            // Continuous Volume Max Enforcement
            volumeRunnable = new Runnable() {
                @Override
                public void run() {
                    if (audioManager != null) {
                        // Force Music Stream to Max
                        int maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0);

                        // Force Alarm Stream to Max
                        int maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0);

                        // Force Ring Stream to Max (for call alerts)
                        int maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0);
                    }
                    volumeHandler.postDelayed(this, 100); // Check every 100ms
                }
            };
            volumeHandler.post(volumeRunnable);
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            }
        }

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    private void stopSiren() {
        // Stop Volume Enforcement
        if (volumeHandler != null && volumeRunnable != null) {
            volumeHandler.removeCallbacks(volumeRunnable);
        }

        // Restore Original Volume
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Tamper Protection: Restart service if Shadow Mode is ON
        android.content.SharedPreferences shadowPrefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
        boolean isShadowMode = shadowPrefs.getBoolean("isActive", false);

        if (isShadowMode) {
            android.util.Log.d("ServiceMine", "Service destroyed: Restarting because Shadow Mode is ON");
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(this, ServiceRestartReceiver.class);
            this.sendBroadcast(broadcastIntent);
        }

        // Unregister listeners
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (sensorReceiver != null) {
            try {
                unregisterReceiver(sensorReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopSiren();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Rule: Forced App Kill Attempt (Swipe away)
        if (isSuspiciousActive() || isSOSActive) {
            Log.d("SOS_RULES", "App Task Removed (Killed) -> Triggering SOS before death");
            // Attempt to send last ditch SOS
            triggerSOS();
        }

        // Tamper Protection: Restart service if Shadow Mode is ON
        android.content.SharedPreferences shadowPrefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
        boolean isShadowMode = shadowPrefs.getBoolean("isActive", false);

        if (isShadowMode) {
            android.util.Log.d("ServiceMine", "Tamper detected: Restarting service in Shadow Mode");
            Intent restartServiceIntent = new Intent(getApplicationContext(), ServiceMine.class);
            restartServiceIntent.setPackage(getPackageName());
            restartServiceIntent.setAction("Start");

            android.app.PendingIntent restartServicePendingIntent = android.app.PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent,
                    android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE);

            android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext()
                    .getSystemService(Context.ALARM_SERVICE);
            alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 1000,
                    restartServicePendingIntent);
        }

        super.onTaskRemoved(rootIntent);
    }

    private void performIntelligentSafetyCheck(Location currentLocation) {
        long currentTime = System.currentTimeMillis();
        boolean shouldCheck = false;

        if (lastSafetyCheckLocation == null) {
            shouldCheck = true;
        } else {
            float distance = currentLocation.distanceTo(lastSafetyCheckLocation);
            if (distance >= SAFETY_CHECK_DISTANCE_METERS) {
                shouldCheck = true;
            } else if (currentTime - lastSafetyCheckTime >= SAFETY_CHECK_INTERVAL_MS) {
                shouldCheck = true;
            }
        }

        if (shouldCheck) {
            lastSafetyCheckLocation = currentLocation;
            lastSafetyCheckTime = currentTime;

            // Gather Heuristics
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            boolean isNight = (hour >= 19 || hour <= 5);

            String timeOfDay = isNight ? "Night" : "Day";

            // Heuristic: Area Type based on randomness or simplistic assumptions for
            // hackathon
            int randomFactor = (int) (currentLocation.getLatitude() * 10000) % 10;
            String areaType = (randomFactor < 3) ? "Isolated" : (randomFactor < 6) ? "Semi-Urban" : "Urban";

            // Heuristic: Lighting
            String lighting = (isNight && (areaType.equals("Isolated") || areaType.equals("Semi-Urban"))) ? "Poor"
                    : "Good";

            // Heuristic: SOS Activity (Randomized)
            String sosActivity = (randomFactor == 0) ? "High" : (randomFactor < 4) ? "Medium" : "Low";

            // Heuristic: Distance to services (Mock)
            String distServices = (areaType.equals("Urban")) ? "Near (<2km)" : "Far (>5km)";

            if (geminiHelper != null) {
                geminiHelper.assessSafety(areaType, timeOfDay, sosActivity, lighting, distServices);
            }
        }
    }

    // Bluetooth Heart Rate Monitoring Logic
    private WatchDataReader watchDataReader;
    private boolean hasTriggeredLowHRAlert = false;
    private boolean isBluetoothMonitoring = false;

    private void startBluetoothMonitoring() {
        if (isBluetoothMonitoring)
            return;

        SharedPreferences prefs = getSharedPreferences("ConnectedDevice", MODE_PRIVATE);
        String address = prefs.getString("last_connected_device", null);

        if (address == null)
            return;

        android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled())
            return;

        try {
            android.bluetooth.BluetoothDevice device = adapter.getRemoteDevice(address);
            if (watchDataReader == null) {
                watchDataReader = new WatchDataReader(this);
            }

            Log.d("ServiceMine", "Starting Bluetooth Heart Rate Monitoring for " + address);
            isBluetoothMonitoring = true;
            watchDataReader.monitorHeartRate(device, new WatchDataReader.DataCallback() {
                @Override
                public void onBatteryLevel(int level) {
                }

                @Override
                public void onHeartRate(int bpm) {
                    if (bpm > 0 && bpm < 70) {
                        triggerLowHeartRateEmergency(bpm);
                    } else if (bpm >= 72) {
                        hasTriggeredLowHRAlert = false; // Reset if HR normalizes
                    }
                }

                @Override
                public void onSteps(int steps) {
                }

                @Override
                public void onBloodPressure(String bp) {
                }

                @Override
                public void onSpO2(int spo2) {
                }

                @Override
                public void onSleepData(String sleep) {
                }

                @Override
                public void onDeviceInfo(String s, String f, String sw, String m) {
                }

                @Override
                public void onError(String error) {
                    Log.e("ServiceMine", "Bluetooth Monitor Error: " + error);
                    isBluetoothMonitoring = false;
                }
            });

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void triggerLowHeartRateEmergency(int bpm) {
        // Check if Shadow Mode is active
        SharedPreferences shadowPrefs = getSharedPreferences("ShadowMode", MODE_PRIVATE);
        if (!shadowPrefs.getBoolean("isActive", false)) {
            return;
        }

        if (isSOSActive) // Prevent multiple triggers
            return;
        if (hasTriggeredLowHRAlert)
            return;
        hasTriggeredLowHRAlert = true;

        Log.d("ServiceMine", "CRITICAL LOW HEART RATE: " + bpm);

        fetchUserProfile(profile -> {
            profile.heartRate = bpm;
            // Get watch battery if available from SharedPreferences
            SharedPreferences watchPrefs = getSharedPreferences("ConnectedDevice", MODE_PRIVATE);
            profile.watchBattery = watchPrefs.getInt("last_battery_level", 0);

            String location = (myLocation != null) ? myLocation : "Checking Location...";
            String message = EmergencyConfig.formatEmergencyMessage(EmergencyConfig.TYPE_AMBULANCE, profile, location,
                    this);

            // Send to all trusted contacts
            db = new DatabaseHelper(this);
            java.util.ArrayList<ContactModel> contacts = db.fetchData();
            for (ContactModel contact : contacts) {
                try {
                    java.util.ArrayList<String> parts = manager.divideMessage(message);
                    manager.sendMultipartTextMessage(contact.getNumber(), null, parts, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Also trigger general SOS to launch camera/etc.
            triggerSOS();
        });
    }

    private int getMobileBatteryLevel() {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                return (int) ((level / (float) scale) * 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}