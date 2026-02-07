package com.example.sos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

public class DeviceConnectActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private BluetoothAdapter bluetoothAdapter;
    private MaterialButton btnScanNow;
    private RecyclerView recyclerPairedDevices, recyclerAvailableDevices;
    private LinearLayout scanningLayout;
    private LinearLayout pairedDevicesSection, availableDevicesSection;
    private DeviceListAdapter pairedDevicesAdapter, availableDevicesAdapter;
    private boolean isScanning = false;

    private android.widget.ImageView ivScanningWatch;
    private View ripple1, ripple2, ripple3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connect);

        // Set system bar colors
        setSystemBarColors();

        // Initialize views
        btnScanNow = findViewById(R.id.btnScanNow);
        recyclerPairedDevices = findViewById(R.id.recyclerPairedDevices);
        recyclerAvailableDevices = findViewById(R.id.recyclerAvailableDevices);
        scanningLayout = findViewById(R.id.scanningLayout);
        pairedDevicesSection = findViewById(R.id.pairedDevicesSection);
        availableDevicesSection = findViewById(R.id.availableDevicesSection);
        ivScanningWatch = findViewById(R.id.imageView6);
        ripple1 = findViewById(R.id.ripple1);
        ripple2 = findViewById(R.id.ripple2);
        ripple3 = findViewById(R.id.ripple3);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Setup Paired Devices RecyclerView
        recyclerPairedDevices.setLayoutManager(new LinearLayoutManager(this));
        pairedDevicesAdapter = new DeviceListAdapter(this, new DeviceListAdapter.OnDeviceClickListener() {
            @Override
            public void onConnectClick(BluetoothDevice device) {
                unpairDevice(device);
            }

            @Override
            public void onItemClick(BluetoothDevice device) {
                if (ActivityCompat.checkSelfPermission(DeviceConnectActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(DeviceConnectActivity.this, "Bluetooth permission required", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                Intent intent = new Intent(DeviceConnectActivity.this, DeviceConnectedActivity.class);
                intent.putExtra("DEVICE_NAME", device.getName());
                intent.putExtra("DEVICE_ADDRESS", device.getAddress());
                startActivity(intent);
            }
        });
        recyclerPairedDevices.setAdapter(pairedDevicesAdapter);

        // Setup Available Devices RecyclerView
        recyclerAvailableDevices.setLayoutManager(new LinearLayoutManager(this));
        availableDevicesAdapter = new DeviceListAdapter(this, new DeviceListAdapter.OnDeviceClickListener() {
            @Override
            public void onConnectClick(BluetoothDevice device) {
                onAvailableDeviceClick(device);
            }

            @Override
            public void onItemClick(BluetoothDevice device) {
                onAvailableDeviceClick(device);
            }
        });
        recyclerAvailableDevices.setAdapter(availableDevicesAdapter);

        // Scan Now button listener
        btnScanNow.setOnClickListener(v -> {
            if (isScanning) {
                stopBluetoothScan();
            } else {
                startBluetoothScan();
            }
        });

        // Header Settings Logic
        findViewById(R.id.editMessage).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsHubActivity.class));
        });

        // Bottom Navigation Setup

        // Load already paired devices
        loadPairedDevices();
    }

    // ... (rest of methods) ...

    private Animation createRippleAnim(long startOffset) {
        AnimationSet set = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(1.0f, 1.3f, 1.0f, 1.3f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(1500);
        scale.setRepeatCount(Animation.INFINITE);

        AlphaAnimation alpha = new AlphaAnimation(0.5f, 0.0f);
        alpha.setDuration(1500);
        alpha.setRepeatCount(Animation.INFINITE);

        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setStartOffset(startOffset);
        return set;
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

    private void stopBluetoothScan() {
        if (bluetoothAdapter != null && isScanning) {
            try {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery();
                }
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
            isScanning = false;
            btnScanNow.setText("Scan for Devices");

            // Stop animation
            stopRippleAnimation();

            // Hide scanning indicator
            scanningLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        pairedDevicesSection.setVisibility(View.GONE);
        availableDevicesSection.setVisibility(View.GONE);
        scanningLayout.setVisibility(View.GONE);
        stopRippleAnimation();
    }

    private void showScanningState() {
        startRippleAnimation();
        scanningLayout.setVisibility(View.VISIBLE);
    }

    private void showDeviceList() {
        if (pairedDevicesAdapter.getItemCount() > 0) {
            pairedDevicesSection.setVisibility(View.VISIBLE);
        }
        if (availableDevicesAdapter.getItemCount() > 0) {
            availableDevicesSection.setVisibility(View.VISIBLE);
        }
        stopRippleAnimation();
    }

    private void loadPairedDevices() {
        if (bluetoothAdapter == null) {
            showEmptyState();
            return;
        }

        if (!hasBluetoothPermissions()) {
            showEmptyState();
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                java.util.Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                pairedDevicesAdapter.clear();

                if (pairedDevices != null && !pairedDevices.isEmpty()) {
                    // Add all paired devices to the paired list
                    for (BluetoothDevice device : pairedDevices) {
                        pairedDevicesAdapter.addDevice(device);
                    }
                    pairedDevicesSection.setVisibility(View.VISIBLE);
                    btnScanNow.setText("Scan for More Devices");
                } else {
                    pairedDevicesSection.setVisibility(View.GONE);
                    if (availableDevicesAdapter.getItemCount() == 0) {
                        showEmptyState();
                    }
                }
            } else {
                showEmptyState();
            }
        } catch (SecurityException e) {
            showEmptyState();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unpair Device")
                .setMessage("Are you sure you want to unpair this device?")
                .setPositiveButton("Unpair", (dialog, which) -> {
                    try {
                        device.getClass().getMethod("removeBond").invoke(device);
                        Toast.makeText(this, "Device unpaired successfully", Toast.LENGTH_SHORT).show();

                        // Refresh lists
                        pairedDevicesAdapter.clear();
                        loadPairedDevices();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to unpair device", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onAvailableDeviceClick(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if there's already a paired device
            java.util.Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                // Show dialog to confirm replacing existing paired device
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Replace Paired Device?")
                        .setMessage(
                                "Only one device can be paired at a time. Do you want to unpair the current device and pair with this new device?")
                        .setPositiveButton("Yes, Replace", (dialog, which) -> {
                            // Unpair all existing devices
                            for (BluetoothDevice pairedDevice : pairedDevices) {
                                try {
                                    pairedDevice.getClass().getMethod("removeBond").invoke(pairedDevice);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // Now pair with the new device
                            Toast.makeText(this, "Pairing with " +
                                    (device.getName() != null ? device.getName() : "device") + "...",
                                    Toast.LENGTH_SHORT).show();
                            device.createBond();

                            // Refresh the lists after a short delay
                            new android.os.Handler().postDelayed(() -> {
                                pairedDevicesAdapter.clear();
                                availableDevicesAdapter.clear();
                                availableDevicesSection.setVisibility(View.GONE);
                                loadPairedDevices();
                            }, 2000);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                // No paired devices, proceed with pairing
                Toast.makeText(this, "Pairing with " +
                        (device.getName() != null ? device.getName() : "device") + "...",
                        Toast.LENGTH_SHORT).show();
                device.createBond();

                // Refresh the lists after a short delay
                new android.os.Handler().postDelayed(() -> {
                    pairedDevicesAdapter.clear();
                    availableDevicesAdapter.clear();
                    availableDevicesSection.setVisibility(View.GONE);
                    loadPairedDevices();
                }, 2000);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show();
        }
    }

    private void startBluetoothScan() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
            startActivityForResult(enableBtIntent, 1);
            return;
        }

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        // Clear previous available devices
        availableDevicesAdapter.clear();

        // Update UI
        showScanningState();
        isScanning = true;
        btnScanNow.setText("Stop Scanning");

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.startDiscovery();
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(DeviceConnectActivity.this,
                                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                            // Only add if not already paired
                            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                                availableDevicesAdapter.addDevice(device);
                                availableDevicesSection.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                btnScanNow.setText("Scan for Devices");
                scanningLayout.setVisibility(View.GONE);
                stopRippleAnimation();

                if (availableDevicesAdapter.getItemCount() == 0) {
                    if (pairedDevicesAdapter.getItemCount() == 0) {
                        Toast.makeText(DeviceConnectActivity.this,
                                "No devices found", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    } else {
                        availableDevicesSection.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startBluetoothScan();
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh paired devices list when returning to this screen
        pairedDevicesAdapter.clear();
        availableDevicesAdapter.clear();
        availableDevicesSection.setVisibility(View.GONE);
        loadPairedDevices();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBluetoothScan();
    }
}
