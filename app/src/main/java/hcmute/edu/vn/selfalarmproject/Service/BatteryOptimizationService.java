package hcmute.edu.vn.selfalarmproject.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import hcmute.edu.vn.selfalarmproject.R;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.selfalarmproject.BatteryOptimization;

public class BatteryOptimizationService extends Service {
    private static final String TAG = "BatteryOptimizationService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "battery_optimization_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            float batteryLevel = intent.getFloatExtra("BATTERY_LEVEL", 100);
            boolean isCharging = intent.getBooleanExtra("IS_CHARGING", false);
            int temperature = intent.getIntExtra("TEMPERATURE", 0);

            // Start as a foreground service
            startForeground(NOTIFICATION_ID, createNotification(batteryLevel, isCharging));

            // Apply optimization based on battery level
            optimizeBattery(batteryLevel, isCharging, temperature);
        }

        return START_STICKY;
    }

    private void optimizeBattery(float batteryLevel, boolean isCharging, int temperature) {
        SharedPreferences prefs = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE);
        boolean autoOptimizeEnabled = prefs.getBoolean("auto_optimize", true);

        if (!autoOptimizeEnabled) {
            Log.d(TAG, "Auto optimization disabled by user");
            return;
        }

        float lowThreshold = prefs.getFloat("low_battery_threshold", 30f);
        float criticalThreshold = prefs.getFloat("critical_battery_threshold", 15f);

        if (!isCharging) {
            if (batteryLevel < criticalThreshold) {
                // Critical battery - aggressive optimization
                applyAggressiveOptimization();
            } else if (batteryLevel < lowThreshold) {
                // Low battery - medium optimization
                applyMediumOptimization();
            } else {
                // Normal battery - light optimization
                applyLightOptimization();
            }

            // Handle high temperature regardless of battery level
            if (temperature > 40) {
                handleHighTemperature();
            }
        } else {
            // Device is charging - restore normal settings if battery is above threshold
            if (batteryLevel > lowThreshold) {
                restoreNormalSettings();
            }
        }
    }

    private void applyAggressiveOptimization() {
        Log.d(TAG, "Applying aggressive battery optimization");

        // Reduce screen brightness
        adjustBrightness(0.2f);

        // Disable Wi-Fi if not connected
        manageWifi(false);

        // Disable auto-sync
        toggleAutoSync(false);

        // Update notification
        updateNotification("Aggressive battery saving mode active");
    }

    private void applyMediumOptimization() {
        Log.d(TAG, "Applying medium battery optimization");

        // Reduce screen brightness moderately
        adjustBrightness(0.5f);

        // Keep Wi-Fi on if connected
        if (!isWifiConnected()) {
            manageWifi(false);
        }

        // Update notification
        updateNotification("Medium battery saving mode active");
    }

    private void applyLightOptimization() {
        Log.d(TAG, "Applying light battery optimization");

        // Normal brightness
        adjustBrightness(0.7f);

        // Keep connectivity normal

        // Update notification
        updateNotification("Light battery saving mode active");
    }

    private void handleHighTemperature() {
        Log.d(TAG, "Managing high battery temperature");

        // Reduce screen brightness
        adjustBrightness(0.3f);

        // Disable intensive features
        toggleAutoSync(false);

        // Update notification with warning
        updateNotification("High battery temperature detected! Applying cooling measures");
    }

    private void restoreNormalSettings() {
        Log.d(TAG, "Restoring normal settings");

        // Restore screen brightness
        adjustBrightness(1.0f);

        // Enable Wi-Fi
        manageWifi(true);

        // Enable auto-sync
        toggleAutoSync(true);

        // Update notification
        updateNotification("Normal power mode");
    }

    private void adjustBrightness(float brightness) {
        SharedPreferences prefs = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE);
        boolean manageBrightness = prefs.getBoolean("manage_brightness", true);

        if (!manageBrightness) {
            return;
        }

        try {
            // Check if we have WRITE_SETTINGS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Log.e(TAG, "No permission to write system settings");
                    return;
                }
            }

            // Set to manual brightness mode
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            // Set brightness level (0-255)
            int brightnessValue = (int) (brightness * 255);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, brightnessValue);

            Log.d(TAG, "Brightness adjusted to: " + brightnessValue);
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting brightness: " + e.getMessage());
        }
    }

    private void manageWifi(boolean enable) {
        SharedPreferences prefs = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE);
        boolean manageWifi = prefs.getBoolean("manage_wifi", true);

        if (!manageWifi) {
            return;
        }

        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null) {
                if (enable && !wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                    Log.d(TAG, "Wi-Fi enabled");
                } else if (!enable && wifiManager.isWifiEnabled() && !isWifiConnected()) {
                    wifiManager.setWifiEnabled(false);
                    Log.d(TAG, "Wi-Fi disabled");
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to change Wi-Fi state: " + e.getMessage());
        }
    }

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo != null && wifiInfo.isConnected();
        }
        return false;
    }

    private void toggleAutoSync(boolean enable) {
        SharedPreferences prefs = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE);
        boolean manageSync = prefs.getBoolean("manage_sync", true);

        if (!manageSync) {
            return;
        }

        try {
            ContentResolver.setMasterSyncAutomatically(enable);
            Log.d(TAG, "Auto-sync " + (enable ? "enabled" : "disabled"));
        } catch (Exception e) {
            Log.e(TAG, "Error toggling auto-sync: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Optimization",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows the current battery optimization status");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(float batteryLevel, boolean isCharging) {
        Intent notificationIntent = new Intent(this, BatteryOptimization.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String batteryStatus = String.format("Battery: %.1f%% - %s",
                batteryLevel, isCharging ? "Charging" : "Discharging","Full");

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Optimization")
                .setContentText(batteryStatus)
                .setSmallIcon(R.drawable.ic_battery)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            Intent notificationIntent = new Intent(this, BatteryOptimization.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Battery Optimization")
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_battery)
                    .setContentIntent(pendingIntent)
                    .build();

            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}