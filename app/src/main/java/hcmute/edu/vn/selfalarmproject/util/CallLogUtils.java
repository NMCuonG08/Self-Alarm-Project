package hcmute.edu.vn.selfalarmproject.util;

import android.content.ContentValues;
import android.content.Context;
import android.provider.CallLog;
import android.util.Log;

import java.util.Date;

public class CallLogUtils {

    private static final String TAG = "CallLogUtils";

    public static void addMissedCall(Context context, String phoneNumber) {
        // Note: This is for demonstration only. In a real app, you cannot directly
        // write to the call log without proper permissions, and on newer Android versions,
        // this is even more restricted. This shows the concept.
        try {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, phoneNumber);
            values.put(CallLog.Calls.DATE, new Date().getTime());
            values.put(CallLog.Calls.DURATION, 0);
            values.put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE);
            values.put(CallLog.Calls.NEW, 1);
            
            context.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
            Log.d(TAG, "Added missed call to log: " + phoneNumber);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied to write call log", e);
        }
    }
}