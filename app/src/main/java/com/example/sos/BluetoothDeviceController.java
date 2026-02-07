package com.example.sos;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

public class BluetoothDeviceController {

    private static final String TAG = "BTDeviceController";

    // Standard BLE Alert Service UUIDs
    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHAR = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    // Xiaomi/Amazfit services found in your watch
    private static final UUID XIAOMI_SERVICE_190E = UUID.fromString("0000190e-0000-1000-8000-00805f9b34fb");
    private static final UUID XIAOMI_SERVICE_FEE7 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    private static final UUID XIAOMI_SERVICE_FEEA = UUID.fromString("0000feea-0000-1000-8000-00805f9b34fb");

    // Actual characteristics from your watch (from logcat)
    private static final UUID CHAR_190E_03 = UUID.fromString("00000003-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_190E_04 = UUID.fromString("00000004-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_FEE7_C9 = UUID.fromString("0000fec9-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_FEE7_A1 = UUID.fromString("0000fea1-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_FEEA_E1 = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");

    // Alert levels and commands
    private static final byte ALERT_LEVEL_HIGH = 0x02;

    private Context context;
    private BluetoothGatt bluetoothGatt;
    private FindDeviceCallback callback;

    public interface FindDeviceCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public BluetoothDeviceController(Context context) {
        this.context = context;
    }

    public void findDevice(BluetoothDevice device, FindDeviceCallback callback) {
        this.callback = callback;

        try {
            // Pause tamper monitoring to avoid false alarms during connection/disconnection
            Intent pauseIntent = new Intent(context, TamperMonitorService.class);
            pauseIntent.putExtra("action", "PAUSE_MONITORING");
            context.startService(pauseIntent);

            Log.i(TAG, "Connecting to device: " + device.getAddress());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            if (callback != null) {
                callback.onFailure("Permission denied");
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server");
                    cleanup();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error in connection state change", e);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered successfully");

                // Log all available services for debugging
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.i(TAG, "Available service: " + service.getUuid().toString());
                    for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                        Log.i(TAG, "  -> Characteristic: " + ch.getUuid().toString());
                    }
                }

                triggerAlert(gatt);
            } else {
                Log.w(TAG, "Service discovery failed with status: " + status);
                if (callback != null) {
                    callback.onFailure("Service discovery failed");
                }
                cleanup();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Alert sent successfully! Your watch should vibrate now!");
                if (callback != null) {
                    callback.onSuccess();
                }
            } else {
                Log.w(TAG, "Alert send failed with status: " + status);
                if (callback != null) {
                    callback.onFailure("Failed to send alert");
                }
            }

            // Disconnect after sending alert
            try {
                gatt.disconnect();
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error disconnecting", e);
            }
        }
    };

    private void triggerAlert(BluetoothGatt gatt) {
        try {
            // Try standard Immediate Alert Service first
            if (tryStandardAlert(gatt))
                return;

            // Try Service 190E (most likely alert service for your watch)
            if (tryService190E(gatt))
                return;

            // Try Service FEE7
            if (tryServiceFEE7(gatt))
                return;

            // Try Service FEEA
            if (tryServiceFEEA(gatt))
                return;

            // No compatible service found
            Log.w(TAG, "No compatible alert service found");
            if (callback != null) {
                callback.onFailure("Your watch doesn't support this feature");
            }
            cleanup();

        } catch (SecurityException e) {
            Log.e(TAG, "Permission error triggering alert", e);
            if (callback != null) {
                callback.onFailure("Permission denied");
            }
            cleanup();
        }
    }

    private boolean tryStandardAlert(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(IMMEDIATE_ALERT_SERVICE);
        if (service != null) {
            Log.i(TAG, "Trying standard Immediate Alert Service");
            BluetoothGattCharacteristic alertChar = service.getCharacteristic(ALERT_LEVEL_CHAR);
            if (alertChar != null) {
                return writeCharacteristic(gatt, alertChar, new byte[] { ALERT_LEVEL_HIGH }, "Standard");
            }
        }
        return false;
    }

    private boolean tryService190E(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(XIAOMI_SERVICE_190E);
        if (service == null)
            return false;

        Log.i(TAG, "Trying Service 190E (Alert Service)");

        // Try characteristic 0x0003 (most likely vibration trigger)
        BluetoothGattCharacteristic char03 = service.getCharacteristic(CHAR_190E_03);
        if (char03 != null) {
            Log.i(TAG, "Found characteristic 0x0003 in service 190E");
            // Try different alert commands including Xiaomi-specific patterns
            byte[][] commands = {
                    { 0x01 }, // Simple alert
                    { 0x02 }, // Medium alert
                    { 0x03 }, // Strong alert
                    { (byte) 0xFF }, // Max alert
                    { 0x05, 0x01 }, // Xiaomi vibrate pattern 1
                    { 0x05, 0x02 }, // Xiaomi vibrate pattern 2
                    { 0x05, 0x03 }, // Xiaomi vibrate pattern 3
                    { 0x01, 0x00 }, // Alert with parameter
                    { 0x02, 0x00 }, // Strong alert with parameter
                    { 0x10 }, // Alternative alert
                    { 0x11 }, // Alternative alert 2
                    { (byte) 0xAA }, // Test pattern
                    { (byte) 0xBB } // Test pattern 2
            };

            for (byte[] cmd : commands) {
                if (writeCharacteristic(gatt, char03, cmd, "190E-03")) {
                    return true;
                }
            }
        }

        // Try characteristic 0x0004
        BluetoothGattCharacteristic char04 = service.getCharacteristic(CHAR_190E_04);
        if (char04 != null) {
            Log.i(TAG, "Found characteristic 0x0004 in service 190E");
            // Try multiple commands on 0x0004 as well
            byte[][] commands04 = {
                    { 0x01 },
                    { 0x02 },
                    { 0x03 },
                    { (byte) 0xFF },
                    { 0x05, 0x01 }
            };

            for (byte[] cmd : commands04) {
                if (writeCharacteristic(gatt, char04, cmd, "190E-04")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryServiceFEE7(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(XIAOMI_SERVICE_FEE7);
        if (service == null)
            return false;

        Log.i(TAG, "Trying Service FEE7 (Xiaomi Alert Service)");

        byte[][] commands = {
                { 0x01 }, { 0x02 }, { 0x03 }, { (byte) 0xFF },
                { 0x05, 0x01 }, { 0x05, 0x02 }, { 0x05, 0x03 },
                { 0x10 }, { 0x11 }, { (byte) 0xAA }
        };

        BluetoothGattCharacteristic charC9 = service.getCharacteristic(CHAR_FEE7_C9);
        if (charC9 != null) {
            Log.i(TAG, "Found FEE7 characteristic FEC9");
            for (byte[] cmd : commands) {
                if (writeCharacteristic(gatt, charC9, cmd, "FEE7-C9"))
                    return true;
            }
        }

        BluetoothGattCharacteristic charA1 = service.getCharacteristic(CHAR_FEE7_A1);
        if (charA1 != null) {
            Log.i(TAG, "Found FEE7 characteristic FEA1");
            for (byte[] cmd : commands) {
                if (writeCharacteristic(gatt, charA1, cmd, "FEE7-A1"))
                    return true;
            }
        }

        return false;
    }

    private boolean tryServiceFEEA(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(XIAOMI_SERVICE_FEEA);
        if (service == null)
            return false;

        Log.i(TAG, "Trying Service FEEA (Xiaomi Main Service)");

        byte[][] commands = {
                { 0x01 }, { 0x02 }, { 0x03 }, { (byte) 0xFF },
                { 0x05, 0x01 }, { 0x05, 0x02 }, { 0x05, 0x03 },
                { 0x10 }, { 0x11 }, { (byte) 0xAA }, { (byte) 0xBB }
        };

        // Try all 6 characteristics in FEEA service
        UUID[] feeaChars = {
                CHAR_FEEA_E1,
                UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("0000fee3-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("0000fee4-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("0000fee5-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("0000fee6-0000-1000-8000-00805f9b34fb")
        };

        for (int i = 0; i < feeaChars.length; i++) {
            BluetoothGattCharacteristic ch = service.getCharacteristic(feeaChars[i]);
            if (ch != null) {
                Log.i(TAG, "Found FEEA characteristic FEE" + (i + 1));
                for (byte[] cmd : commands) {
                    if (writeCharacteristic(gatt, ch, cmd, "FEEA-" + (i + 1)))
                        return true;
                }
            }
        }

        return false;
    }

    private boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            byte[] value, String name) {
        try {
            int properties = characteristic.getProperties();
            boolean writeSuccess = false;

            // Try WRITE request (WITH RESPONSE)
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                characteristic.setValue(value);
                if (gatt.writeCharacteristic(characteristic)) {
                    Log.i(TAG, "Write initiated to " + name + " (WITH_RESPONSE) value " + bytesToHex(value));
                    return true;
                }
            }

            // Try WRITE COMMAND (NO RESPONSE)
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                characteristic.setValue(value);
                if (gatt.writeCharacteristic(characteristic)) {
                    Log.i(TAG, "Write initiated to " + name + " (NO_RESPONSE) value " + bytesToHex(value));
                    return true;
                }
            }

            // Fallback if properties not advertised but might work (common in some
            // firmwares)
            if (!writeSuccess) {
                Log.w(TAG, "Properties don't explicitly allow write, trying anyway for " + name);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                characteristic.setValue(value);
                return gatt.writeCharacteristic(characteristic);
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error writing to " + name, e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void cleanup() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing GATT", e);
            }
            bluetoothGatt = null;

            // Resume monitoring after a short delay to let the disconnect broadcast pass
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent resumeIntent = new Intent(context, TamperMonitorService.class);
                resumeIntent.putExtra("action", "RESUME_MONITORING");
                context.startService(resumeIntent);
            }, 3000); // 3 seconds delay
        }
    }

    public void disconnect() {
        cleanup();
    }
}
