package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private MaterialButton btnStartCapture, btnStopCapture;
    private TextView tvStatus;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable captureRunnable;
    private boolean isMonitoring = false;
    private static final long CAPTURE_INTERVAL = 30 * 1000; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        btnStartCapture = findViewById(R.id.btnStartCapture);
        btnStopCapture = findViewById(R.id.btnStopCapture);
        tvStatus = findViewById(R.id.tvStatus);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 10);
        }

        btnStartCapture.setOnClickListener(v -> startMonitoring());
        btnStopCapture.setOnClickListener(v -> stopMonitoring());

        MaterialButton btnViewGallery = findViewById(R.id.btnViewGallery);
        btnViewGallery.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, GalleryActivity.class);
            startActivity(intent);
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Check for Auto Start
        if (getIntent().getBooleanExtra("AUTO_START", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::startMonitoring, 1500); // Small delay to let camera
                                                                                          // init
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                } catch (Exception exc) {
                    Log.e("CameraActivity", "Use case binding failed", exc);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraActivity", "Camera provider fail", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startMonitoring() {
        if (imageCapture == null)
            return;

        isMonitoring = true;
        btnStartCapture.setEnabled(false);
        btnStopCapture.setEnabled(true);
        tvStatus.setText("Evidence Mode: ACTIVE\nCapturing in 30s intervals...");

        // Take first photo immediately
        takePhoto();

        // Schedule periodic photos
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    takePhoto();
                    timerHandler.postDelayed(this, CAPTURE_INTERVAL);
                }
            }
        };
        timerHandler.postDelayed(captureRunnable, CAPTURE_INTERVAL);
    }

    private void stopMonitoring() {
        isMonitoring = false;
        if (captureRunnable != null) {
            timerHandler.removeCallbacks(captureRunnable);
        }
        btnStartCapture.setEnabled(true);
        btnStopCapture.setEnabled(false);
        tvStatus.setText("Evidence Mode: STOPPED");
        Toast.makeText(this, "Monitoring Stopped", Toast.LENGTH_SHORT).show();
    }

    private void takePhoto() {
        if (imageCapture == null)
            return;

        // Create output file in app-specific storage (Secure)
        File photoDir = new File(getFilesDir(), "evidence_photos");
        if (!photoDir.exists())
            photoDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        File photoFile = new File(photoDir, "EVIDENCE_" + timeStamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(CameraActivity.this, "Evidence Captured: " + photoFile.getName(),
                                Toast.LENGTH_SHORT).show();
                        Log.d("CameraActivity", "Photo saved: " + photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraActivity", "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        cameraExecutor.shutdown();
    }
}
