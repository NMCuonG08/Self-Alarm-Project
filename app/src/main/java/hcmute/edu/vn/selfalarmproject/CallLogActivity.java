package hcmute.edu.vn.selfalarmproject;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.selfalarmproject.Manager.CallManager;
import hcmute.edu.vn.selfalarmproject.model.CallLogData;

public class CallLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_log);

        ListView listView = findViewById(R.id.call_log_list_view);
        List<CallLogData> callLogs = CallManager.getCallLogs(this);

        List<Map<String, String>> data = new ArrayList<>();

        for (CallLogData call : callLogs) {
            Map<String, String> item = new HashMap<>();
            item.put("number", call.getNumber());
            item.put("name", call.getName() != null ? call.getName() : "Không xác định");
            item.put("date", call.getDateAsString());
            item.put("duration", call.getDurationAsString());
            item.put("type", call.getTypeAsString());

            data.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                R.layout.call_log_list_item,
                new String[]{"number", "name", "date", "duration", "type"},
                new int[]{R.id.call_number, R.id.call_name, R.id.call_date, R.id.call_duration, R.id.call_type}
        );

        listView.setAdapter(adapter);
    }
}