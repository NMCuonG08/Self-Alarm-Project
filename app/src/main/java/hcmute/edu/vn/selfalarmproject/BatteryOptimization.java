package hcmute.edu.vn.selfalarmproject;
import hcmute.edu.vn.selfalarmproject.Service.BatteryOptimizationService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class BatteryOptimization extends AppCompatActivity {
    private TextView batteryLevelText;
    private TextView batteryStatusText;
    private TextView batteryTempText;
    private SwitchMaterial autoOptimizeSwitch;
    private SwitchMaterial manageBrightnessSwitch;
    private SwitchMaterial manageWifiSwitch;
    private SwitchMaterial manageSyncSwitch;
    private Slider lowBatteryThresholdSlider;
    private Slider criticalBatteryThresholdSlider;

    private BroadcastReceiver batteryInfoReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optimize_battery);

        // Initialize views
        batteryLevelText = findViewById(R.id.battery_level_text);
        batteryStatusText = findViewById(R.id.battery_status_text);
        batteryTempText = findViewById(R.id.battery_temp_text);
        autoOptimizeSwitch = findViewById(R.id.auto_optimize_switch);
        manageBrightnessSwitch = findViewById(R.id.manage_brightness_switch);
        manageWifiSwitch = findViewById(R.id.manage_wifi_switch);
        manageSyncSwitch = findViewById(R.id.manage_sync_switch);
        lowBatteryThresholdSlider = findViewById(R.id.low_battery_threshold_slider);
        criticalBatteryThresholdSlider = findViewById(R.id.critical_battery_threshold_slider);
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();


            }
        });

        // Load preferences
        loadPreferences();

        // Set up listeners
        setupListeners();

        // Check for system write settings permission for brightness control
        checkSystemWritePermission();

        // Set up battery info receiver
        setupBatteryInfoReceiver();

        // Start the service
        startBatteryOptimizationService();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE);

        // Load switches
        autoOptimizeSwitch.setChecked(prefs.getBoolean("auto_optimize", true));
        manageBrightnessSwitch.setChecked(prefs.getBoolean("manage_brightness", true));
        manageWifiSwitch.setChecked(prefs.getBoolean("manage_wifi", true));
        manageSyncSwitch.setChecked(prefs.getBoolean("manage_sync", true));

        // Load sliders
        lowBatteryThresholdSlider.setValue(prefs.getFloat("low_battery_threshold", 30f));
        criticalBatteryThresholdSlider.setValue(prefs.getFloat("critical_battery_threshold", 15f));
    }

    private void setupListeners() {
        // Switch listeners
        autoOptimizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("auto_optimize", isChecked);
        });

        manageBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("manage_brightness", isChecked);
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    requestSystemWritePermission();
                }
            }
        });

        manageWifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("manage_wifi", isChecked);
        });

        manageSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("manage_sync", isChecked);
        });

        // Slider listeners
        lowBatteryThresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                savePreference("low_battery_threshold", value);

                // Ensure critical threshold is lower than low threshold
                if (value <= criticalBatteryThresholdSlider.getValue()) {
                    criticalBatteryThresholdSlider.setValue(value - 5);
                    savePreference("critical_battery_threshold", value - 5);
                }
            }
        });

        criticalBatteryThresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                savePreference("critical_battery_threshold", value);

                // Ensure low threshold is higher than critical threshold
                if (value >= lowBatteryThresholdSlider.getValue()) {
                    lowBatteryThresholdSlider.setValue(value + 5);
                    savePreference("low_battery_threshold", value + 5);
                }
            }
        });

    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void savePreference(String key, float value) {
        SharedPreferences.Editor editor = getSharedPreferences("BatteryOptPrefs", MODE_PRIVATE).edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    private void checkSystemWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this) && manageBrightnessSwitch.isChecked()) {
                requestSystemWritePermission();
            }
        }
    }

    private void requestSystemWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Please grant permission to modify system settings", Toast.LENGTH_LONG).show();
        }
    }

    private void setupBatteryInfoReceiver() {
        batteryInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryInfo(intent);
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryInfoReceiver, filter);
    }

    private void updateBatteryInfo(Intent intent) {
        if (intent != null) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;

            // Update UI
            batteryLevelText.setText(String.format("%.1f%%", batteryPct));
            batteryStatusText.setText(isCharging ? "Charging" : "Discharging");
            batteryTempText.setText(String.format("%d°C", temperature));

            // Check if the battery level is below the low threshold
            float lowBatteryThreshold = lowBatteryThresholdSlider.getValue();
            if (batteryPct < lowBatteryThreshold) {
                // Show toast to warn user about low battery
                Toast.makeText(this, "Battery level is below the low threshold! Please charge your device.", Toast.LENGTH_LONG).show();
            }

            // Check for critical battery level
            float criticalBatteryThreshold = criticalBatteryThresholdSlider.getValue();
            if (batteryPct < criticalBatteryThreshold) {
                // Show urgent warning toast
                Toast.makeText(this, "Battery level is critically low! Please charge immediately!", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void startBatteryOptimizationService() {
        Intent serviceIntent = new Intent(this, BatteryOptimizationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryInfoReceiver != null) {
            unregisterReceiver(batteryInfoReceiver);
        }
    }
}