package com.example.sos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ServiceRestartReceiver", "Service restart triggered by tamper protection");

        // Double check shadow mode logic here too if needed, but the sender checks it
        Intent serviceIntent = new Intent(context, ServiceMine.class);
        serviceIntent.setAction("Start");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
