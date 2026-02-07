package com.example.sos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class TamperMonitorService extends Service {

    private static final String TAG = "TamperMonitorService";
    private static final String CHANNEL_ID = "tamper_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private String monitoredDeviceAddress = null;
    private boolean isMonitoring = false;
    private boolean isPaused = false;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null && monitoredDeviceAddress != null) {
                    try {
                        if (device.getAddress().equals(monitoredDeviceAddress)) {
                            if (isPaused) {
                                Log.i(TAG, "Ignored disconnect for " + device.getName() + " (Monitoring Paused)");
                                return;
                            }
                            Log.w(TAG, "Monitored device disconnected: " + device.getName());
                            onDeviceDisconnected();
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission error checking device", e);
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (device != null && monitoredDeviceAddress != null) {
                    try {
                        if (device.getAddress().equals(monitoredDeviceAddress) &&
                                bondState == BluetoothDevice.BOND_NONE) {
                            Log.w(TAG, "Monitored device unpaired: " + device.getName());
                            onDeviceRemoved();
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission error checking device", e);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Register Bluetooth receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);

        loadMonitoredDevice();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");

            if ("START_MONITORING".equals(action)) {
                String deviceAddress = intent.getStringExtra("device_address");
                startMonitoring(deviceAddress);
            } else if ("STOP_MONITORING".equals(action)) {
                stopMonitoring();
            } else if ("PAUSE_MONITORING".equals(action)) {
                isPaused = true;
                Log.i(TAG, "Monitoring paused temporarily");
            } else if ("RESUME_MONITORING".equals(action)) {
                isPaused = false;
                Log.i(TAG, "Monitoring resumed");
            }
        }

        return START_STICKY;
    }

    private void startMonitoring(String deviceAddress) {
        monitoredDeviceAddress = deviceAddress;
        isMonitoring = true;

        // Save to preferences
        SharedPreferences prefs = getSharedPreferences("tamper_monitor", MODE_PRIVATE);
        prefs.edit()
                .putString("monitored_device", deviceAddress)
                .putBoolean("is_monitoring", true)
                .apply();

        updateNotification("Monitoring device for tamper detection");
        Log.i(TAG, "Started monitoring device: " + deviceAddress);
    }

    private void stopMonitoring() {
        isMonitoring = false;
        monitoredDeviceAddress = null;

        SharedPreferences prefs = getSharedPreferences("tamper_monitor", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_monitoring", false)
                .remove("monitored_device")
                .apply();

        updateNotification("Tamper monitoring stopped");
        Log.i(TAG, "Stopped monitoring");
    }

    private void loadMonitoredDevice() {
        SharedPreferences prefs = getSharedPreferences("tamper_monitor", MODE_PRIVATE);
        isMonitoring = prefs.getBoolean("is_monitoring", false);
        monitoredDeviceAddress = prefs.getString("monitored_device", null);

        if (isMonitoring && monitoredDeviceAddress != null) {
            Log.i(TAG, "Resumed monitoring device: " + monitoredDeviceAddress);
        }
    }

    private void onDeviceDisconnected() {
        if (!isMonitoring) {
            Log.w(TAG, "Device disconnected but monitoring is OFF");
            return;
        }

        Log.w(TAG, "Device disconnected - triggering tamper alert");
        launchTamperAlert("Device Disconnected");
    }

    private void onDeviceRemoved() {
        if (!isMonitoring) {
            Log.w(TAG, "Device removed but monitoring is OFF");
            return;
        }

        Log.w(TAG, "Device removed/unpaired - triggering tamper alert");
        launchTamperAlert("Device Removed");
    }

    private void launchTamperAlert(String reason) {
        Log.e(TAG, "LAUNCHING TAMPER ALERT - Reason: " + reason);

        Intent alertIntent = new Intent(this, TamperAlertActivity.class);
        alertIntent.putExtra("reason", reason);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        try {
            startActivity(alertIntent);
            Log.i(TAG, "TamperAlertActivity launched successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch TamperAlertActivity", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tamper Monitor Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Monitors connected device for tamper detection");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Device Protection Active")
                .setContentText("Monitoring for unauthorized device removal")
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Device Protection Active")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
