package hcmute.edu.vn.selfalarmproject.Receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.selfalarmproject.CallLogActivity;
import hcmute.edu.vn.selfalarmproject.database.DatabaseHelper;

public class MessageCallService extends Service {
    private static final String TAG = "MessageCallService";
    private static final int NOTIFICATION_ID = 1000;
    private static final String CHANNEL_ID = "sms_call_service_channel";
    
    private TelephonyManager telephonyManager;
    private CallStateListener phoneStateListener;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MessageCallService - onCreate called");
        
        // Create notification channel
        createNotificationChannel();
        
        // Start foreground service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            Log.d(TAG, "Service started in foreground");
            
            // Register phone state listener
            setupCallStateListener();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MessageCallService - onStartCommand called");
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "MessageCallService - onDestroy called");
        
        // Unregister phone state listener
        if (telephonyManager != null && phoneStateListener != null) {
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                Log.d(TAG, "Phone state listener unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering phone state listener: " + e.getMessage());
            }
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS and Call Service",
                    NotificationManager.IMPORTANCE_HIGH);
            
            channel.setDescription("Dịch vụ theo dõi tin nhắn và cuộc gọi");
            channel.enableVibration(true);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dịch vụ SMS & Cuộc gọi")
                .setContentText("Đang theo dõi tin nhắn và cuộc gọi")
                .setSmallIcon(android.R.drawable.stat_sys_phone_call)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }
    
    private void setupCallStateListener() {
        try {
            Log.d(TAG, "Setting up call state listener");
            
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            phoneStateListener = new CallStateListener();
            
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                Log.d(TAG, "Phone state listener registered successfully");
            } else {
                Log.e(TAG, "TelephonyManager is null!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up call state listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Custom phone state listener class for better logging and debugging
    private class CallStateListener extends PhoneStateListener {
        private String lastPhoneNumber = "";
        private boolean wasRinging = false;
        
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            Log.d(TAG, "Call state changed: state=" + state + ", number=" + 
                 (phoneNumber != null ? phoneNumber : "unknown"));
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                lastPhoneNumber = phoneNumber;
            }
            
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    wasRinging = true;
                    handleIncomingCall(lastPhoneNumber);
                    break;
                    
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "Call is active (off hook)");
                    break;
                    
                case TelephonyManager.CALL_STATE_IDLE:
                    if (wasRinging) {
                        Log.d(TAG, "Call ended or rejected");
                        wasRinging = false;
                    }
                    break;
            }
        }
    }
    
    private void handleIncomingCall(String phoneNumber) {
        Log.d(TAG, "Handling incoming call: " + phoneNumber);
        
        // Only process if we have a valid phone number
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.w(TAG, "Empty phone number, cannot process call");
            return;
        }
        
        // Save to SQLite instead of Firebase
        saveCallToSQLite(phoneNumber);
    }
    
    private void saveCallToSQLite(String phoneNumber) {
        try {
            Log.d(TAG, "Saving call to SQLite: " + phoneNumber);
            
            // Get database helper
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            
            // Save call with current timestamp
            long timestamp = System.currentTimeMillis();
            long id = dbHelper.addCall(phoneNumber, timestamp, "incoming");
            
            if (id > 0) {
                Log.d(TAG, "Call saved to SQLite successfully with ID: " + id);
            } else {
                Log.e(TAG, "Failed to save call to SQLite");
            }
            
            // Always show notification regardless of save success
            showCallNotification(phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Exception saving call to SQLite: " + e.getMessage());
            e.printStackTrace();
            
            // Show notification anyway
            showCallNotification(phoneNumber);
        }
    }
    
    private void showCallNotification(String phoneNumber) {
        try {
            Log.d(TAG, "Showing call notification for: " + phoneNumber);
            
            // Create intent to open call log activity
            Intent intent = new Intent(this, CallLogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            // Get default sound
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Cuộc gọi đến")
                    .setContentText("Từ số: " + phoneNumber)
                    .setSmallIcon(android.R.drawable.stat_sys_phone_call)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(pendingIntent)
                    .setSound(sound)
                    .setVibrate(new long[]{0, 500, 200, 500})
                    .setAutoCancel(true);
            
            // Show notification
            int notificationId = (int) System.currentTimeMillis();
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Call notification displayed with ID: " + notificationId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing call notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}