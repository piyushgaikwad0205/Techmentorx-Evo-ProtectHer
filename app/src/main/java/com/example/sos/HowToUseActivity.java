package com.example.sos;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class HowToUseActivity extends AppCompatActivity {

        private ViewPager2 viewPager;
        private TabLayout tabLayout;
        private MaterialButton btnNext, btnBack, btnSkip;
        private HowToUseAdapter adapter;
        private List<TutorialStep> tutorialSteps;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_how_to_use);

                initViews();
                setupTutorialData();
                setupViewPager();
                setupButtons();
        }

        private void initViews() {
                viewPager = findViewById(R.id.viewPager);
                tabLayout = findViewById(R.id.tabLayout);
                btnNext = findViewById(R.id.btnNext);
                btnBack = findViewById(R.id.btnBack);
                btnSkip = findViewById(R.id.btnSkip);
        }

        private void setupTutorialData() {
                tutorialSteps = new ArrayList<>();

                // 1. Introduction
                tutorialSteps.add(new TutorialStep(
                                "Welcome to Shakti",
                                "Your comprehensive personal safety companion protecting you 24/7.",
                                R.drawable.women_sos_logo, // App Icon
                                TutorialStep.TYPE_STANDARD,
                                null));

                // 2. Setup Profile & Contacts
                List<String> profileChecklist = new ArrayList<>();
                profileChecklist.add("Fill Name & Medical Info");
                profileChecklist.add("Add Trusted Contacts");
                profileChecklist.add("Grant Permisions");

                tutorialSteps.add(new TutorialStep(
                                "Step 1: Setup",
                                "One-time setup to ensure we can help you effectively.",
                                R.drawable.ic_person,
                                TutorialStep.TYPE_CHECKLIST,
                                profileChecklist));

                // 3. Activation Methods (Triggers) - KEY ADDITION
                List<String> triggersList = new ArrayList<>();
                triggersList.add("Shake Phone: Shake vigorously");
                triggersList.add("Power Button: Press 3-4 times");
                triggersList.add("Voice: Say 'Help' or 'Siren'");

                tutorialSteps.add(new TutorialStep(
                                "How to Activate SOS",
                                "Trigger help instantly without unlocking your phone.",
                                R.drawable.shake,
                                TutorialStep.TYPE_CHECKLIST,
                                triggersList));

                // 4. Emergency Services
                List<TutorialStep.EmergencyType> emergencyTypes = new ArrayList<>();
                emergencyTypes.add(new TutorialStep.EmergencyType("Police", "SMS + Location", R.drawable.police_badge));
                emergencyTypes.add(new TutorialStep.EmergencyType("Ambulance", "Call + Medical Data",
                                R.drawable.ambulance));
                emergencyTypes.add(new TutorialStep.EmergencyType("Siren", "Loud Alarm", R.drawable.siren));
                emergencyTypes.add(new TutorialStep.EmergencyType("Review", "Record Evidence", R.drawable.camera_icon));

                tutorialSteps.add(new TutorialStep(
                                "Emergency Actions",
                                "Specific tools for every situation.",
                                R.drawable.crisis_alert,
                                TutorialStep.TYPE_GRID,
                                emergencyTypes));

                // 5. Automatic Protection (Smart Features)
                List<String> featureList = new ArrayList<>();
                featureList.add("📍 Live Location Sharing");
                featureList.add("📸 Auto-Camera Evidence");
                featureList.add("🛡️ Background Monitoring");

                tutorialSteps.add(new TutorialStep(
                                "Smart Protection",
                                "We do the work so you can focus on staying safe.",
                                R.drawable.ic_shield,
                                TutorialStep.TYPE_CHECKLIST,
                                featureList));

                // 6. Voice Commands
                List<TutorialStep.EmergencyType> voiceCommands = new ArrayList<>();
                voiceCommands.add(new TutorialStep.EmergencyType("'HELP'", "General SOS", R.drawable.ic_item_mic)); // Assuming
                                                                                                                    // mic
                                                                                                                    // icon
                                                                                                                    // exists
                                                                                                                    // or
                                                                                                                    // generic
                voiceCommands.add(new TutorialStep.EmergencyType("'POLICE'", "Alerts Police", R.drawable.police_badge));
                voiceCommands.add(new TutorialStep.EmergencyType("'DOCTOR'", "Medical Alert", R.drawable.hospital));
                voiceCommands.add(new TutorialStep.EmergencyType("'SIREN'", "Loud Alarm", R.drawable.siren));

                tutorialSteps.add(new TutorialStep(
                                "Voice Commands",
                                "Just speak to trigger help. Works even if phone is in pocket.",
                                R.drawable.mic,
                                TutorialStep.TYPE_GRID,
                                voiceCommands));

                // 7. Testing Mode
                tutorialSteps.add(new TutorialStep(
                                "Practice Safely",
                                "Use 'Test Mode' to try all features without calling real authorities. Be prepared, not scared.",
                                R.drawable.ic_settings,
                                TutorialStep.TYPE_STANDARD,
                                null));

                // 8. You're Ready
                tutorialSteps.add(new TutorialStep(
                                "You Are Protected",
                                "Shakti is active. Keep your battery charged and location on.",
                                R.drawable.ic_check_circle,
                                TutorialStep.TYPE_FINAL,
                                null));
        }

        private void setupViewPager() {
                adapter = new HowToUseAdapter(this, tutorialSteps);
                viewPager.setAdapter(adapter);

                new TabLayoutMediator(tabLayout, viewPager,
                                (tab, position) -> {
                                        // No text for tabs, just dots
                                }).attach();

                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                updateButtonState(position);
                        }
                });
        }

        private void setupButtons() {
                btnNext.setOnClickListener(v -> {
                        int currentItem = viewPager.getCurrentItem();
                        if (currentItem < tutorialSteps.size() - 1) {
                                viewPager.setCurrentItem(currentItem + 1);
                        } else {
                                finish(); // Or navigate to Home
                        }
                });

                btnBack.setOnClickListener(v -> {
                        int currentItem = viewPager.getCurrentItem();
                        if (currentItem > 0) {
                                viewPager.setCurrentItem(currentItem - 1);
                        }
                });

                btnSkip.setOnClickListener(v -> finish());
        }

        private void updateButtonState(int position) {
                // Back button visibility
                if (position == 0) {
                        btnBack.setVisibility(View.INVISIBLE);
                } else {
                        btnBack.setVisibility(View.VISIBLE);
                }

                // Next button text
                if (position == tutorialSteps.size() - 1) {
                        btnNext.setText("Start");
                        btnSkip.setVisibility(View.INVISIBLE);
                } else {
                        btnNext.setText("Next");
                        btnSkip.setVisibility(View.VISIBLE);
                }
        }

        // Data Models
        public static class TutorialStep {
                public static final int TYPE_STANDARD = 0;
                public static final int TYPE_CHECKLIST = 1;
                public static final int TYPE_GRID = 2;
                public static final int TYPE_FINAL = 3;

                public String title;
                public String subtitle;
                public int imageResId;
                public int type;
                public Object contentData;

                public TutorialStep(String title, String subtitle, int imageResId, int type, Object contentData) {
                        this.title = title;
                        this.subtitle = subtitle;
                        this.imageResId = imageResId;
                        this.type = type;
                        this.contentData = contentData;
                }

                public static class EmergencyType {
                        public String name;
                        public String desc;
                        public int iconResId;

                        public EmergencyType(String name, String desc, int iconResId) {
                                this.name = name;
                                this.desc = desc;
                                this.iconResId = iconResId;
                        }
                }
        }
}
