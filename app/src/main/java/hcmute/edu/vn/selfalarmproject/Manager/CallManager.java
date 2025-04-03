package hcmute.edu.vn.selfalarmproject.Manager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.model.CallLogData;

public class CallManager {

    // Thực hiện cuộc gọi
    public static void makeCall(Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        context.startActivity(intent);
    }

    // Đọc nhật ký cuộc gọi
    public static List<CallLogData> getCallLogs(Context context) {
        List<CallLogData> callLogs = new ArrayList<>();

        String[] projection = new String[] {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CallLogData callLog = new CallLogData();
                    callLog.setId((int) cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID)));
                    callLog.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)));
                    callLog.setType(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE))));
                    callLog.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)));


                    callLogs.add(callLog);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return callLogs;
    }
}