package hcmute.edu.vn.selfalarmproject.Receiver;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MessageCallService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "message_call_service_channel";
    private static final String TAG = "MessageCallService";

    private BroadcastReceiver smsReceiver;
    private BroadcastReceiver callReceiver;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isReceiverRegistered = false;
    
    // Firebase references
    private DatabaseReference smsReference;
    private DatabaseReference callReference;

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo Firebase Database
        initFirebase();
        
        // Tạo notification channel cho foreground service
        createNotificationChannel();

        // Bắt đầu foreground service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            
            // Đăng ký receivers
            registerReceivers();
            
            // Đăng ký lắng nghe trạng thái cuộc gọi
            setupCallStateListener();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Khởi tạo Firebase
    private void initFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            smsReference = database.getReference("messages");
            callReference = database.getReference("calls");
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
        
        // Hủy đăng ký lắng nghe cuộc gọi
        if (telephonyManager != null && phoneStateListener != null) {
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering phone listener: " + e.getMessage());
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Message and Call Service",
                        NotificationManager.IMPORTANCE_HIGH); // Changed to HIGH for better notification visibility
                
                channel.setDescription("Handles SMS and call notifications");
                channel.enableVibration(true);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS & Call Manager")
                .setContentText("Đang chạy để xử lý tin nhắn và cuộc gọi")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }

    // Lắng nghe trạng thái cuộc gọi
    private void setupCallStateListener() {
        try {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            
            phoneStateListener = new PhoneStateListener() {
                private String incomingNumber = "";
                
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        incomingNumber = phoneNumber;
                    }
                    
                    switch (state) {
                        case TelephonyManager.CALL_STATE_RINGING:
                            // Cuộc gọi đến
                            if (!incomingNumber.isEmpty()) {
                                handleIncomingCall(incomingNumber);
                            }
                            break;
                    }
                }
            };
            
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                Log.d(TAG, "Call state listener registered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up call state listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceivers() {
        try {
            // Đăng ký receiver cho SMS thực tế
            smsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                            Bundle bundle = intent.getExtras();
                            if (bundle != null) {
                                // Lấy tin nhắn SMS từ intent
                                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                                
                                if (messages != null && messages.length > 0) {
                                    String sender = messages[0].getOriginatingAddress();
                                    StringBuilder messageBody = new StringBuilder();
                                    
                                    // Kết hợp các phần của tin nhắn nếu cần
                                    for (SmsMessage sms : messages) {
                                        messageBody.append(sms.getMessageBody());
                                    }
                                    
                                    // Xử lý tin nhắn SMS
                                    handleIncomingSMS(sender, messageBody.toString());
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing received SMS: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
            
            // Đăng ký nhận SMS thực tế
            IntentFilter smsFilter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            smsFilter.setPriority(999); // Độ ưu tiên cao
            registerReceiver(smsReceiver, smsFilter);
            
            // Vẫn giữ lại receiver cũ cho tin nhắn tùy chỉnh nếu cần
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String sender = intent.getStringExtra("sender");
                    String message = intent.getStringExtra("message");

                    if (sender != null && message != null) {
                        handleIncomingSMS(sender, message);
                    }
                }
            }, new IntentFilter("SMS_RECEIVED_BROADCAST"));
            
            isReceiverRegistered = true;
            Log.d(TAG, "SMS receivers registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receivers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void unregisterReceivers() {
        try {
            if (isReceiverRegistered) {
                if (smsReceiver != null) {
                    unregisterReceiver(smsReceiver);
                }

                if (callReceiver != null) {
                    unregisterReceiver(callReceiver);
                }
                
                isReceiverRegistered = false;
                Log.d(TAG, "Receivers unregistered successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receivers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lưu tin nhắn vào Firebase
    private void saveSMSToFirebase(String sender, String message) {
        try {
            String messageId = smsReference.push().getKey();
            if (messageId != null) {
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("sender", sender);
                messageData.put("message", message);
                messageData.put("timestamp", System.currentTimeMillis());
                
                smsReference.child(messageId).setValue(messageData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "SMS saved to Firebase successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving SMS to Firebase: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception saving SMS to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Lưu cuộc gọi vào Firebase
    private void saveCallToFirebase(String phoneNumber) {
        try {
            String callId = callReference.push().getKey();
            if (callId != null) {
                Map<String, Object> callData = new HashMap<>();
                callData.put("phoneNumber", phoneNumber);
                callData.put("timestamp", System.currentTimeMillis());
                callData.put("type", "incoming");
                
                callReference.child(callId).setValue(callData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Call saved to Firebase successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving call to Firebase: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception saving call to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleIncomingSMS(String sender, String message) {
        try {
            Log.d(TAG, "Received SMS from: " + sender);
            
            // Hiển thị notification với sound và vibration
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Tin nhắn mới từ: " + sender)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Dùng timestamp làm notification ID để tránh ghi đè
                notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
            }
            
            // Lưu tin nhắn vào Firebase
            saveSMSToFirebase(sender, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleIncomingCall(String phoneNumber) {
        try {
            Log.d(TAG, "Received incoming call from: " + phoneNumber);
            
            // Hiển thị notification với sound và vibration
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Cuộc gọi đến")
                    .setContentText("Từ số: " + phoneNumber)
                    .setSmallIcon(android.R.drawable.ic_dialog_dialer)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Dùng timestamp làm notification ID để tránh ghi đè
                notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
            }
            
            // Lưu cuộc gọi vào Firebase
            saveCallToFirebase(phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming call: " + e.getMessage());
            e.printStackTrace();
        }
    }
}