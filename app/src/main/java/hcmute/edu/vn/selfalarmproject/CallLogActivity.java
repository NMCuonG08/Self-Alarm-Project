package hcmute.edu.vn.selfalarmproject;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.database.DatabaseHelper;
import hcmute.edu.vn.selfalarmproject.model.CallLogData;

public class CallLogActivity extends AppCompatActivity {

    private static final String TAG = "CallLogActivity";
    private Button addTestCallBtn;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout errorLayout;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_log);

        // Initialize views
        listView = findViewById(R.id.call_list_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        errorLayout = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);
        
        // Setup swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadCallsFromSQLite);
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
        }
        
        // Add test button
        addTestCallBtn = findViewById(R.id.add_test_call_btn);
        if (addTestCallBtn != null) {
            addTestCallBtn.setOnClickListener(v -> addTestCall());
        }
        
        // Load data
        loadCallsFromSQLite();
    }

    private void loadCallsFromSQLite() {
        try {
            Log.d(TAG, "Loading calls from SQLite");
            
            // Get database helper
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            
            // Get all calls
            List<CallLogData> callList = dbHelper.getAllCalls();
            
            Log.d(TAG, "Retrieved " + callList.size() + " calls from SQLite");
            
            // If swipe refresh is active, stop it
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            
            // Hide error layout if visible
            if (errorLayout != null) {
                errorLayout.setVisibility(View.GONE);
            }
            
            // Check if we have any data
            if (callList.isEmpty()) {
                Log.d(TAG, "No call logs found in SQLite");
                Toast.makeText(this, "Không có cuộc gọi nào", Toast.LENGTH_SHORT).show();
                listView.setAdapter(null);
                return;
            }
            
            // Prepare data for display
            List<Map<String, String>> data = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            
            for (CallLogData call : callList) {
                Map<String, String> item = new HashMap<>();
                
                // Phone number
                item.put("number", call.getPhoneNumber() != null ? call.getPhoneNumber() : "Unknown");
                
                // Date
                String dateStr = call.getDate() > 0 ? 
                    dateFormat.format(new Date(call.getDate())) : "Unknown";
                item.put("date", dateStr);
                
                // Call type
                String type = call.getType();
                String typeDisplay = "Không xác định";
                
                if ("incoming".equals(type)) {
                    typeDisplay = "Cuộc gọi đến";
                } else if ("outgoing".equals(type)) {
                    typeDisplay = "Cuộc gọi đi";
                } else if ("missed".equals(type)) {
                    typeDisplay = "Cuộc gọi nhỡ";
                }
                
                item.put("type", typeDisplay);
                
                data.add(item);
            }
            
            // Create and set adapter
            SimpleAdapter adapter = new SimpleAdapter(
                    this,
                    data,
                    R.layout.call_log_item,
                    new String[]{"number", "date", "type"},
                    new int[]{R.id.call_number, R.id.call_date, R.id.call_type}
            );
            
            listView.setAdapter(adapter);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading call data: " + e.getMessage());
            e.printStackTrace();
            
            // If swipe refresh is active, stop it
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            
            // Show error
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Show error layout if available
            if (errorLayout != null && errorText != null) {
                errorText.setText("Lỗi tải dữ liệu: " + e.getMessage());
                errorLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addTestCall() {
        try {
            Log.d(TAG, "Adding test call to SQLite");
            
            // Get database helper
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            
            // Add test call
            long timestamp = System.currentTimeMillis();
            long id = dbHelper.addCall("0987654321", timestamp, "incoming");
            
            if (id > 0) {
                Log.d(TAG, "Test call added to SQLite with ID: " + id);
                Toast.makeText(this, "Đã thêm cuộc gọi thử nghiệm", Toast.LENGTH_SHORT).show();
                
                // Reload data
                loadCallsFromSQLite();
            } else {
                Log.e(TAG, "Failed to add test call to SQLite");
                Toast.makeText(this, "Thêm cuộc gọi thất bại", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding test call: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}