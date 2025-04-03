package hcmute.edu.vn.selfalarmproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.Receiver.MessageCallService;
import hcmute.edu.vn.selfalarmproject.database.DatabaseHelper;

public class SMSCallActivity extends AppCompatActivity {
    private static final String TAG = "SMSCallActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private ImageView statusIcon;
    private TextView statusText;
    private Button toggleServiceBtn;
    private boolean serviceRunning = false;
    private EditText phoneNumberEt, messageEt;
    private Button sendBtn, viewSmsBtn, viewCallLogBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_call_activity);

        // Initialize UI components
        phoneNumberEt = findViewById(R.id.phone_number_et);
        messageEt = findViewById(R.id.message_et);
        sendBtn = findViewById(R.id.send_btn);
        viewSmsBtn = findViewById(R.id.view_sms_btn);
        viewCallLogBtn = findViewById(R.id.view_call_log_btn);

        // Check permissions and start service
        if (checkPermissions()) {
            startMonitoringService();
        }

        // Set up SMS button
        sendBtn.setOnClickListener(v -> {
            String phoneNumber = phoneNumberEt.getText().toString().trim();
            String message = messageEt.getText().toString().trim();

            if (!phoneNumber.isEmpty() && !message.isEmpty()) {
                sendSMS(phoneNumber, message);
            } else {
                Toast.makeText(SMSCallActivity.this, 
                    "Vui lòng nhập số điện thoại và tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });

        // View SMS list
        viewSmsBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SMSCallActivity.this, SMSListActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Error opening SMS list: " + e.getMessage());
                Toast.makeText(SMSCallActivity.this, 
                    "Không thể mở danh sách SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // View call log
        viewCallLogBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SMSCallActivity.this, CallLogActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Error opening call log: " + e.getMessage());
                Toast.makeText(SMSCallActivity.this, 
                    "Không thể mở lịch sử cuộc gọi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        // Find status views
        statusIcon = findViewById(R.id.status_icon);
        statusText = findViewById(R.id.status_text);
        toggleServiceBtn = findViewById(R.id.toggle_service_btn);

        // Add toggle service button functionality
        toggleServiceBtn.setOnClickListener(v -> {
            if (serviceRunning) {
                // Stop service
                try {
                    stopService(new Intent(this, MessageCallService.class));
                    updateServiceStatus(false);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping service: " + e.getMessage());
                }
            } else {
                // Start service
                if (checkPermissions()) {
                    startMonitoringService();
                    updateServiceStatus(true);
                }
            }
        });

        // Set initial status based on if service is running
        updateServiceStatus(true);
    }

    private void updateServiceStatus(boolean isRunning) {
        serviceRunning = isRunning;
        if (isRunning) {
            statusIcon.setImageResource(android.R.drawable.presence_online);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
            }
            statusText.setText("Đang giám sát tin nhắn và cuộc gọi");
            toggleServiceBtn.setText("Dừng giám sát");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toggleServiceBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_light)));
            }
        } else {
            statusIcon.setImageResource(android.R.drawable.presence_offline);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
            }
            statusText.setText("Chưa giám sát tin nhắn và cuộc gọi");
            toggleServiceBtn.setText("Bắt đầu giám sát");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toggleServiceBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light)));
            }
        }
    }

    private boolean checkPermissions() {
        // List all required permissions
        String[] permissions = {
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
        };

        // Add foreground service permission for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions = new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.FOREGROUND_SERVICE
            };
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            List<String> permissionList = new ArrayList<>(Arrays.asList(permissions));
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
            permissions = permissionList.toArray(new String[0]);
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                Log.d(TAG, "Permission not granted: " + permission);
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            Log.d(TAG, "Requesting permissions");
        } else {
            Log.d(TAG, "All permissions already granted");
        }
        
        return allPermissionsGranted;
    }
    
    private void startMonitoringService() {
        try {
            Intent serviceIntent = new Intent(this, MessageCallService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Started monitoring service");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage());
        }
    }
    
    private void sendSMS(String phoneNumber, String message) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                    == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                
                Toast.makeText(this, "Đã gửi SMS thành công", Toast.LENGTH_SHORT).show();
                
                // Save outgoing SMS to SQLite
                saveOutgoingSMSToDatabase(phoneNumber, message);
                
            } else {
                Toast.makeText(this, "Cần cấp quyền gửi SMS", Toast.LENGTH_SHORT).show();
                checkPermissions();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage());
            Toast.makeText(this, "Gửi SMS thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOutgoingSMSToDatabase(String phoneNumber, String message) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            long timestamp = System.currentTimeMillis();
            
            // Type 2 means outgoing message
            long id = dbHelper.addSMS("Me", phoneNumber, message, timestamp, 2);
            
            if (id > 0) {
                Log.d(TAG, "Outgoing SMS saved to database with ID: " + id);
            } else {
                Log.e(TAG, "Failed to save outgoing SMS to database");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving outgoing SMS to database: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startMonitoringService();
            } else {
                Toast.makeText(this, "Cần cấp quyền để ứng dụng hoạt động đúng", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

