package com.example.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker; // This Marker is for OSM, not Mapbox

import java.util.HashMap;
import java.util.Map;

public class NearbyMapActivity extends AppCompatActivity {

    private static final String MAPTILER_KEY = "nUEBje1Fkb1ikcm8SyrI";
    private static final String MAP_STYLE_URL = "https://api.maptiler.com/maps/streets/style.json?key=" + MAPTILER_KEY;

    // Maps
    private com.mapbox.mapboxsdk.maps.MapView mapLibreView;
    private MapboxMap mapLibreMap;
    private MapView mapOsmView;

    // Data
    private String requestId;
    private boolean isRequester;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ListenerRegistration liveLocationListener;

    // State
    private boolean isOnline = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    // Markers
    private final Map<String, com.mapbox.mapboxsdk.annotations.Marker> mapLibreMarkers = new HashMap<>(); // ID ->
                                                                                                          // Marker
    private final Map<String, org.osmdroid.views.overlay.Marker> osmMarkers = new HashMap<>(); // ID -> Marker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSM Configuration
        Configuration.getInstance().load(getApplicationContext(),
                android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        // MapLibre (Mapbox Legacy) Configuration
        Mapbox.getInstance(this);

        setContentView(R.layout.activity_nearby_map);

        requestId = getIntent().getStringExtra("requestId");
        isRequester = getIntent().getBooleanExtra("isRequester", false);

        if (requestId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMaps(savedInstanceState);
        setupEndHelpButton();

        startNetworkMonitoring();
    }

    private void initMaps(Bundle savedInstanceState) {
        // Init MapLibre (Mapbox)
        mapLibreView = findViewById(R.id.mapLibreView);
        mapLibreView.onCreate(savedInstanceState);
        mapLibreView.getMapAsync(map -> {
            mapLibreMap = map;
            map.setStyle(MAP_STYLE_URL, new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
            map.getUiSettings().setCompassEnabled(true);
            updateMapVisibility();
        });

        // Init OSMDroid
        mapOsmView = findViewById(R.id.mapOsmView);
        mapOsmView.setMultiTouchControls(true);
        mapOsmView.getController().setZoom(15.0);
    }

    @SuppressWarnings({ "MissingPermission" })
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponent locationComponent = mapLibreMap.getLocationComponent();
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    private void startNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    isOnline = true;
                    updateMapVisibility();
                    Toast.makeText(NearbyMapActivity.this, "Online Mode: High Precision Map", Toast.LENGTH_SHORT)
                            .show();
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    isOnline = false;
                    updateMapVisibility();
                    Toast.makeText(NearbyMapActivity.this, "Offline Mode: Using Offline Map", Toast.LENGTH_SHORT)
                            .show();
                });
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);

        // Initial Check
        isOnline = isNetworkAvailable();
        updateMapVisibility();
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null)
            return false;
        NetworkCapabilities capabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void updateMapVisibility() {
        if (isOnline) {
            mapLibreView.setVisibility(android.view.View.VISIBLE);
            mapOsmView.setVisibility(android.view.View.GONE);
        } else {
            mapLibreView.setVisibility(android.view.View.GONE);
            mapOsmView.setVisibility(android.view.View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapLibreView.onStart();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationSharing();
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
        }
        listenToSessionUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapLibreView.onResume();
        mapOsmView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapLibreView.onPause();
        mapOsmView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapLibreView.onStop();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapLibreView.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        if (liveLocationListener != null) {
            liveLocationListener.remove();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapLibreView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapLibreView.onSaveInstanceState(outState);
    }

    private void startLocationSharing() {
        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location loc = locationResult.getLastLocation();
                    updateMyLocationInFirestore(loc);

                    // Update Map Centers
                    if (isOnline && mapLibreMap != null) {
                        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(loc.getLatitude(), loc.getLongitude()), 15));
                    } else if (!isOnline) {
                        mapOsmView.getController().setCenter(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateMyLocationInFirestore(Location location) {
        if (mAuth.getCurrentUser() == null)
            return;

        Map<String, Object> locData = new HashMap<>();
        locData.put("latitude", location.getLatitude());
        locData.put("longitude", location.getLongitude());
        locData.put("updatedAt", FieldValue.serverTimestamp());
        locData.put("userId", mAuth.getCurrentUser().getUid());

        db.collection("active_help_sessions").document(requestId)
                .collection("live_locations").document(mAuth.getCurrentUser().getUid())
                .set(locData);
    }

    private void listenToSessionUpdates() {
        liveLocationListener = db.collection("active_help_sessions").document(requestId)
                .collection("live_locations")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null)
                        return;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.equals(mAuth.getCurrentUser().getUid()))
                            continue;

                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (lat != null && lng != null) {
                            updateMarker(userId, lat, lng);
                        }
                    }

                    // Update Counts (Approximation)
                    int nearby = value.size() - 1; // Exclude self
                    TextView tvCount = findViewById(R.id.tvNearbyCount);
                    if (tvCount != null)
                        tvCount.setText(String.valueOf(nearby));
                });
    }

    private void updateMarker(String userId, double lat, double lng) {
        // Update MapLibre Marker
        if (mapLibreMap != null) {
            LatLng pos = new LatLng(lat, lng);
            if (mapLibreMarkers.containsKey(userId)) {
                mapLibreMarkers.get(userId).setPosition(pos);
            } else {
                com.mapbox.mapboxsdk.annotations.Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("User"));
                mapLibreMarkers.put(userId, marker);
            }
        }

        // Update OSM Marker
        GeoPoint osmPos = new GeoPoint(lat, lng);
        if (osmMarkers.containsKey(userId)) {
            osmMarkers.get(userId).setPosition(osmPos);
        } else {
            org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(mapOsmView);
            marker.setPosition(osmPos);
            marker.setTitle("User");
            marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                    org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            mapOsmView.getOverlays().add(marker);
            osmMarkers.put(userId, marker);
        }
        mapOsmView.invalidate();
    }

    private void setupEndHelpButton() {
        MaterialButton btnEndHelp = findViewById(R.id.btnEndHelp);
        btnEndHelp.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("End Help Session?")
                    .setMessage("This will stop live tracking.")
                    .setPositiveButton("End", (dialog, which) -> endHelpSession())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void endHelpSession() {
        db.collection("active_help_sessions").document(requestId)
                .update("status", "ended")
                .addOnSuccessListener(aVoid -> finish());
    }
}
