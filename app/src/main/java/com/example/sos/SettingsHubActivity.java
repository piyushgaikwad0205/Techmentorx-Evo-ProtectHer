package com.example.sos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsHubActivity extends AppCompatActivity {

    MaterialCardView cardProfile, cardAppSettings, cardHistory, cardSmartDefense, cardUserGuide;
    ImageView btnBack;
    MaterialButton btnLogout;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_hub);

        // Set light theme
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        cardProfile = findViewById(R.id.cardProfile);
        cardAppSettings = findViewById(R.id.cardAppSettings);
        cardHistory = findViewById(R.id.cardHistory);
        cardSmartDefense = findViewById(R.id.cardSmartDefense);
        cardUserGuide = findViewById(R.id.cardUserGuide);
        btnLogout = findViewById(R.id.btnLogout);

        // Back button listener
        btnBack.setOnClickListener(v -> onBackPressed());

        // Logout button listener
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsHubActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cardProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsHubActivity.this, ProfileActivity.class));
        });

        cardAppSettings.setOnClickListener(v -> {
            startActivity(new Intent(SettingsHubActivity.this, AppSettingsActivity.class));
        });

        cardHistory.setOnClickListener(v -> {
            startActivity(new Intent(SettingsHubActivity.this, HistoryActivity.class));
        });

        cardUserGuide.setOnClickListener(v -> {
            startActivity(new Intent(SettingsHubActivity.this, HowToUseActivity.class));
        });

        // Smart Defense Card Listener
        cardSmartDefense.setOnClickListener(v -> showSmartDefenseDialog());
    }

    private void showSmartDefenseDialog() {
        // Layout for Dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Title
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Smart Defence Configuration");
        title.setTextSize(20);
        title.setTextColor(android.graphics.Color.parseColor("#FF3333"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        // Description
        android.widget.TextView description = new android.widget.TextView(this);
        description.setText("Enter your ESP32 device IP address");
        description.setTextSize(14);
        description.setTextColor(android.graphics.Color.parseColor("#666666"));
        description.setPadding(0, 0, 0, 30);
        layout.addView(description);

        // IP Input Field
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("ESP32 IP (e.g., 192.168.4.1)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setPadding(30, 30, 30, 30);
        input.setBackgroundResource(R.drawable.bg_input_field);

        android.content.SharedPreferences prefs = getSharedPreferences("SmartDefensePrefs", MODE_PRIVATE);
        input.setText(prefs.getString("esp_ip", ""));

        layout.addView(input);

        // Create Dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            prefs.edit().putString("esp_ip", ip).apply();
            android.widget.Toast.makeText(SettingsHubActivity.this,
                    "ESP32 IP saved successfully",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendRequest(String ip, String command) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String urlString = "http://" + ip + "/" + command;
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        android.widget.Toast.makeText(SettingsHubActivity.this, "Device " + command.toUpperCase(),
                                android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(SettingsHubActivity.this, "Error: " + responseCode,
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    android.widget.Toast
                            .makeText(SettingsHubActivity.this, "Connection Failed", android.widget.Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }
}
