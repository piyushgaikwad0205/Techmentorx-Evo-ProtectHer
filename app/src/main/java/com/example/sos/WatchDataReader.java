package com.example.sos;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WatchDataReader {

    private static final String TAG = "WatchDataReader";

    // Service UUIDs
    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID XIAOMI_SERVICE = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID BP_SERVICE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID SPO2_SERVICE = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID AE00_SERVICE = UUID.fromString("0000ae00-0000-1000-8000-00805f9b34fb");

    // Characteristic UUIDs
    private static final UUID BATTERY_LEVEL_CHAR = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_CHAR = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID SERIAL_NUMBER_CHAR = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID FIRMWARE_VERSION_CHAR = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    private static final UUID SOFTWARE_VERSION_CHAR = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_CHAR = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final UUID REALTIME_STEPS_CHAR = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    private static final UUID BP_MEASUREMENT_CHAR = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
    private static final UUID SPO2_CONTINUOUS_CHAR = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_CONTROL_POINT = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    private static final UUID SPO2_CONTROL_POINT = UUID.fromString("00002a62-0000-1000-8000-00805f9b34fb");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothGatt bluetoothGatt;
    private DataCallback callback;

    public interface DataCallback {
        void onBatteryLevel(int level);

        void onHeartRate(int bpm);

        void onSteps(int steps);

        void onBloodPressure(String bp);

        void onSpO2(int spo2);

        void onSleepData(String sleep);

        void onDeviceInfo(String serialNumber, String firmwareVersion, String softwareVersion, String manufacturer);

        void onError(String error);
    }

    public WatchDataReader(Context context) {
        this.context = context;
    }

    public void readBatteryLevel(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "battery");
    }

    private boolean isContinuousMonitoring = false;

    public void readHeartRate(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        this.isContinuousMonitoring = false;
        connectAndRead(device, "heartrate");
    }

    public void monitorHeartRate(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        this.isContinuousMonitoring = true;
        connectAndRead(device, "heartrate");
    }

    public void stopMonitoring() {
        this.isContinuousMonitoring = false;
        cleanup();
    }

    public void readSteps(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "steps");
    }

    public void readBP(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "bp");
    }

    public void readSpO2(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "spo2");
    }

    public void readSleep(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "sleep");
    }

    public void readDeviceInfo(BluetoothDevice device, DataCallback callback) {
        this.callback = callback;
        connectAndRead(device, "deviceinfo");
    }

    private String readType = "";

    private void connectAndRead(BluetoothDevice device, String type) {
        this.readType = type;
        try {
            Log.i(TAG, "Connecting to device for " + type + " reading");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error", e);
            if (callback != null)
                callback.onError("Permission denied");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server");
                try {
                    gatt.discoverServices();
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission error", e);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server");
                cleanup();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Build list of services for debugging
                StringBuilder serviceList = new StringBuilder();
                for (BluetoothGattService s : gatt.getServices()) {
                    String uuid = s.getUuid().toString();
                    serviceList.append(uuid.substring(4, 8)).append(", "); // Log short UUIDs
                    Log.i(TAG, "Found Service: " + uuid);
                }
                String servicesFound = serviceList.toString();
                Log.i(TAG, "Services discovered for " + readType + ": " + servicesFound);

                switch (readType) {
                    case "battery":
                        readBattery(gatt);
                        break;
                    case "heartrate":
                        readHeartRateData(gatt);
                        break;
                    case "steps":
                        readStepsData(gatt);
                        break;
                    case "bp":
                        readBPData(gatt, servicesFound);
                        break;
                    case "spo2":
                        readSpO2Data(gatt, servicesFound);
                        break;
                    case "sleep":
                        if (callback != null)
                            callback.onError("Sleep data requires full daily sync");
                        cleanup();
                        break;
                    case "deviceinfo":
                        readDeviceInfoData(gatt);
                        break;
                }
            } else {
                if (callback != null)
                    callback.onError("Service discovery failed: " + status);
                cleanup();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                handleCharacteristicRead(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleCharacteristicRead(characteristic);
        }
    };

    private void readBattery(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(BATTERY_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic c = service.getCharacteristic(BATTERY_LEVEL_CHAR);
            if (c != null) {
                try {
                    gatt.readCharacteristic(c);
                    return;
                } catch (SecurityException e) {
                }
            }
        }
        if (callback != null)
            callback.onError("Battery service/char not found");
        cleanup();
    }

    private void readHeartRateData(BluetoothGatt gatt) {
        try {
            BluetoothGattService service = gatt.getService(HEART_RATE_SERVICE);
            if (service != null) {
                BluetoothGattCharacteristic c = service.getCharacteristic(HEART_RATE_CHAR);
                if (c != null) {
                    gatt.setCharacteristicNotification(c, true);
                    BluetoothGattDescriptor d = c.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (d != null) {
                        d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(d);

                        // Try to trigger measurement
                        BluetoothGattCharacteristic cp = service.getCharacteristic(HEART_RATE_CONTROL_POINT);
                        if (cp != null && (cp.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                            cp.setValue(new byte[] { 1 });
                            cp.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            // Small delay to let notification setup finish
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (bluetoothGatt != null)
                                    bluetoothGatt.writeCharacteristic(cp);
                            }, 500);
                        }
                        return;
                    }
                }
            }
            if (callback != null)
                callback.onError("HR service not found");
            cleanup();
        } catch (SecurityException e) {
            cleanup();
        }
    }

    private void readStepsData(BluetoothGatt gatt) {
        try {
            BluetoothGattService service = gatt.getService(XIAOMI_SERVICE);
            if (service != null) {
                BluetoothGattCharacteristic c = service.getCharacteristic(REALTIME_STEPS_CHAR);
                if (c != null) {
                    gatt.readCharacteristic(c);
                    return;
                }
            }
            if (callback != null)
                callback.onError("Steps (Xiaomi) service not found");
            cleanup();
        } catch (SecurityException e) {
            cleanup();
        }
    }

    private void readBPData(BluetoothGatt gatt, String servicesFound) {
        try {
            BluetoothGattService service = gatt.getService(BP_SERVICE);
            // Fallback: Check if it's in Health/Heart Rate service
            if (service == null)
                service = gatt.getService(HEART_RATE_SERVICE);
            if (service == null)
                service = gatt.getService(AE00_SERVICE);

            if (service != null) {
                BluetoothGattCharacteristic c = service.getCharacteristic(BP_MEASUREMENT_CHAR);
                if (c != null) {
                    gatt.setCharacteristicNotification(c, true);
                    BluetoothGattDescriptor d = c.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (d != null) {
                        d.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        gatt.writeDescriptor(d);
                        return;
                    }
                }
            }
            if (callback != null)
                callback.onError("BP srv/char not found. Avail: " + servicesFound);
            cleanup();
        } catch (SecurityException e) {
            cleanup();
        }
    }

    private void readSpO2Data(BluetoothGatt gatt, String servicesFound) {
        try {
            BluetoothGattService service = gatt.getService(SPO2_SERVICE);
            // Fallback: Check Heart Rate service or Xiaomi service
            if (service == null)
                service = gatt.getService(HEART_RATE_SERVICE);
            if (service == null)
                service = gatt.getService(XIAOMI_SERVICE);
            if (service == null)
                service = gatt.getService(AE00_SERVICE);

            if (service != null) {
                // Try standard SpO2 char
                BluetoothGattCharacteristic c = service.getCharacteristic(SPO2_CONTINUOUS_CHAR);
                // Fallback: Try a known "Raw/Sensor" char if standard fail
                if (c == null)
                    c = service.getCharacteristic(UUID.fromString("00000003-0000-1000-8000-00805f9b34fb")); // Custom
                                                                                                            // fallback
                if (c == null)
                    c = service.getCharacteristic(UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb")); // L8Star
                                                                                                            // SpO2
                if (c == null)
                    c = service.getCharacteristic(UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb")); // L8Star
                                                                                                            // alt
                if (c == null)
                    c = service.getCharacteristic(UUID.fromString("0000ae03-0000-1000-8000-00805f9b34fb")); // L8Star
                                                                                                            // alt2

                // If still not found, scan ALL characteristics and pick first one with NOTIFY
                if (c == null) {
                    Log.i(TAG,
                            "Standard chars not found, scanning all characteristics in service " + service.getUuid());
                    for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                        Log.i(TAG, "  Found char: " + ch.getUuid().toString() + " props=" + ch.getProperties());
                        if ((ch.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            Log.i(TAG, "  -> This char supports NOTIFY, using it!");
                            c = ch;
                            break;
                        }
                    }
                }

                if (c != null) {
                    Log.i(TAG, "Using characteristic: " + c.getUuid().toString());
                    gatt.setCharacteristicNotification(c, true);
                    BluetoothGattDescriptor d = c.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (d != null) {
                        d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(d);

                        // Try to trigger measurement
                        BluetoothGattCharacteristic cp = service.getCharacteristic(SPO2_CONTROL_POINT);
                        if (cp != null && (cp.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                            cp.setValue(new byte[] { 1 });
                            cp.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            // Small delay to let notification setup finish
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (bluetoothGatt != null)
                                    bluetoothGatt.writeCharacteristic(cp);
                            }, 500);
                        }
                        return;
                    } else {
                        Log.w(TAG, "Characteristic found but no CCCD descriptor!");
                    }
                }
            }
            if (callback != null)
                callback.onError("SpO2 srv/char not found. Avail: " + servicesFound);
            cleanup();
        } catch (SecurityException e) {
            cleanup();
        }
    }

    private String serialNumber = "", firmwareVersion = "", softwareVersion = "", manufacturer = "";
    private int deviceInfoReadCount = 0;

    private void readDeviceInfoData(BluetoothGatt gatt) {
        try {
            BluetoothGattService service = gatt.getService(DEVICE_INFO_SERVICE);
            if (service != null) {
                BluetoothGattCharacteristic c = service.getCharacteristic(SERIAL_NUMBER_CHAR);
                if (c != null) {
                    gatt.readCharacteristic(c);
                    return;
                }
            }
            if (callback != null)
                callback.onError("Device info invalid");
            cleanup();
        } catch (SecurityException e) {
            cleanup();
        }
    }

    private void handleCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(BATTERY_LEVEL_CHAR)) {
            int val = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            if (callback != null)
                callback.onBatteryLevel(val);
            cleanup();
        } else if (uuid.equals(HEART_RATE_CHAR)) {
            int flags = characteristic.getProperties();
            int format = -1;
            if ((flags & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            int bpm = characteristic.getIntValue(format, 1);
            if (callback != null)
                callback.onHeartRate(bpm);

            if (!isContinuousMonitoring) {
                cleanup();
            }
        } else if (uuid.equals(REALTIME_STEPS_CHAR)) {
            // Xiaomi format: 4 bytes (Little Endian)
            byte[] val = characteristic.getValue();
            if (val != null && val.length >= 4) {
                int steps = ByteBuffer.wrap(val).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if (callback != null)
                    callback.onSteps(steps);
            } else {
                if (callback != null)
                    callback.onSteps(0);
            }
            cleanup();
        } else if (uuid.equals(BP_MEASUREMENT_CHAR)) {
            // Handle complex BP flags if needed, simple hex dump for now or standard
            // Typically float values
            String bpVal = "120/80 (Simulated)"; // Parsing BP is complex without proper specific byte alignment logic
            if (callback != null)
                callback.onBloodPressure(characteristic.getStringValue(0));
            cleanup();
        } else if (uuid.equals(SPO2_CONTINUOUS_CHAR)) {
            // 2nd byte usually O2
            byte[] val = characteristic.getValue();
            if (val != null && val.length > 1) {
                int spo2 = val[1];
                if (callback != null)
                    callback.onSpO2(spo2);
            }
            cleanup();
        } else if (uuid.equals(SERIAL_NUMBER_CHAR)) {
            serialNumber = characteristic.getStringValue(0);
            deviceInfoReadCount++;
            readNextDeviceInfo();
        } else if (uuid.equals(FIRMWARE_VERSION_CHAR)) {
            firmwareVersion = characteristic.getStringValue(0);
            deviceInfoReadCount++;
            readNextDeviceInfo();
        } else if (uuid.equals(SOFTWARE_VERSION_CHAR)) {
            softwareVersion = characteristic.getStringValue(0);
            deviceInfoReadCount++;
            readNextDeviceInfo();
        } else if (uuid.equals(MANUFACTURER_CHAR)) {
            manufacturer = characteristic.getStringValue(0);
            deviceInfoReadCount++;
            readNextDeviceInfo();
        }
    }

    private void readNextDeviceInfo() {
        try {
            BluetoothGattService service = bluetoothGatt.getService(DEVICE_INFO_SERVICE);
            BluetoothGattCharacteristic next = null;
            if (deviceInfoReadCount == 1)
                next = service.getCharacteristic(FIRMWARE_VERSION_CHAR);
            else if (deviceInfoReadCount == 2)
                next = service.getCharacteristic(SOFTWARE_VERSION_CHAR);
            else if (deviceInfoReadCount == 3)
                next = service.getCharacteristic(MANUFACTURER_CHAR);
            else {
                if (callback != null)
                    callback.onDeviceInfo(serialNumber, firmwareVersion, softwareVersion, manufacturer);
                cleanup();
                return;
            }
            if (next != null)
                bluetoothGatt.readCharacteristic(next);
            else {
                deviceInfoReadCount++;
                readNextDeviceInfo();
            }
        } catch (SecurityException e) {
        }
    }

    private void cleanup() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.close();
            } catch (Exception e) {
            }
            bluetoothGatt = null;
        }
    }
}
