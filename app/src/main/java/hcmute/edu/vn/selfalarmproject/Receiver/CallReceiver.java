package hcmute.edu.vn.selfalarmproject.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;

import hcmute.edu.vn.selfalarmproject.model.Call;
import hcmute.edu.vn.selfalarmproject.util.CallLogUtils;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";
    private static String savedNumber;
    private static long callStartTime;
    private static boolean isIncoming;
    private static boolean isAnswered;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (number != null) {
                savedNumber = number;
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // Phone is ringing
                isIncoming = true;
                callStartTime = new Date().getTime();
                isAnswered = false;
                Log.d(TAG, "Incoming call from: " + savedNumber);
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                // Call is answered or outgoing call is made
                if (!isIncoming) {
                    // Outgoing call
                    callStartTime = new Date().getTime();
                    Log.d(TAG, "Outgoing call to: " + savedNumber);
                } else {
                    // Incoming call answered
                    isAnswered = true;
                    Log.d(TAG, "Call answered: " + savedNumber);
                }
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                // Call ended
                if (isIncoming && !isAnswered && savedNumber != null) {
                    // Missed call
                    Log.d(TAG, "Missed call from: " + savedNumber);
                    CallLogUtils.addMissedCall(context, savedNumber);
                } else if (savedNumber != null) {
                    // Call ended (either incoming/answered or outgoing)
                    long duration = (new Date().getTime() - callStartTime) / 1000;
                    Log.d(TAG, "Call ended with: " + savedNumber + ", duration: " + duration + "s");
                }

                // Reset values for next call
                isIncoming = false;
                callStartTime = 0;
                isAnswered = false;
                savedNumber = null;
            }
        }
    }
}