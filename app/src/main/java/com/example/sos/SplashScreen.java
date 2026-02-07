package com.example.sos;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize and apply theme before setting content view
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.initTheme();

        setContentView(R.layout.activity_splash_screen);

        // Get views for animation
        android.view.View rootLayout = findViewById(android.R.id.content);
        android.view.View logoContainer = findViewById(R.id.logoContainer);
        android.view.View appName = findViewById(R.id.appName);
        android.view.View appTagline = findViewById(R.id.appTagline);
        android.view.View shimmerOverlay = findViewById(R.id.shimmerOverlay);

        // Background Color Animation - Pulsing effect
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFFCC0000, 0xFFFF0000, 0xFFCC0000);
        colorAnimator.setDuration(2000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.addUpdateListener(animator -> rootLayout.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimator.start();

        // Shimmer Animation - Sweep across screen
        ObjectAnimator shimmerAnim = ObjectAnimator.ofFloat(shimmerOverlay, "translationX",
                -200f, getResources().getDisplayMetrics().widthPixels + 200f);
        shimmerAnim.setDuration(2500);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.setRepeatMode(ValueAnimator.RESTART);
        shimmerAnim.setStartDelay(1500);
        shimmerAnim.start();

        // Logo Container Animation - Fade in + Scale + Rotation
        logoContainer.setScaleX(0.3f);
        logoContainer.setScaleY(0.3f);
        logoContainer.setRotation(-180f);

        logoContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(1200)
                .setStartDelay(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .withEndAction(() -> {
                    // Add continuous pulsing effect after initial animation
                    ObjectAnimator pulseX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 1f, 1.05f, 1f);
                    ObjectAnimator pulseY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 1f, 1.05f, 1f);
                    pulseX.setDuration(1500);
                    pulseY.setDuration(1500);
                    pulseX.setRepeatCount(ValueAnimator.INFINITE);
                    pulseY.setRepeatCount(ValueAnimator.INFINITE);
                    pulseX.start();
                    pulseY.start();
                })
                .start();

        // App Name Animation - Fade in with slide up and bounce
        appName.setTranslationY(50f);
        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(1000)
                .setDuration(800)
                .setInterpolator(new android.view.animation.BounceInterpolator())
                .start();

        // Tagline Animation - Fade in with slide up and scale
        appTagline.setTranslationY(30f);
        appTagline.setScaleX(0.8f);
        appTagline.setScaleY(0.8f);
        appTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(1400)
                .setDuration(800)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                .start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    // Navigate to Welcome screen for new users
                    startActivity(new Intent(SplashScreen.this, WelcomeActivity.class));
                    finish();
                } else {
                    // Check profile completeness
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists() && documentSnapshot.contains("name")
                                        && documentSnapshot.contains("phone") && documentSnapshot.contains("age")) {
                                    // Profile complete, now check Medical Info
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(currentUser.getUid())
                                            .collection("medical_info").document("details")
                                            .get()
                                            .addOnSuccessListener(medSnap -> {
                                                if (medSnap.exists() && medSnap.contains("bloodType")) {
                                                    // Everything complete - Set local flags to pass HomeActivity checks
                                                    getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                                            .edit()
                                                            .putBoolean("ProfileCompleted", true)
                                                            .apply();

                                                    // Set dummy bloodType to satisfy .contains check in HomeActivity
                                                    getSharedPreferences("MedicalInfo", MODE_PRIVATE)
                                                            .edit()
                                                            .putString("bloodType", medSnap.getString("bloodType"))
                                                            .apply();

                                                    startActivity(new Intent(SplashScreen.this, HomeActivity.class));
                                                } else {
                                                    // Medical incomplete
                                                    Intent intent = new Intent(SplashScreen.this,
                                                            MedicalInfoActivity.class);
                                                    intent.putExtra("ONBOARDING", true);
                                                    startActivity(intent);
                                                }
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Fallback to Home if error checking medical (or maybe Medical activity
                                                // safe?)
                                                // Better safe: go to Home, Home will do its own check if needed
                                                startActivity(new Intent(SplashScreen.this, HomeActivity.class));
                                                finish();
                                            });
                                } else {
                                    // Profile incomplete
                                    Intent intent = new Intent(SplashScreen.this, ProfileActivity.class);
                                    intent.putExtra("ONBOARDING", true);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Fallback to Home
                                startActivity(new Intent(SplashScreen.this, HomeActivity.class));
                                finish();
                            });
                }
            }
        }, 2500);
    }
}