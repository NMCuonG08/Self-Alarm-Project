package hcmute.edu.vn.selfalarmproject.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            // Get battery level
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;

            // Get charging state
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // Get temperature
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10; // in Celsius

            Log.d(TAG, "Battery Level: " + batteryPct + "%, Charging: " + isCharging + ", Temp: " + temperature + "°C");

            // Start the battery optimization service
            Intent serviceIntent = new Intent(context, BatteryOptimizationService.class);
            serviceIntent.putExtra("BATTERY_LEVEL", batteryPct);
            serviceIntent.putExtra("IS_CHARGING", isCharging);
            serviceIntent.putExtra("TEMPERATURE", temperature);
            context.startService(serviceIntent);
            // Load the threshold values for low and critical battery levels
            SharedPreferences prefs = context.getSharedPreferences("BatteryOptPrefs", Context.MODE_PRIVATE);
            float lowBatteryThreshold = prefs.getFloat("low_battery_threshold", 30f); // Default 30%
            float criticalBatteryThreshold = prefs.getFloat("critical_battery_threshold", 15f); // Default 15%

            // Check if battery level is below the low battery threshold
            if (batteryPct < lowBatteryThreshold) {
                Toast.makeText(context, "Battery level is below the low threshold! Please charge your device.", Toast.LENGTH_LONG).show();
            }

            // Check if battery level is below the critical battery threshold
            if (batteryPct < criticalBatteryThreshold) {
                Toast.makeText(context, "Battery level is critically low! Please charge immediately!", Toast.LENGTH_LONG).show();
            }


        }
    }

}