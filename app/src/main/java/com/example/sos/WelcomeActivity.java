package com.example.sos;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class WelcomeActivity extends AppCompatActivity {

    MaterialButton btnGetStarted;
    TextView tvSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
            finish();
            return;
        }

        // Set status bar and navigation bar color to red
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
            window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.transparent));

            // Make status bar and navigation bar red
            window.setStatusBarColor(0xFFEF3A3A); // Red color
            window.setNavigationBarColor(0xFFEF3A3A); // Red color

            // Set light status bar icons for better visibility on red background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                // Remove light status bar flag to make icons white/light
                decorView.setSystemUiVisibility(
                        decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            // Set light navigation bar icons for better visibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decorView = window.getDecorView();
                // Remove light navigation bar flag to make icons white/light
                decorView.setSystemUiVisibility(
                        decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }

        setContentView(R.layout.activity_welcome);

        btnGetStarted = findViewById(R.id.btnGetStarted);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Initialize animations
        initAnimations();

        // Get Started button - Navigate to Signup
        btnGetStarted.setOnClickListener(v -> {
            // Add click animation
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
            startActivity(new Intent(WelcomeActivity.this, SignupActivity.class));
        });

        // Sign In text - Navigate to Login
        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });
    }

    private void initAnimations() {
        // Get all views
        View headerLayout = findViewById(R.id.headerLayout);
        View circleBg = findViewById(R.id.circleBg);
        View womanImage = findViewById(R.id.womanImage);
        MaterialCardView bottomCard = (MaterialCardView) btnGetStarted.getParent().getParent();

        // 1. Header Animation - Slide in from left with fade
        headerLayout.setTranslationX(-300f);
        headerLayout.setAlpha(0f);
        headerLayout.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 2. Circle Background - Rotate and scale in
        circleBg.setScaleX(0.5f);
        circleBg.setScaleY(0.5f);
        circleBg.setAlpha(0f);
        circleBg.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .withEndAction(() -> {
                    // Add continuous slow rotation
                    ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(circleBg, "rotation", 0f, 360f);
                    rotateAnim.setDuration(20000);
                    rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
                    rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                    rotateAnim.start();
                })
                .start();

        // 3. Woman Image - Fade in with scale and float effect
        womanImage.setScaleX(0.8f);
        womanImage.setScaleY(0.8f);
        womanImage.setAlpha(0f);
        womanImage.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(400)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    // Add floating animation
                    ObjectAnimator floatUp = ObjectAnimator.ofFloat(womanImage, "translationY", 0f, -20f, 0f);
                    floatUp.setDuration(3000);
                    floatUp.setRepeatCount(ValueAnimator.INFINITE);
                    floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
                    floatUp.start();
                })
                .start();

        // 4. Bottom Card - Slide up from bottom with bounce
        bottomCard.setTranslationY(500f);
        bottomCard.setAlpha(0f);
        bottomCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(600)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();

        // 5. Button Pulse Animation - Continuous attention grabber
        btnGetStarted.postDelayed(() -> {
            ObjectAnimator pulseX = ObjectAnimator.ofFloat(btnGetStarted, "scaleX", 1f, 1.05f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(btnGetStarted, "scaleY", 1f, 1.05f, 1f);
            pulseX.setDuration(1500);
            pulseY.setDuration(1500);
            pulseX.setRepeatCount(ValueAnimator.INFINITE);
            pulseY.setRepeatCount(ValueAnimator.INFINITE);
            pulseX.start();
            pulseY.start();
        }, 1600);

        // 6. Sign In Text - Subtle fade in
        tvSignIn.setAlpha(0f);
        tvSignIn.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(1400)
                .start();
    }
}
