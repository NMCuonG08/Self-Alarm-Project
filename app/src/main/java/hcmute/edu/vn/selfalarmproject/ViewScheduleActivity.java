package hcmute.edu.vn.selfalarmproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewScheduleActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SchedulePrefs";
    private static final String KEY_EVENTS = "events";

    private LinearLayout taskContainer;
    private SharedPreferences sharedPreferences;
    private List<String> taskData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule); // Make sure this XML exists

        taskContainer = findViewById(R.id.taskContainer); // LinearLayout to display tasks

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load events from SharedPreferences
        taskData = loadEvents();

        // Display tasks
        displayTasks();
    }

    private void displayTasks() {
        taskContainer.removeAllViews(); // Clear previous views

        // Check if there are no tasks
        if (taskData.isEmpty()) {
            TextView noTasksText = new TextView(this);
            noTasksText.setText("No tasks available");
            taskContainer.addView(noTasksText);
        }

        for (String task : taskData) {
            // Create a new TextView for each task
            TextView taskView = new TextView(this);
            taskView.setText(task);
            taskView.setTextSize(16);
            taskView.setPadding(10, 10, 10, 10);

            // Create Edit and Delete buttons for each task
            Button btnEdit = new Button(this);
            btnEdit.setText("Edit");
            btnEdit.setOnClickListener(v -> editTask(task));

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setOnClickListener(v -> deleteTask(task));

            // Create a horizontal layout to display task and buttons
            LinearLayout taskLayout = new LinearLayout(this);
            taskLayout.setOrientation(LinearLayout.HORIZONTAL);
            taskLayout.addView(taskView);
            taskLayout.addView(btnEdit);
            taskLayout.addView(btnDelete);

            // Add this layout to the taskContainer
            taskContainer.addView(taskLayout);
        }
    }

    private void editTask(String oldTask) {
        // Start AddEventActivity to allow editing the task
        Intent intent = new Intent(this, AddEventActivity.class);
        intent.putExtra("EDIT_TASK", oldTask);
        startActivityForResult(intent, 1);
    }

    private void deleteTask(String task) {
        // Remove task from the list
        taskData.remove(task);

        // Save updated task list to SharedPreferences
        saveEvents();

        // Refresh the UI to reflect the changes
        displayTasks();
    }

    private void saveEvents() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> eventSet = new HashSet<>(taskData);
        editor.putStringSet(KEY_EVENTS, eventSet);
        editor.apply();
    }

    private List<String> loadEvents() {
        Set<String> eventSet = sharedPreferences.getStringSet(KEY_EVENTS, new HashSet<>());
        return new ArrayList<>(eventSet);
    }

    // Handle result from AddEventActivity (for editing)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String oldTask = data.getStringExtra("EDIT_TASK");
            String newTaskName = data.getStringExtra("EVENT_NAME");
            String newTaskTime = data.getStringExtra("EVENT_TIME");
            String updatedTask = newTaskName + " at " + newTaskTime;

            // Replace the old task with the updated one
            int index = taskData.indexOf(oldTask);
            if (index != -1) {
                taskData.set(index, updatedTask);
                saveEvents();
                displayTasks();  // Ensure UI is refreshed
            }
        }
    }
}
