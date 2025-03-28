package hcmute.edu.vn.selfalarmproject.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (state != null) {
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Đang có cuộc gọi đến
                    Toast.makeText(context, "Cuộc gọi đến từ: " + incomingNumber, Toast.LENGTH_LONG).show();

                    // Gửi broadcast để app có thể phản hồi
                    Intent broadcastIntent = new Intent("INCOMING_CALL_BROADCAST");
                    broadcastIntent.putExtra("phone_number", incomingNumber);
                    context.sendBroadcast(broadcastIntent);
                } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    // Cuộc gọi đã được trả lời hoặc cuộc gọi đi đang diễn ra
                    Toast.makeText(context, "Cuộc gọi đang diễn ra", Toast.LENGTH_SHORT).show();
                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    // Không có cuộc gọi nào đang diễn ra
                    Toast.makeText(context, "Cuộc gọi đã kết thúc", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}