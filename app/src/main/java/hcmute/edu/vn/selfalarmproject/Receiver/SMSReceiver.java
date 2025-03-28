package hcmute.edu.vn.selfalarmproject.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (messages != null && messages.length > 0) {
                    StringBuilder messageBody = new StringBuilder();
                    String senderNumber = messages[0].getDisplayOriginatingAddress();

                    for (SmsMessage message : messages) {
                        messageBody.append(message.getMessageBody());
                    }

                    // Hiển thị thông báo
                    Toast.makeText(context, "SMS từ: " + senderNumber + "\nNội dung: " +
                            messageBody.toString(), Toast.LENGTH_LONG).show();

                    // Gửi broadcast để cập nhật UI hoặc thông báo
                    Intent broadcastIntent = new Intent("SMS_RECEIVED_BROADCAST");
                    broadcastIntent.putExtra("sender", senderNumber);
                    broadcastIntent.putExtra("message", messageBody.toString());
                    context.sendBroadcast(broadcastIntent);
                }
            }
        }
    }
}