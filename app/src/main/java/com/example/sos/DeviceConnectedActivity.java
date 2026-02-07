package com.example.sos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;

import java.util.UUID;

public class DeviceConnectedActivity extends AppCompatActivity {

    private TextView tvDeviceName, tvDeviceAddress, tvConnectionStatus;
    private TextView tvBatteryLevel, tvHeartRate, tvMonitoringStatus;
    private MaterialButton btnDeviceInfo, btnRemove, btnFindDevice;
    private ImageView btnBack, btnSettings;

    private BluetoothDevice connectedDevice;
    private BluetoothGatt bluetoothGatt;
    private String deviceName;
    private String deviceAddress;

    private HeartRateMonitor heartRateMonitor;

    // Standard UUIDs for Battery and Heart Rate services
    private static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connected);

        // Set system bar colors
        setSystemBarColors();

        // Initialize views
        initializeViews();

        // Initialize Heart Rate Monitor
        heartRateMonitor = new HeartRateMonitor(this, findViewById(android.R.id.content));

        // Get device info from intent
        Intent intent = getIntent();
        deviceName = intent.getStringExtra("DEVICE_NAME");
        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");

        // Set device information
        if (deviceName != null && !deviceName.isEmpty()) {
            tvDeviceName.setText(deviceName);
        } else {
            tvDeviceName.setText("Unknown Device");
        }

        if (deviceAddress != null) {
            tvDeviceAddress.setText(deviceAddress);
        }

        // Set initial values
        tvConnectionStatus.setText("Connecting...");
        tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        tvBatteryLevel.setText("--");
        // Heart rate and status managed by HeartRateMonitor now
        // tvHeartRate.setText("--");
        // tvMonitoringStatus.setText("Initializing");

        // Setup click listeners
        setupClickListeners();

        // Connect to device
        connectToDevice();
    }

    private void setSystemBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(getColor(android.R.color.white));
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void connectToDevice() {
        if (deviceAddress == null) {
            Toast.makeText(this, "Device address not available", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            connectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Connect to GATT server
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = connectedDevice.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = connectedDevice.connectGatt(this, false, gattCallback);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Connected");
                    tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    // Start monitoring UI
                    if (heartRateMonitor != null)
                        heartRateMonitor.start();
                });

                try {
                    if (ActivityCompat.checkSelfPermission(DeviceConnectedActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices();
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Disconnected");
                    tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    // Stop monitoring UI
                    if (heartRateMonitor != null)
                        heartRateMonitor.stop();
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Read battery level
                BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE_UUID);
                if (batteryService != null) {
                    BluetoothGattCharacteristic batteryChar = batteryService.getCharacteristic(BATTERY_LEVEL_UUID);
                    if (batteryChar != null) {
                        try {
                            if (ActivityCompat.checkSelfPermission(DeviceConnectedActivity.this,
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt.readCharacteristic(batteryChar);
                            }
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Enable heart rate notifications
                BluetoothGattService heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID);
                if (heartRateService != null) {
                    BluetoothGattCharacteristic heartRateChar = heartRateService
                            .getCharacteristic(HEART_RATE_MEASUREMENT_UUID);
                    if (heartRateChar != null) {
                        try {
                            if (ActivityCompat.checkSelfPermission(DeviceConnectedActivity.this,
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt.setCharacteristicNotification(heartRateChar, true);
                            }
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (BATTERY_LEVEL_UUID.equals(characteristic.getUuid())) {
                    int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    runOnUiThread(() -> tvBatteryLevel.setText(batteryLevel + "%"));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (HEART_RATE_MEASUREMENT_UUID.equals(characteristic.getUuid())) {
                int heartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                runOnUiThread(() -> {
                    if (heartRateMonitor != null)
                        heartRateMonitor.updateHeartRate(heartRate);
                });
            }
        }
    };

    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);

        // Device info
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvDeviceAddress = findViewById(R.id.tvDeviceAddress);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel);

        // Health monitoring
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvMonitoringStatus = findViewById(R.id.tvMonitoringStatus);

        // Buttons
        btnDeviceInfo = findViewById(R.id.btnDeviceInfo);
        btnRemove = findViewById(R.id.btnRemove);
        btnFindDevice = findViewById(R.id.btnFindDevice);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsHubActivity.class));
        });

        btnDeviceInfo.setOnClickListener(v -> {
            showDeviceInfo();
        });

        btnRemove.setOnClickListener(v -> {
            showRemoveDialog();
        });

        btnFindDevice.setOnClickListener(v -> {
            findDevice();
        });
    }

    private void showDeviceInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Device Information");

        String info = "Device Name: " + (deviceName != null ? deviceName : "Unknown") + "\n\n" +
                "MAC Address: " + (deviceAddress != null ? deviceAddress : "Unknown") + "\n\n" +
                "Status: " + tvConnectionStatus.getText() + "\n\n" +
                "Type: Bluetooth LE Device";

        builder.setMessage(info);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showRemoveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Device")
                .setMessage("Are you sure you want to remove this device? You will need to pair it again to reconnect.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeDevice();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeDevice() {
        try {
            // Stop monitoring service to prevent false alarm
            Intent stopIntent = new Intent(this, TamperMonitorService.class);
            stopIntent.putExtra("action", "STOP_MONITORING");
            startService(stopIntent);

            // Disconnect GATT first
            if (bluetoothGatt != null) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            }

            // Try to unpair the device using reflection
            if (connectedDevice != null) {
                connectedDevice.getClass().getMethod("removeBond").invoke(connectedDevice);
            }

            Toast.makeText(this, "Device removed successfully", Toast.LENGTH_SHORT).show();

            // Go back to device connect screen
            Intent intent = new Intent(this, DeviceConnectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to remove device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void findDevice() {
        if (connectedDevice == null) {
            Toast.makeText(this, "Device not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Triggering alert on device...", Toast.LENGTH_SHORT).show();

        BluetoothDeviceController controller = new BluetoothDeviceController(this);
        controller.findDevice(connectedDevice, new BluetoothDeviceController.FindDeviceCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(DeviceConnectedActivity.this,
                            "Alert sent to device!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DeviceConnectedActivity.this,
                            "Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, DeviceConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (heartRateMonitor != null) {
            heartRateMonitor.cleanup();
        }
        // We do NOT disconnect here anymore to keep connection alive in background
        // The user must explicitly remove the device to disconnect
    }
}
