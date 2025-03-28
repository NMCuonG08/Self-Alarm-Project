package hcmute.edu.vn.selfalarmproject.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                // Get SMS messages from intent
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                
                if (messages != null && messages.length > 0) {
                    String sender = messages[0].getOriginatingAddress();
                    StringBuilder messageBody = new StringBuilder();
                    
                    // Combine multi-part messages if needed
                    for (SmsMessage sms : messages) {
                        messageBody.append(sms.getMessageBody());
                    }
                    
                    Log.d(TAG, "SMS from: " + sender + ", message: " + messageBody.toString());
                    
                    // Start service to handle the SMS if it's not already running
                    Intent serviceIntent = new Intent(context, MessageCallService.class);
                    context.startService(serviceIntent);
                    
                    // Forward the SMS to our service
                    Intent forwardIntent = new Intent("SMS_RECEIVED_BROADCAST");
                    forwardIntent.putExtra("sender", sender);
                    forwardIntent.putExtra("message", messageBody.toString());
                    context.sendBroadcast(forwardIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving SMS: " + e.getMessage());
        }
    }
}