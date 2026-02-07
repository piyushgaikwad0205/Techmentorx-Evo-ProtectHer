package com.example.sos;

import android.content.Context;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HeartRateMonitor {
    private Context context;
    private boolean isMonitoring = false;
    private Handler monitoringHandler = new Handler();
    private List<Integer> heartRateHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    private TextView tvHeartRate;
    private TextView tvMonitoringStatus;
    private ImageView imgHeartPulse;
    private LinearLayout layoutHeartRateHistory;

    public HeartRateMonitor(Context context, View rootView) {
        this.context = context;
        if (rootView != null) {
            tvHeartRate = rootView.findViewById(R.id.tvHeartRate);
            tvMonitoringStatus = rootView.findViewById(R.id.tvMonitoringStatus);
            imgHeartPulse = rootView.findViewById(R.id.imgHeartPulse);
            layoutHeartRateHistory = rootView.findViewById(R.id.layoutHeartRateHistory);
        }

        // Auto-set status to waiting
        if (tvMonitoringStatus != null) {
            tvMonitoringStatus.setText("Waiting for device...");
            tvMonitoringStatus.setBackgroundResource(R.drawable.rounded_bg_gray);
            // We'll trust the XML implementation or update as needed, but keeping it simple
            // for now
        }
    }

    public void start() {
        isMonitoring = true;
        if (tvMonitoringStatus != null) {
            tvMonitoringStatus.setText("Live Monitoring");
            tvMonitoringStatus.setTextColor(0xFF4CAF50); // Green
            tvMonitoringStatus.setBackgroundResource(R.drawable.rounded_bg_green);
        }
    }

    public void stop() {
        isMonitoring = false;
        if (tvMonitoringStatus != null) {
            tvMonitoringStatus.setText("Paused");
            tvMonitoringStatus.setTextColor(0xFFFFA000); // Amber
            tvMonitoringStatus.setBackgroundResource(R.drawable.rounded_bg_gray);
        }
        monitoringHandler.removeCallbacksAndMessages(null);
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public void updateHeartRate(int bpm) {
        if (tvHeartRate != null) {
            tvHeartRate.setText(String.valueOf(bpm));
        }
        addToHistory(bpm);
        updateBarGraph();
        animateHeartIcon();
    }

    private void animateHeartIcon() {
        if (imgHeartPulse != null) {
            imgHeartPulse.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200)
                    .withEndAction(() -> imgHeartPulse.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start())
                    .start();
        }
    }

    private void addToHistory(int bpm) {
        heartRateHistory.add(bpm);
        if (heartRateHistory.size() > MAX_HISTORY) {
            heartRateHistory.remove(0);
        }
    }

    private void updateBarGraph() {
        if (layoutHeartRateHistory == null)
            return;

        layoutHeartRateHistory.removeAllViews();
        if (heartRateHistory.isEmpty())
            return;

        int maxBPM = Math.max(Collections.max(heartRateHistory), 100);
        int minBPM = 40;

        for (int bpm : heartRateHistory) {
            View bar = new View(context);
            // Calculate height in dp, max height ~60dp
            float heightPercent = (float) (bpm - minBPM) / (maxBPM - minBPM);
            heightPercent = Math.max(0.1f, Math.min(1.0f, heightPercent));

            int heightDp = (int) (heightPercent * 60);
            int heightPx = (int) (heightDp * context.getResources().getDisplayMetrics().density);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    heightPx,
                    1);
            params.setMargins(4, 0, 4, 0);
            params.gravity = android.view.Gravity.BOTTOM;

            bar.setLayoutParams(params);
            bar.setBackgroundColor(0xFFE91E63); // Pink color

            // Rounded corners for bars
            // bar.setBackgroundResource(R.drawable.rounded_bar); // If we had a drawable

            layoutHeartRateHistory.addView(bar);
        }
    }

    public Handler getHandler() {
        return monitoringHandler;
    }

    public void cleanup() {
        stop();
    }
}
