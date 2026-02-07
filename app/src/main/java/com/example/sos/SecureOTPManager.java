package com.example.sos;

import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Secure OTP Manager with hashing, expiry, and attempt limiting
 * Designed to be carrier-friendly and secure
 */
public class SecureOTPManager {

    private static final String TAG = "SecureOTPManager";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    private static class OTPData {
        String hashedOTP;
        long expiryTime;
        int attempts;

        OTPData(String hashedOTP, long expiryTime) {
            this.hashedOTP = hashedOTP;
            this.expiryTime = expiryTime;
            this.attempts = 0;
        }
    }

    private final Map<String, OTPData> otpStorage = new HashMap<>();

    private static final String[] WORD_LIST = {
            "SKY", "BLUE", "SAFE", "STAR", "MOON", "TREE", "FISH",
            "BIRD", "CAT", "DOG", "RED", "GOLD", "LAKE", "RAIN",
            "WIND", "SNOW", "SUN", "ROSE", "LILY", "JUMP"
    };

    /**
     * Generate a random 3-word phrase OTP (e.g., "SKY-BLUE-MOON")
     */
    public String generateOTP() {
        SecureRandom random = new SecureRandom();
        StringBuilder phrase = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            if (i > 0)
                phrase.append("-");
            phrase.append(WORD_LIST[random.nextInt(WORD_LIST.length)]);
        }
        return phrase.toString();
    }

    /**
     * Hash OTP using SHA-256
     */
    private String hashOTP(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Hashing failed", e);
            return otp; // Fallback
        }
    }

    /**
     * Store OTP with expiry time
     */
    public void storeOTP(String phoneNumber, String otp) {
        String hashedOTP = hashOTP(otp);
        long expiryTime = System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60 * 1000);
        otpStorage.put(phoneNumber, new OTPData(hashedOTP, expiryTime));
        cleanExpiredOTPs();
        Log.d(TAG, "OTP stored for: " + phoneNumber + " (expires in " + OTP_EXPIRY_MINUTES + " min)");
    }

    /**
     * Verify entered OTP
     * 
     * @return VerificationResult with status and message
     */
    public VerificationResult verifyOTP(String phoneNumber, String enteredOTP) {
        OTPData otpData = otpStorage.get(phoneNumber);

        if (otpData == null) {
            return new VerificationResult(false, "No OTP found. Please request a new one.");
        }

        // Check expiry
        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpStorage.remove(phoneNumber);
            return new VerificationResult(false, "OTP expired. Please request a new one.");
        }

        // Check attempts
        if (otpData.attempts >= MAX_ATTEMPTS) {
            otpStorage.remove(phoneNumber);
            return new VerificationResult(false, "Too many incorrect attempts. Please request a new OTP.");
        }

        // Verify OTP
        // Normalize input: Uppercase, trim, replace spaces with hyphens to match format
        String normalizedInput = enteredOTP.toUpperCase().trim().replace(" ", "-");

        String enteredHash = hashOTP(normalizedInput);
        if (enteredHash.equals(otpData.hashedOTP)) {
            otpStorage.remove(phoneNumber);
            Log.d(TAG, "OTP verified successfully for: " + phoneNumber);
            return new VerificationResult(true, "Verified successfully!");
        } else {
            otpData.attempts++;
            int remainingAttempts = MAX_ATTEMPTS - otpData.attempts;

            if (remainingAttempts > 0) {
                return new VerificationResult(false,
                        String.format("Incorrect OTP. %d attempts remaining.", remainingAttempts));
            } else {
                otpStorage.remove(phoneNumber);
                return new VerificationResult(false,
                        "Too many incorrect attempts. Please request a new OTP.");
            }
        }
    }

    /**
     * Clean up expired OTPs
     */
    private void cleanExpiredOTPs() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, OTPData>> iterator = otpStorage.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, OTPData> entry = iterator.next();
            if (entry.getValue().expiryTime < currentTime) {
                iterator.remove();
                Log.d(TAG, "Cleaned expired OTP for: " + entry.getKey());
            }
        }
    }

    /**
     * Cancel OTP for a phone number
     */
    public void cancelOTP(String phoneNumber) {
        otpStorage.remove(phoneNumber);
        Log.d(TAG, "OTP cancelled for: " + phoneNumber);
    }

    /**
     * Get OTP expiry time in minutes
     */
    public int getExpiryMinutes() {
        return OTP_EXPIRY_MINUTES;
    }

    /**
     * Result class for verification
     */
    public static class VerificationResult {
        public final boolean success;
        public final String message;

        VerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
