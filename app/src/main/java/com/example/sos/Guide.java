package com.example.sos;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

public class Guide extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnSkip;
    private LinearLayout indicatorLayout;
    private GuideSlide[] slides;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize theme
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.initTheme();

        setContentView(R.layout.activity_guide);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        indicatorLayout = findViewById(R.id.indicatorLayout);

        // Define guide slides
        slides = new GuideSlide[] {
                new GuideSlide(
                        R.drawable.crisis_alert,
                        "Welcome to SHAKTI",
                        "Your personal safety companion. Stay protected with smart emergency features."),
                new GuideSlide(
                        R.drawable.emergency,
                        "Quick SOS Activation",
                        "Shake your phone or press power button 3 times to trigger emergency alert instantly."),
                new GuideSlide(
                        R.drawable.users_icon,
                        "Add Trusted Contacts",
                        "Add emergency contacts who will receive your SOS alerts with your location."),
                new GuideSlide(
                        R.drawable.maps,
                        "Live Location Sharing",
                        "Your real-time location is shared with emergency contacts when SOS is triggered."),
                new GuideSlide(
                        R.drawable.ic_security,
                        "AI Safety Analysis",
                        "Get AI-powered safety recommendations based on your surroundings and situation."),
                new GuideSlide(
                        R.drawable.verified_user,
                        "You're All Set!",
                        "Stay safe and remember: help is just a shake away. Tap 'Get Started' to begin.")
        };

        // Setup ViewPager
        SlideAdapter adapter = new SlideAdapter(slides);
        viewPager.setAdapter(adapter);

        // Setup indicators
        setupIndicators(slides.length);
        setCurrentIndicator(0);

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                setCurrentIndicator(position);

                if (position == slides.length - 1) {
                    btnNext.setText("Get Started");
                    btnNext.setIcon(null);
                } else {
                    btnNext.setText("Next");
                    btnNext.setIconResource(R.drawable.arrow_forward_ios);
                }
            }
        });

        // Next button click
        btnNext.setOnClickListener(v -> {
            if (currentPage == slides.length - 1) {
                finish();
            } else {
                viewPager.setCurrentItem(currentPage + 1);
            }
        });

        // Skip button click
        btnSkip.setOnClickListener(v -> finish());
    }

    private void setupIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageResource(R.drawable.circle_background);
            indicators[i].setLayoutParams(params);
            indicators[i].setColorFilter(Color.GRAY);
            indicatorLayout.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int position) {
        int childCount = indicatorLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) indicatorLayout.getChildAt(i);
            if (i == position) {
                imageView.setColorFilter(getColor(R.color.colorPrimary));
            } else {
                imageView.setColorFilter(Color.GRAY);
            }
        }
    }

    // Guide Slide Model
    static class GuideSlide {
        int imageRes;
        String title;
        String description;

        GuideSlide(int imageRes, String title, String description) {
            this.imageRes = imageRes;
            this.title = title;
            this.description = description;
        }
    }

    // ViewPager Adapter
    class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideViewHolder> {
        private GuideSlide[] slides;

        SlideAdapter(GuideSlide[] slides) {
            this.slides = slides;
        }

        @NonNull
        @Override
        public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide_slide, parent, false);
            return new SlideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
            GuideSlide slide = slides[position];
            holder.image.setImageResource(slide.imageRes);
            holder.title.setText(slide.title);
            holder.description.setText(slide.description);
        }

        @Override
        public int getItemCount() {
            return slides.length;
        }

        class SlideViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, description;

            SlideViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.slideImage);
                title = itemView.findViewById(R.id.slideTitle);
                description = itemView.findViewById(R.id.slideDescription);
            }
        }
    }
}