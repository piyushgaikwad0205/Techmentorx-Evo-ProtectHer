package com.example.sos;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "app_theme";

    // Theme modes
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    private SharedPreferences prefs;

    public ThemeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setTheme(int themeMode) {
        prefs.edit().putInt(KEY_THEME, themeMode).apply();
        applyTheme(themeMode);
    }

    public int getTheme() {
        return prefs.getInt(KEY_THEME, THEME_LIGHT); // Default to Light theme
    }

    public void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public void initTheme() {
        applyTheme(getTheme());
    }

    // Helper method to check if dark mode is enabled
    public boolean isDarkMode() {
        return getTheme() == THEME_DARK;
    }

    // Helper method to set dark mode with boolean
    public void setDarkMode(boolean isDark) {
        setTheme(isDark ? THEME_DARK : THEME_LIGHT);
    }
}
