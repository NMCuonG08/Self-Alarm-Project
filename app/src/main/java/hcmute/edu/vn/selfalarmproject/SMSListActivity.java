package hcmute.edu.vn.selfalarmproject;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.model.SMSData;

public class SMSListActivity extends AppCompatActivity {
    
    private static final String TAG = "SMSListActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_list);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference smsReference = database.getReference("messages");
        
        ListView listView = findViewById(R.id.sms_list_view);
        
        // Load SMS from Firebase
        smsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Container for SMS data
                List<SMSData> smsList = new ArrayList<>();
                
                // Process each SMS message from Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        String sender = snapshot.child("sender").getValue(String.class);
                        String message = snapshot.child("message").getValue(String.class);
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        
                        if (sender != null && message != null && timestamp != null) {
                            // Create SMSData object with Firebase data
                            SMSData smsData = new SMSData();
                            smsData.setAddress(sender);
                            smsData.setBody(message);
                            smsData.setDate(timestamp);
                            smsData.setType(1); // Assuming all Firebase SMS are incoming (1)
                            
                            smsList.add(smsData);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS data: " + e.getMessage());
                    }
                }
                
                // Same display logic as before
                List<Map<String, String>> data = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                for (SMSData sms : smsList) {
                    Map<String, String> item = new HashMap<>();
                    item.put("address", sms.getAddress());
                    item.put("body", sms.getBody());
                    item.put("date", dateFormat.format(new Date(sms.getDate())));

                    String type;
                    switch (sms.getType()) {
                        case 1:
                            type = "Nhận vào";
                            break;
                        case 2:
                            type = "Đã gửi";
                            break;
                        case 3:
                            type = "Bản nháp";
                            break;
                        default:
                            type = "Không xác định";
                    }
                    item.put("type", type);

                    data.add(item);
                }

                SimpleAdapter adapter = new SimpleAdapter(
                        SMSListActivity.this,
                        data,
                        R.layout.sms_list_item,
                        new String[]{"address", "body", "date", "type"},
                        new int[]{R.id.sms_address, R.id.sms_body, R.id.sms_date, R.id.sms_type}
                );

                listView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase Database error: " + databaseError.getMessage());
                Toast.makeText(SMSListActivity.this, "Không thể tải dữ liệu tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }
}