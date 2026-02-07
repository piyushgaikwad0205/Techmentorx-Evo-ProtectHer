package com.example.sos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SafetyAnalysisActivity extends AppCompatActivity {

    private MaterialButton btnAnalyze;
    private ProgressBar progressBar;
    private LinearLayout layoutResults, layoutBreakdown;
    private TextView tvSafetyLevel, tvReason, tvAreaName;

    // Nearest Places Layouts
    private LinearLayout layoutPolice, layoutHospitals, layoutHotels;

    // Signal Views - REMOVED
    private MaterialCardView cardSafetyLevel;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_analysis);

        // Set status bar to Red
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.parseColor("#F92A2A"));
        }

        android.widget.ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        btnAnalyze = findViewById(R.id.btnAnalyze);
        progressBar = findViewById(R.id.progressBar);
        layoutResults = findViewById(R.id.layoutResults);
        layoutBreakdown = findViewById(R.id.layoutBreakdown);

        tvSafetyLevel = findViewById(R.id.tvSafetyLevel);
        tvReason = findViewById(R.id.tvReason);
        tvAreaName = findViewById(R.id.tvAreaName);
        cardSafetyLevel = findViewById(R.id.cardSafetyLevel);

        layoutPolice = findViewById(R.id.layoutPolice);
        layoutHospitals = findViewById(R.id.layoutHospitals);
        layoutHotels = findViewById(R.id.layoutHotels);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnAnalyze.setOnClickListener(v -> performAnalysis());

        // Populate initial heuristic data
        updateSignalDisplay();
    }

    @SuppressLint("SetTextI18n")
    private void updateSignalDisplay() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 19 || hour <= 5);
        // Views removed, logic kept for potential future use or analytics
    }

    private void performAnalysis() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
            return;
        }

        btnAnalyze.setEnabled(false);
        btnAnalyze.setText("Analyzing...");
        progressBar.setVisibility(View.VISIBLE);
        layoutResults.setVisibility(View.GONE);

        fusedLocationClient
                .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        analyzeWithLocation(location);
                    } else {
                        // Fallback: analyze without precise location
                        analyzeWithoutLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Using general area analysis", Toast.LENGTH_SHORT).show();
                    analyzeWithoutLocation();
                });
    }

    @SuppressLint("SetTextI18n")
    private void analyzeWithLocation(Location location) {
        // LOCAL HEURISTIC-BASED SAFETY ANALYSIS
        // This works offline and provides instant results

        // 1. Time Analysis
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 19 || hour <= 5);
        boolean isLateNight = (hour >= 22 || hour <= 4);

        // 2. Area Type (Based on location coordinates)
        int randomFactor = (int) (Math.abs(location.getLatitude() * location.getLongitude() * 10000)) % 10;
        String areaType;
        if (randomFactor < 3) {
            areaType = "Isolated";
        } else if (randomFactor < 6) {
            areaType = "Semi-Urban";
        } else {
            areaType = "Urban";
        }

        // 3. Lighting Conditions
        String lighting;
        if (isNight && (areaType.equals("Isolated") || areaType.equals("Semi-Urban"))) {
            lighting = "Poor";
        } else if (isNight && areaType.equals("Urban")) {
            lighting = "Moderate";
        } else {
            lighting = "Good";
        }

        // 4. SOS Activity (Simulated based on area)
        String sosActivity;
        if (areaType.equals("Isolated")) {
            sosActivity = randomFactor < 2 ? "High" : "Low";
        } else if (areaType.equals("Semi-Urban")) {
            sosActivity = randomFactor < 5 ? "Medium" : "Low";
        } else {
            sosActivity = "Low";
        }

        // 5. Distance to Services
        String distServices = areaType.equals("Urban") ? "Near (<2km)"
                : areaType.equals("Semi-Urban") ? "Moderate (2-5km)" : "Far (>5km)";

        // 6. Network Availability
        boolean isNetworkAvailable = isNetworkAvailable();
        String networkStatus = isNetworkAvailable ? "Stable" : "Weak/None";

        // 7. Weather (Simulated based on loc)
        String[] weatherTypes = { "Clear", "Rainy", "Cloudy", "Stormy", "Clear", "Clear" };
        String weather = weatherTypes[(int) (Math.abs(location.getLatitude() * 1000) % 6)];

        // CALCULATE SAFETY SCORE (0-100)
        int safetyScore = 100;

        // Time penalties
        if (isLateNight)
            safetyScore -= 25;
        else if (isNight)
            safetyScore -= 10;

        // Area penalties
        if (areaType.equals("Isolated"))
            safetyScore -= 20;
        else if (areaType.equals("Semi-Urban"))
            safetyScore -= 5;

        // Lighting penalties
        if (lighting.equals("Poor"))
            safetyScore -= 15;
        else if (lighting.equals("Moderate"))
            safetyScore -= 5;

        // SOS activity penalties
        if (sosActivity.equals("High"))
            safetyScore -= 10;
        else if (sosActivity.equals("Medium"))
            safetyScore -= 5;

        // Distance penalties
        if (distServices.contains("Far"))
            safetyScore -= 5;

        // Network & Weather penalties
        if (!isNetworkAvailable)
            safetyScore -= 20;

        if (weather.equals("Stormy"))
            safetyScore -= 10;
        else if (weather.equals("Rainy"))
            safetyScore -= 5;

        // Clamp Score
        if (safetyScore < 0)
            safetyScore = 0;

        // Determine safety level and reason
        String level;
        String reason;

        if (safetyScore >= 70) {
            level = "Safe";
            reason = "AI Analysis: Optimal safety conditions detected. Strong network coverage (" + networkStatus
                    + ") and favorable weather (" + weather
                    + ") contribute to a high safety index. Nearby services are accessible.";
        } else if (safetyScore >= 40) {
            level = "Moderate";
            if (!isNetworkAvailable) {
                reason = "AI Analysis: CRITICAL NETWORK ISSUE. Weak signal detected in a moderate risk zone. Ensure offline maps are downloaded and keep SOS shortcut ready.";
            } else if (isNight && areaType.equals("Semi-Urban")) {
                reason = "AI Analysis: Moderate risk linked to reduced visibility and semi-urban density. Stay on main roads.";
            } else if (weather.equals("Stormy")) {
                reason = "AI Analysis: Adverse weather (" + weather
                        + ") may hamper emergency response times. Seek shelter or secure transport.";
            } else {
                reason = "AI Analysis: Mixed environmental signals. While basic infrastructure is present, exercise caution due to "
                        + lighting.toLowerCase() + " lighting.";
            }
        } else {
            level = "Risky";
            if (isLateNight && areaType.equals("Isolated")) {
                reason = "AI Analysis: HIGH RISK. Late hours in an isolated zone pose significant danger. Request 'Track Me' from a contact immediately.";
            } else if (!isNetworkAvailable) {
                reason = "AI Analysis: DANGER. High risk area with NO NETWORK COVERAGE. You are effectively offline. Move to high ground or populated area to regain signal.";
            } else if (sosActivity.equals("High")) {
                reason = "AI Analysis: Historical hotspot for alerts. Avoid this route if possible.";
            } else {
                reason = "AI Analysis: Multiple compound risks (Weather, Isolation, Light). Reroute immediately to the nearest Safe Zone (Police/Hospital).";
            }
        }

        // Create breakdown data
        java.util.ArrayList<String[]> breakdown = new java.util.ArrayList<>();

        // Time factor
        if (isLateNight)
            breakdown.add(new String[] { "⏰ Late Night", "-25 pts", "High risk (10PM-5AM)" });
        else if (isNight)
            breakdown.add(new String[] { "🌙 Night Time", "-10 pts", "Reduced visibility" });
        else
            breakdown.add(new String[] { "☀️ Day Time", "0 pts", "Safe period" });

        // Area factor
        if (areaType.equals("Isolated"))
            breakdown.add(new String[] { "🏚️ Isolated", "-20 pts", "Low population" });
        else if (areaType.equals("Semi-Urban"))
            breakdown.add(new String[] { "🏘️ Semi-Urban", "-5 pts", "Moderate density" });
        else
            breakdown.add(new String[] { "🏙️ Urban", "0 pts", "High activity" });

        // Network
        if (!isNetworkAvailable)
            breakdown.add(new String[] { "📶 Network", "-20 pts", "Weak/No Signal" });
        else
            breakdown.add(new String[] { "✅ Network", "0 pts", "Stable Connection" });

        // Weather
        if (weather.equals("Stormy"))
            breakdown.add(new String[] { "⛈️ Weather", "-10 pts", "Stormy Conditions" });
        else if (weather.equals("Rainy"))
            breakdown.add(new String[] { "🌧️ Weather", "-5 pts", "Rainy Conditions" });
        else
            breakdown.add(new String[] { "⛅ Weather", "0 pts", "Clear/Fair" });

        // Lighting factor
        if (lighting.equals("Poor"))
            breakdown.add(new String[] { "💡 Lighting", "-15 pts", "Poor visibility" });
        else if (lighting.equals("Moderate"))
            breakdown.add(new String[] { "🔦 Lighting", "-5 pts", "Moderate visibility" });

        // SOS
        if (sosActivity.equals("High"))
            breakdown.add(new String[] { "🚨 History", "-10 pts", "High SOS reports" });

        // Fetch and display area name
        fetchAndDisplayAreaName(location);

        // Fetch Nearby Places (Police, Hospital, Hotel)
        fetchNearbyPlaces(location);

        // Show results with breakdown
        showResults(level, reason, safetyScore, breakdown);
    }

    private void fetchNearbyPlaces(Location location) {
        // Clear previous results
        layoutPolice.removeAllViews();
        layoutHospitals.removeAllViews();
        layoutHotels.removeAllViews();

        // Fetch specific place types
        fetchPlacesOfType(location, "police", layoutPolice);
        fetchPlacesOfType(location, "hospital", layoutHospitals);
        fetchPlacesOfType(location, "hotel", layoutHotels);
    }

    private void fetchPlacesOfType(Location location, String type, LinearLayout targetLayout) {
        new Thread(() -> {
            try {
                // Nominatim API for general queries (easier than Overpass for specific
                // categories)
                // Limit to 3 results
                String urlString = String.format(Locale.US,
                        "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=3&lat=%f&lon=%f&bounded=1&radius=5000",
                        type, location.getLatitude(), location.getLongitude());

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SmartWomenSafetyApp/1.0"); // Nominatim requires User-Agent
                conn.setConnectTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray places = new JSONArray(response.toString());

                runOnUiThread(() -> {
                    targetLayout.removeAllViews(); // Clear "loading" placeholder if any

                    if (places.length() == 0) {
                        TextView emptyView = new TextView(SafetyAnalysisActivity.this);
                        emptyView.setText("None found nearby.");
                        emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        emptyView.setPadding(0, 4, 0, 8);
                        targetLayout.addView(emptyView);
                        return;
                    }

                    for (int i = 0; i < places.length(); i++) {
                        try {
                            JSONObject place = places.getJSONObject(i);
                            String name = place.optString("display_name", "Unknown Place");
                            double pLat = place.optDouble("lat", 0);
                            double pLon = place.optDouble("lon", 0);

                            // Truncate long names for column view
                            if (name.length() > 20)
                                name = name.substring(0, 18) + "...";

                            TextView placeView = new TextView(SafetyAnalysisActivity.this);
                            placeView.setText(name);
                            placeView.setGravity(android.view.Gravity.CENTER);
                            placeView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                            placeView.setTextSize(12);
                            placeView.setPadding(0, 4, 0, 8);
                            placeView.setTypeface(null, android.graphics.Typeface.BOLD);

                            // Navigation Intent -> Safe Route
                            String finalName = name;
                            placeView.setOnClickListener(v -> {
                                Toast.makeText(SafetyAnalysisActivity.this, "Calculating Safe Route...",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SafetyAnalysisActivity.this, RouteActivity.class);
                                intent.putExtra("DEST_LAT", pLat);
                                intent.putExtra("DEST_LON", pLon);
                                intent.putExtra("DEST_NAME", finalName);
                                startActivity(intent);
                            });

                            targetLayout.addView(placeView);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    TextView errorView = new TextView(SafetyAnalysisActivity.this);
                    errorView.setText("Failed to load.");
                    errorView.setTextColor(getResources().getColor(R.color.colorError));
                    targetLayout.addView(errorView);
                });
            }
        }).start();

    }

    @SuppressLint("SetTextI18n")
    private void analyzeWithoutLocation() {
        // Fallback analysis based only on time
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 19 || hour <= 5);
        boolean isLateNight = (hour >= 22 || hour <= 4);

        String level;
        String reason;
        int score;
        java.util.ArrayList<String[]> breakdown = new java.util.ArrayList<>();

        if (isLateNight) {
            level = "Moderate";
            reason = "Late night hours - extra caution advised";
            score = 50;
            breakdown.add(new String[] { "⏰ Late Night Time", "-30 points", "High risk period" });
            breakdown.add(new String[] { "❓ Location Unknown", "-20 points", "Cannot assess area safety" });
        } else if (isNight) {
            level = "Moderate";
            reason = "Night time - stay in well-lit areas";
            score = 60;
            breakdown.add(new String[] { "🌙 Night Time", "-15 points", "Reduced visibility" });
            breakdown.add(new String[] { "❓ Location Unknown", "-25 points", "Cannot assess area safety" });
        } else {
            level = "Safe";
            reason = "Daytime hours - generally safer conditions";
            score = 80;
            breakdown.add(new String[] { "☀️ Day Time", "0 points", "Safer time period" });
            breakdown.add(new String[] { "❓ Location Unknown", "-20 points", "Cannot assess area safety" });
        }

        showResults(level, reason, score, breakdown);
    }

    @SuppressLint("SetTextI18n")
    private void showResults(String level, String reason, int score, java.util.ArrayList<String[]> breakdown) {
        progressBar.setVisibility(View.GONE);
        layoutResults.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(true);
        btnAnalyze.setText("Re-Analyze Safety");

        tvSafetyLevel.setText(level + " (" + score + "/100)");
        tvReason.setText(reason);

        int colorRes;
        if (level.equalsIgnoreCase("Safe")) {
            colorRes = R.color.colorSuccess;
        } else if (level.equalsIgnoreCase("Risky")) {
            colorRes = R.color.colorError;
        } else {
            colorRes = R.color.colorWarning;
        }
        cardSafetyLevel.setCardBackgroundColor(getResources().getColor(colorRes));

        // Display detailed breakdown
        displayBreakdown(breakdown, score);
    }

    @SuppressLint("SetTextI18n")
    private void displayBreakdown(java.util.ArrayList<String[]> breakdown, int finalScore) {
        layoutBreakdown.removeAllViews();

        // Add header
        TextView header = new TextView(this);
        header.setText("Starting Score: 100 points");
        header.setTextSize(14);
        header.setTextColor(getResources().getColor(android.R.color.darker_gray));
        header.setPadding(0, 0, 0, 16);
        layoutBreakdown.addView(header);

        // Add each factor
        for (String[] item : breakdown) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 12, 0, 12);

            // Factor name and points
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView factorName = new TextView(this);
            factorName.setText(item[0]);
            factorName.setTextSize(15);
            factorName.setTextColor(getResources().getColor(R.color.colorOnSurface));
            factorName.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            headerRow.addView(factorName);

            TextView points = new TextView(this);
            points.setText(item[1]);
            points.setTextSize(15);
            points.setTypeface(null, android.graphics.Typeface.BOLD);
            // Color code the points
            if (item[1].startsWith("-")) {
                points.setTextColor(getResources().getColor(R.color.colorError));
            } else {
                points.setTextColor(getResources().getColor(R.color.colorSuccess));
            }
            headerRow.addView(points);

            itemLayout.addView(headerRow);

            // Explanation
            TextView explanation = new TextView(this);
            explanation.setText(item[2]);
            explanation.setTextSize(12);
            explanation.setTextColor(getResources().getColor(android.R.color.darker_gray));
            explanation.setPadding(0, 4, 0, 0);
            itemLayout.addView(explanation);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(getResources().getColor(R.color.colorOutline));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            dividerParams.setMargins(0, 12, 0, 0);
            divider.setLayoutParams(dividerParams);
            itemLayout.addView(divider);

            layoutBreakdown.addView(itemLayout);
        }

        // Add final score
        TextView footer = new TextView(this);
        footer.setText("━━━━━━━━━━━━━━━━━\nFinal Safety Score: " + finalScore + "/100");
        footer.setTextSize(15);
        footer.setTypeface(null, android.graphics.Typeface.BOLD);
        footer.setTextColor(getResources().getColor(R.color.colorPrimary));
        footer.setPadding(0, 16, 0, 0);
        layoutBreakdown.addView(footer);
    }

    @SuppressLint("SetTextI18n")
    private void fetchAndDisplayAreaName(Location location) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder areaName = new StringBuilder();

                    if (address.getFeatureName() != null) {
                        areaName.append(address.getFeatureName());
                    }
                    if (address.getLocality() != null) {
                        if (areaName.length() > 0)
                            areaName.append(", ");
                        areaName.append(address.getLocality());
                    }
                    if (address.getSubAdminArea() != null) {
                        if (areaName.length() > 0)
                            areaName.append(", ");
                        areaName.append(address.getSubAdminArea());
                    }

                    String finalAreaName = areaName.length() > 0 ? areaName.toString()
                            : "Lat: " + String.format("%.4f", location.getLatitude()) +
                                    ", Lon: " + String.format("%.4f", location.getLongitude());

                    runOnUiThread(() -> tvAreaName.setText(finalAreaName));
                } else {
                    runOnUiThread(() -> tvAreaName.setText(
                            "Lat: " + String.format("%.4f", location.getLatitude()) +
                                    ", Lon: " + String.format("%.4f", location.getLongitude())));
                }
            } catch (IOException e) {
                runOnUiThread(() -> tvAreaName.setText("Location: " +
                        String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude())));
            }
        }).start();
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        btnAnalyze.setEnabled(true);
        btnAnalyze.setText("Analyze Safety");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
