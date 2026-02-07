package com.example.sos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity {

    GridView gridView;
    ArrayList<File> imageFiles;
    TextView tvEmptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        gridView = findViewById(R.id.gridView);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // Set status bar to Red
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.parseColor("#F92A2A"));
        }

        // Initialize header back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Ensure 3 columns for better layout
        if (gridView != null) {
            gridView.setNumColumns(3);
            gridView.setVerticalSpacing(8);
            gridView.setHorizontalSpacing(8);
            gridView.setPadding(8, 8, 8, 8);
            gridView.setClipToPadding(false);
            gridView.setVisibility(View.GONE); // Hide initially
        }

        if (tvEmptyMessage != null)
            tvEmptyMessage.setVisibility(View.GONE);

        imageFiles = new ArrayList<>();

        authenticateUser();
    }

    private void authenticateUser() {
        java.util.concurrent.Executor executor = androidx.core.content.ContextCompat.getMainExecutor(this);
        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(
                GalleryActivity.this,
                executor, new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                            @androidx.annotation.NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(getApplicationContext(), "Authentication error: " + errString,
                                Toast.LENGTH_SHORT).show();
                        finish(); // Close activity if auth failed/cancelled
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @androidx.annotation.NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Auth success, load content
                        loadImages();
                        if (imageFiles.isEmpty()) {
                            if (tvEmptyMessage != null)
                                tvEmptyMessage.setVisibility(View.VISIBLE);
                        } else {
                            if (gridView != null) {
                                gridView.setVisibility(View.VISIBLE);
                                GalleryAdapter adapter = new GalleryAdapter();
                                gridView.setAdapter(adapter);
                            }
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Evidence Gallery")
                .setSubtitle("Use fingerprint or device credential to access")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void loadImages() {
        File photoDir = new File(getFilesDir(), "evidence_photos");
        if (photoDir.exists()) {
            File[] files = photoDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jpg")) {
                        imageFiles.add(file);
                    }
                }
            }
        }

        if (imageFiles.isEmpty()) {
            Toast.makeText(this, "No evidence found.", Toast.LENGTH_SHORT).show();
        }
    }

    private class GalleryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return imageFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return imageFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evidence, parent, false);
            }

            ImageView imageView = view.findViewById(R.id.imgEvidence);
            File imgFile = imageFiles.get(position);

            if (imgFile.exists()) {
                // Decode bitmap with options optionally to save memory
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imageView.setImageBitmap(myBitmap);
            }

            view.setOnClickListener(v -> {
                // Share or view Intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(GalleryActivity.this,
                        getApplicationContext().getPackageName() + ".provider", imgFile);
                intent.setDataAndType(uri, "image/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "View Evidence"));
            });

            return view;
        }
    }
}
