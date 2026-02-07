package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executor;

public class TamperAlertActivity extends AppCompatActivity {

    private TextView tvCountdown;
    private MaterialButton btnAuthenticate;

    private CountDownTimer countDownTimer;
    private Vibrator vibrator;
    private int secondsRemaining = 20;
    private boolean isCancelled = false;
    private boolean isAuthenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tamper_alert);

        // Keep screen on and show over lockscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        tvCountdown = findViewById(R.id.tvCountdown);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Start continuous vibration
        startContinuousVibration();

        // Button listener
        btnAuthenticate.setOnClickListener(v -> showBiometricPrompt());

        // Start countdown
        startCountdown();

        // Auto-show biometric prompt
        showBiometricPrompt();
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) (millisUntilFinished / 1000);
                tvCountdown.setText(String.valueOf(secondsRemaining));

                // Pulse animation
                tvCountdown.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            tvCountdown.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(300)
                                    .start();
                        })
                        .start();
            }

            @Override
            public void onFinish() {
                if (!isCancelled && !isAuthenticated) {
                    triggerSOS();
                }
            }
        }.start();
    }

    private void startContinuousVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Continuous strong vibration pattern
                long[] pattern = { 0, 500, 200, 500, 200 };
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                vibrator.vibrate(effect);
            } else {
                // Legacy vibration
                long[] pattern = { 0, 500, 200, 500, 200 };
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);

        if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(TamperAlertActivity.this,
                                    "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        isAuthenticated = true;
                        authenticatedSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(TamperAlertActivity.this,
                                "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to Cancel SOS")
                .setSubtitle("Verify your identity to prevent SOS trigger")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void authenticatedSuccess() {
        Toast.makeText(this, "Authentication successful! Alert cancelled.", Toast.LENGTH_SHORT).show();
        cancelAlert();
    }

    private void cancelAlert() {
        isCancelled = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        stopVibration();

        Toast.makeText(this, "Tamper alert cancelled", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void triggerSOS() {
        stopVibration();

        Toast.makeText(this, "SOS TRIGGERED - Notifying emergency contacts!", Toast.LENGTH_LONG).show();

        // Trigger SOS service
        Intent sosIntent = new Intent(this, ServiceMine.class);
        sosIntent.putExtra("trigger_source", "tamper_detection");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sosIntent);
        } else {
            startService(sosIntent);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopVibration();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing alert
        Toast.makeText(this, "Please authenticate to dismiss", Toast.LENGTH_SHORT).show();
    }
}
