package hcmute.edu.vn.selfalarmproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;



public class ManagePersonalScheduleActivity extends AppCompatActivity {

    private ImageView scheduleImage;
    private TextView description;
    private Button btnAddEvent, btnViewSchedule;
    private RecyclerView taskList;
    private hcmute.edu.vn.selfalarmproject.TaskAdapter taskAdapter;
    private List<String> taskData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_personal_schedule); // Ensure correct XML filename

        // Initialize Views
        scheduleImage = findViewById(R.id.scheduleImage);
        description = findViewById(R.id.description);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        taskList = findViewById(R.id.taskList);

        // Setup RecyclerView
        taskData = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskData);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskList.setAdapter(taskAdapter);

        // Load Sample Data
        loadSampleTasks();

        // Button Click Listeners
        btnAddEvent.setOnClickListener(view -> openAddEventActivity());
        btnViewSchedule.setOnClickListener(view -> openViewScheduleActivity());
    }

    private void loadSampleTasks() {
        taskData.add("Meeting at 10 AM");
        taskData.add("Workout Session at 6 PM");
        taskData.add("Doctor Appointment at 3 PM");
        taskAdapter.notifyDataSetChanged();
    }

    private void openAddEventActivity() {
        Intent intent = new Intent(this, AddEventActivity.class);
        startActivity(intent);
    }

    private void openViewScheduleActivity() {
        Intent intent = new Intent(this, ViewScheduleActivity.class);
        startActivity(intent);
    }
}
