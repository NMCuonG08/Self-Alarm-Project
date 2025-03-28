package hcmute.edu.vn.selfalarmproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.Receiver.MessageCallService;

public class SMSCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private EditText phoneNumberEt, messageEt;
    private Button sendBtn, viewSmsBtn, viewCallLogBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_call_activity);

        // Khởi tạo các thành phần UI
        phoneNumberEt = findViewById(R.id.phone_number_et);
        messageEt = findViewById(R.id.message_et);
        sendBtn = findViewById(R.id.send_btn);
        viewSmsBtn = findViewById(R.id.view_sms_btn);
        viewCallLogBtn = findViewById(R.id.view_call_log_btn);

        // Kiểm tra quyền
        if (checkPermissions()) {
            // Khởi động service chỉ khi đã có quyền
            startService(new Intent(this, MessageCallService.class));
        }

        // Thiết lập sự kiện nút gửi SMS
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = phoneNumberEt.getText().toString().trim();
                String message = messageEt.getText().toString().trim();

                if (!phoneNumber.isEmpty() && !message.isEmpty()) {
                    sendSMS(phoneNumber, message);
                } else {
                    Toast.makeText(SMSCallActivity.this, "Vui lòng nhập số điện thoại và tin nhắn", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Thiết lập sự kiện nút xem SMS
        viewSmsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(SMSCallActivity.this, SMSListActivity.class));
                } catch (Exception e) {
                    Toast.makeText(SMSCallActivity.this, "Không thể mở SMSListActivity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Thiết lập sự kiện nút xem lịch sử cuộc gọi
        viewCallLogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(SMSCallActivity.this, CallLogActivity.class));
                } catch (Exception e) {
                    Toast.makeText(SMSCallActivity.this, "Không thể mở CallLogActivity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Kiểm tra và yêu cầu quyền
    private boolean checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
        
        return allPermissionsGranted;
    }
    
    // Updated method to include Firebase saving
    private void sendSMS(String phoneNumber, String message) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                    == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                
                // Show success toast
                Toast.makeText(this, "Đã gửi SMS thành công", Toast.LENGTH_SHORT).show();
                
                // Save to Firebase as outgoing message
                saveOutgoingSMSToFirebase(phoneNumber, message);
                
            } else {
                Toast.makeText(this, "Cần cấp quyền gửi SMS", Toast.LENGTH_SHORT).show();
                checkPermissions();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gửi SMS thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // New method to save outgoing SMS to Firebase
    private void saveOutgoingSMSToFirebase(String phoneNumber, String message) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference smsReference = database.getReference("messages");
            
            String messageId = smsReference.push().getKey();
            if (messageId != null) {
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("sender", "Me"); // Or you can use "Outgoing" 
                messageData.put("recipient", phoneNumber);
                messageData.put("message", message);
                messageData.put("timestamp", System.currentTimeMillis());
                messageData.put("type", 2); // 2 for outgoing, matching your SMSData.type
                
                smsReference.child(messageId).setValue(messageData)
                    .addOnSuccessListener(aVoid -> Log.d("SMSCallActivity", "Outgoing SMS saved to Firebase"))
                    .addOnFailureListener(e -> Log.e("SMSCallActivity", "Failed to save outgoing SMS: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e("SMSCallActivity", "Error saving outgoing SMS: " + e.getMessage());
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
                // Khởi động service sau khi được cấp quyền
                startService(new Intent(this, MessageCallService.class));
            } else {
                Toast.makeText(this, "Cần cấp quyền để ứng dụng hoạt động đúng", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

