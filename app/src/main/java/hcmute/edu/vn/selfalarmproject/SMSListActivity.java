package hcmute.edu.vn.selfalarmproject;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.database.DatabaseHelper;
import hcmute.edu.vn.selfalarmproject.model.SMSData;

public class SMSListActivity extends AppCompatActivity {
    
    private static final String TAG = "SMSListActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_list);
        
        ListView listView = findViewById(R.id.sms_list_view);
        
        // Load SMS from SQLite database
        loadSMSFromDatabase(listView);
    }
    
    private void loadSMSFromDatabase(ListView listView) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            List<SMSData> smsList = dbHelper.getAllSMS();
            
            if (smsList.isEmpty()) {
                Toast.makeText(this, "Không có tin nhắn SMS nào", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Convert to display format
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

            // Create and set adapter
            SimpleAdapter adapter = new SimpleAdapter(
                    this,
                    data,
                    R.layout.sms_list_item,
                    new String[]{"address", "body", "date", "type"},
                    new int[]{R.id.sms_address, R.id.sms_body, R.id.sms_date, R.id.sms_type}
            );

            listView.setAdapter(adapter);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS data: " + e.getMessage());
            Toast.makeText(this, "Không thể tải dữ liệu tin nhắn", Toast.LENGTH_SHORT).show();
        }
    }
}