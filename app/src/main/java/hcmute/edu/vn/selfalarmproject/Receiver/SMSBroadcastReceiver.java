package hcmute.edu.vn.selfalarmproject.Receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.selfalarmproject.R;
import hcmute.edu.vn.selfalarmproject.SMSListActivity;
import hcmute.edu.vn.selfalarmproject.database.DatabaseHelper;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSBroadcastReceiver";
    private static final String CHANNEL_ID = "sms_notification_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS broadcast received: " + intent.getAction());
        
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Get SMS messages from intent
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                
                if (messages != null && messages.length > 0) {
                    String sender = messages[0].getOriginatingAddress();
                    StringBuilder messageBody = new StringBuilder();
                    
                    // Combine multi-part messages if needed
                    for (SmsMessage sms : messages) {
                        messageBody.append(sms.getMessageBody());
                    }
                    
                    String fullMessage = messageBody.toString();
                    Log.d(TAG, "SMS from: " + sender + ", message: " + fullMessage);
                    
                    // Save SMS to SQLite database
                    saveIncomingSMSToDatabase(context, sender, fullMessage);
                    
                    // Show prominent notification immediately
                    showProminentNotification(context, sender, fullMessage);
                    
                    // Start the service for additional processing if needed
                    Intent serviceIntent = new Intent(context, MessageCallService.class);
                    context.startService(serviceIntent);
                }
            }
        }
    }
    
    private void saveIncomingSMSToDatabase(Context context, String sender, String message) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            long timestamp = System.currentTimeMillis();
            
            // Type 1 means incoming message
            long id = dbHelper.addSMS(sender, null, message, timestamp, 1);
            
            if (id > 0) {
                Log.d(TAG, "SMS saved to database successfully with ID: " + id);
            } else {
                Log.e(TAG, "Failed to save SMS to database");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception saving SMS to database: " + e.getMessage());
        }
    }
    
    private void showProminentNotification(Context context, String sender, String message) {
        try {
            // Create notification channel for Android O and above
            createNotificationChannel(context);
            
            // Set up notification intent
            Intent intent = new Intent(context, SMSListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            // Get default notification sound
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            
            // Create notification with high prominence
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle("Tin nhắn từ: " + sender)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(new long[]{0, 500, 200, 500}) // Strong vibration pattern
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for Heads-up
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show content on lock screen
                    .setContentIntent(pendingIntent);
                    
            // Use big text style for long messages
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(message);
            notificationBuilder.setStyle(bigTextStyle);
            
            // Create unique ID for each notification
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            
            // Get notification manager and show the notification
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(notificationId, notificationBuilder.build());
                Log.d(TAG, "Prominent notification shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager notificationManager = 
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "SMS Notifications",
                            NotificationManager.IMPORTANCE_HIGH); // Use HIGH importance
                    
                    // Configure the notification channel
                    channel.setDescription("Hiển thị thông báo khi có tin nhắn SMS đến");
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    channel.setShowBadge(true);
                    
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }
}