package com.example.sos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SmartDefenseActivity extends AppCompatActivity {

    private TextInputEditText etIpAddress;
    private MaterialButton btnSaveIp, btnPower;
    private TextView tvStatus;
    private ImageView imgStatusIcon;

    private boolean isDeviceOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_defense);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etIpAddress = findViewById(R.id.etIpAddress);
        btnSaveIp = findViewById(R.id.btnSaveIp);
        btnPower = findViewById(R.id.btnPower);
        tvStatus = findViewById(R.id.tvStatus);
        imgStatusIcon = findViewById(R.id.imgStatusIcon);

        // Load Saved IP
        SharedPreferences prefs = getSharedPreferences("SmartDefensePrefs", MODE_PRIVATE);
        etIpAddress.setText(prefs.getString("esp_ip", "192.168.4.1"));

        btnSaveIp.setOnClickListener(v -> {
            String ip = etIpAddress.getText().toString().trim();
            if (ip.isEmpty()) {
                etIpAddress.setError("Enter IP");
                return;
            }
            prefs.edit().putString("esp_ip", ip).apply();
            Toast.makeText(this, "IP Address Saved", Toast.LENGTH_SHORT).show();
        });

        btnPower.setOnClickListener(v -> togglePower());

        updateUI();
    }

    private void togglePower() {
        SharedPreferences prefs = getSharedPreferences("SmartDefensePrefs", MODE_PRIVATE);
        String espIp = prefs.getString("esp_ip", "");

        if (espIp.isEmpty()) {
            Toast.makeText(this, "Please save IP address first", Toast.LENGTH_SHORT).show();
            return;
        }

        isDeviceOn = !isDeviceOn;
        String command = isDeviceOn ? "on" : "off";

        // Optimistic UI Update
        updateUI();

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String urlString = "http://" + espIp + "/" + command;
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);

                int code = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "Success: Device " + command.toUpperCase(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + code, Toast.LENGTH_SHORT).show();
                        // Revert
                        isDeviceOn = !isDeviceOn;
                        updateUI();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
                    // Revert
                    isDeviceOn = !isDeviceOn;
                    updateUI();
                });
            }
        });
    }

    private void updateUI() {
        if (isDeviceOn) {
            btnPower.setBackgroundColor(android.graphics.Color.RED);
            tvStatus.setText("DEVICE ACTIVE - HIGH VOLTAGE");
            tvStatus.setTextColor(android.graphics.Color.RED);
            imgStatusIcon.setColorFilter(android.graphics.Color.RED);
        } else {
            btnPower.setBackgroundColor(android.graphics.Color.parseColor("#CCCCCC"));
            tvStatus.setText("DEVICE OFF");
            tvStatus.setTextColor(android.graphics.Color.parseColor("#999999"));
            imgStatusIcon.setColorFilter(android.graphics.Color.parseColor("#CCCCCC"));
        }
    }
}
