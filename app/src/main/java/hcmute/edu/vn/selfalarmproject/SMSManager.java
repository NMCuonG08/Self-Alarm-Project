package hcmute.edu.vn.selfalarmproject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.model.SMSData;

public class SMSManager {

    // Gửi SMS
    public static void sendSMS(Context context, String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            // Intent cho việc gửi SMS thành công
            String SENT = "SMS_SENT";
            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT),
                    PendingIntent.FLAG_IMMUTABLE);

            // Đăng ký broadcast receiver
            ContextCompat.registerReceiver(context, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(context, "SMS đã gửi thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Gửi SMS thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }, new IntentFilter(SENT), ContextCompat.RECEIVER_NOT_EXPORTED);

            // Nếu tin nhắn dài, chia thành nhiều phần
            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentPI);
            }

            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null);
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi gửi SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Đọc tất cả SMS
    public static List<SMSData> getAllSMS(Context context) {
        List<SMSData> smsList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms");

        String[] projection = new String[] {
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };

        try (Cursor cursor = contentResolver.query(uri, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SMSData smsData = new SMSData();
                    smsData.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)));
                    smsData.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)));
                    smsData.setBody(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                    smsData.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                    smsData.setType(cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)));

                    smsList.add(smsData);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return smsList;
    }
}
