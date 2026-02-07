package com.example.sos;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SOSMessageFormatter {

    /**
     * Creates a professional, beautifully formatted SOS message
     * 
     * @param context       Application context
     * @param location      User's current location
     * @param emergencyType Type of emergency (SOS, POLICE, MEDICAL, etc.)
     * @return Formatted message string
     */
    public static String createProfessionalSOSMessage(Context context, Location location, String emergencyType) {
        StringBuilder message = new StringBuilder();

        message.append("🚨 SOS EMERGENCY 🚨\n");
        message.append("I need immediate help!\n");

        if (location != null) {
            // Precise numeric location with Google Maps Link
            message.append("\n📍 Location:\n");
            message.append("http://maps.google.com/?q=").append(location.getLatitude()).append(",")
                    .append(location.getLongitude());
        } else {
            message.append("\n📍 Location: GPS Searching...");
        }

        // Battery
        android.os.BatteryManager bm = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            int batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            message.append("\n\n🔋 Battery: ").append(batteryLevel).append("%");
        }

        return message.toString();
    }

    // Helper to convert numbers to words (e.g. 28.5 -> Two Eight dot Five)
    public static String convertToWords(double value) {
        String str = String.format(Locale.US, "%.5f", value);
        StringBuilder result = new StringBuilder();

        for (char c : str.toCharArray()) {
            switch (c) {
                case '0':
                    result.append("Zero ");
                    break;
                case '1':
                    result.append("One ");
                    break;
                case '2':
                    result.append("Two ");
                    break;
                case '3':
                    result.append("Three ");
                    break;
                case '4':
                    result.append("Four ");
                    break;
                case '5':
                    result.append("Five ");
                    break;
                case '6':
                    result.append("Six ");
                    break;
                case '7':
                    result.append("Seven ");
                    break;
                case '8':
                    result.append("Eight ");
                    break;
                case '9':
                    result.append("Nine ");
                    break;
                case '.':
                    result.append("Dot ");
                    break;
                case '-':
                    result.append("Minus ");
                    break;
                default:
                    break;
            }
        }
        return result.toString().trim();
    }

    // Helper to convert OTP digits to words (e.g. "123" -> "One Two Three")
    public static String convertDigitsToWords(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '0':
                    result.append("Zero ");
                    break;
                case '1':
                    result.append("One ");
                    break;
                case '2':
                    result.append("Two ");
                    break;
                case '3':
                    result.append("Three ");
                    break;
                case '4':
                    result.append("Four ");
                    break;
                case '5':
                    result.append("Five ");
                    break;
                case '6':
                    result.append("Six ");
                    break;
                case '7':
                    result.append("Seven ");
                    break;
                case '8':
                    result.append("Eight ");
                    break;
                case '9':
                    result.append("Nine ");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString().trim();
    }

    /**
     * Creates a compact SOS message for SMS length limits
     */
    public static String createCompactSOSMessage(Context context, Location location, String emergencyType) {
        StringBuilder message = new StringBuilder();
        message.append("SOS ").append(emergencyType).append("\n");

        if (location != null) {
            // Compact Word Format: "Lat Two Eight..."
            message.append("L: ").append(convertToWords(location.getLatitude())).append("\n");
            message.append("L: ").append(convertToWords(location.getLongitude()));
        } else {
            message.append("Loc: Searching");
        }
        return message.toString();
    }

    // Keep other methods but make them safe too if needed, or leave as is if not
    // used for SMS.
    // Assuming createLiveTrackingMessage might be SMS too.

    public static String createLiveTrackingMessage(Context context, Location location) {
        StringBuilder message = new StringBuilder();
        message.append("TRACKING UPDATE\n");
        if (location != null) {
            message.append("Lat: ").append(convertToWords(location.getLatitude())).append("\n");
            message.append("Lon: ").append(convertToWords(location.getLongitude()));
        }
        return message.toString();
    }

    public static String createRouteDeviationMessage(Context context, Location location) {
        return "ALERT: Route Deviation!\n" + createLiveTrackingMessage(context, location);
    }

    public static String createNearbyHelpMessage(Context context, Location location) {
        return "HELP NEEDED NEARBY\n" + createLiveTrackingMessage(context, location);
    }
}
