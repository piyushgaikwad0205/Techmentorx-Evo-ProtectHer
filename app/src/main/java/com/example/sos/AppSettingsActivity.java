package com.example.sos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.card.MaterialCardView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class AppSettingsActivity extends AppCompatActivity {

    MaterialSwitch switchVoiceParams, switchDarkMode, switchShakeDetection;
    Slider sliderShake, sliderVoiceRange;
    MaterialCardView btnEditMedicalSos, btnEditContactsSos, btnEditKeywordSos;
    // MaterialToolbar topAppBar;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // topAppBar = findViewById(R.id.topAppBar);
        // topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        switchVoiceParams = findViewById(R.id.switchVoiceParams);
        switchShakeDetection = findViewById(R.id.switchShakeDetection);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        sliderShake = findViewById(R.id.sliderShake);
        sliderVoiceRange = findViewById(R.id.sliderVoiceRange);

        btnEditMedicalSos = findViewById(R.id.btnEditMedicalSos);
        btnEditContactsSos = findViewById(R.id.btnEditContactsSos);
        btnEditKeywordSos = findViewById(R.id.btnEditKeywordSos);

        // SOS Message Edit Listeners
        btnEditMedicalSos
                .setOnClickListener(v -> showEditSosDialog("Medical", "msg_medical", "Need immediate medical help!"));
        btnEditContactsSos.setOnClickListener(
                v -> showEditSosDialog("Contacts", "msg_contacts", "I need help! Please check my location."));
        btnEditKeywordSos.setOnClickListener(
                v -> showEditSosDialog("Keyword", "msg_keywords", "Keyword triggered SOS! I might be in danger."));

        // Load saved state
        boolean isVoiceEnabled = sharedPreferences.getBoolean("voice_detection", true);
        boolean isShakeEnabled = sharedPreferences.getBoolean("shake_detection", true);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        int shakeSens = sharedPreferences.getInt("shake_sensitivity", 50);
        int voiceSens = sharedPreferences.getInt("voice_sensitivity", 70);

        switchVoiceParams.setChecked(isVoiceEnabled);
        if (switchShakeDetection != null) {
            switchShakeDetection.setChecked(isShakeEnabled);
        }
        switchDarkMode.setChecked(isDarkMode);
        sliderShake.setValue(shakeSens);
        if (sliderVoiceRange != null) {
            sliderVoiceRange.setValue(voiceSens);
        }

        switchVoiceParams.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("voice_detection", isChecked);
            editor.apply();
            syncSettingsToFirestore();
        });

        if (switchShakeDetection != null) {
            switchShakeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                editor.putBoolean("shake_detection", isChecked);
                editor.apply();
                syncSettingsToFirestore();
            });
        }

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();
            syncSettingsToFirestore();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        sliderShake.addOnChangeListener((slider, value, fromUser) -> {
            editor.putInt("shake_sensitivity", (int) value);
            editor.apply();
            syncSettingsToFirestore();
        });

        if (sliderVoiceRange != null) {
            sliderVoiceRange.addOnChangeListener((slider, value, fromUser) -> {
                editor.putInt("voice_sensitivity", (int) value);
                editor.apply();
                syncSettingsToFirestore();
            });
        }

    }

    private void showEditSosDialog(String title, String key, String defaultValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + title + " SOS Message");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(40, 20, 40, 20);
        input.setLayoutParams(lp);

        SharedPreferences sosPrefs = getSharedPreferences("SosConfig", MODE_PRIVATE);
        input.setText(sosPrefs.getString(key, defaultValue));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                sosPrefs.edit().putString(key, message).apply();
                Toast.makeText(AppSettingsActivity.this, title + " SOS updated", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void syncSettingsToFirestore() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            java.util.Map<String, Object> settings = new java.util.HashMap<>();
            settings.put("voice_detection", sharedPreferences.getBoolean("voice_detection", true));
            settings.put("shake_detection", sharedPreferences.getBoolean("shake_detection", true));
            settings.put("dark_mode", sharedPreferences.getBoolean("dark_mode", false));
            settings.put("shake_sensitivity", sharedPreferences.getInt("shake_sensitivity", 50));
            settings.put("voice_sensitivity", sharedPreferences.getInt("voice_sensitivity", 70));

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .collection("settings").document("app_config")
                    .set(settings, com.google.firebase.firestore.SetOptions.merge());
        }
    }
}
