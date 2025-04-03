package hcmute.edu.vn.selfalarmproject;

import android.os.Build;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import hcmute.edu.vn.selfalarmproject.Adapter.TaskAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ManagePersonalScheduleActivity extends AppCompatActivity {
    private static final int ADD_EVENT_REQUEST = 1;
    private static final int EDIT_EVENT_REQUEST = 2;
    private static final String PREFS_NAME = "SchedulePrefs";
    private static final String KEY_EVENTS = "events";

    private ImageView scheduleImage;
    private TextView description;
    private Button btnAddEvent, btnViewSchedule;
    private RecyclerView taskList;
    private TaskAdapter taskAdapter;
    private List<String> taskData;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_personal_schedule);

        // Initialize Views
        scheduleImage = findViewById(R.id.scheduleImage);
        description = findViewById(R.id.description);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        taskList = findViewById(R.id.taskList);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load and display events
        loadAndDisplayEvents();

        // Button Click Listeners
        btnAddEvent.setOnClickListener(view -> openAddEventActivity(null, -1)); // No task for new event
        btnViewSchedule.setOnClickListener(view -> openViewScheduleActivity());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayEvents();
        scheduleAllNotifications(); // Re-schedule all notifications on return
    }

    private void openAddEventActivity(String existingEvent, int position) {
        Intent intent = new Intent(this, AddEventActivity.class);
        if (existingEvent != null) {
            intent.putExtra("EXISTING_EVENT", existingEvent);
            intent.putExtra("POSITION", position);
        }
        startActivityForResult(intent, existingEvent != null ? EDIT_EVENT_REQUEST : ADD_EVENT_REQUEST);
    }

    private void openViewScheduleActivity() {
        Intent intent = new Intent(this, ViewScheduleActivity.class);
        startActivity(intent);
    }

    private void loadAndDisplayEvents() {
        taskData = loadEvents();
        taskAdapter = new TaskAdapter(taskData, this, new TaskAdapter.OnTaskInteractionListener() {
            @Override
            public void onEditTask(int position) {
                String task = taskData.get(position);
                openAddEventActivity(task, position);
            }

            @Override
            public void onDeleteTask(int position) {
                String eventDetails = taskData.get(position);
                String eventName = eventDetails.split(" at ")[0];

                cancelNotification(eventName); // Cancel notification for deleted event
                taskData.remove(position);
                taskAdapter.notifyItemRemoved(position);
                saveEvents();
            }
        });
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskList.setAdapter(taskAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_EVENT_REQUEST && resultCode == RESULT_OK) {
            String eventName = data.getStringExtra("EVENT_NAME");
            String eventTime = data.getStringExtra("EVENT_TIME");
            String fullEvent = eventName + " at " + eventTime;

            taskData.add(fullEvent);
            taskAdapter.notifyDataSetChanged();
            saveEvents();

            scheduleNotification(eventName, eventTime);
        } else if (requestCode == EDIT_EVENT_REQUEST && resultCode == RESULT_OK) {
            int position = data.getIntExtra("POSITION", -1);
            if (position != -1) {
                String oldEventName = taskData.get(position).split(" at ")[0];
                cancelNotification(oldEventName); // Cancel old notification

                String newEventName = data.getStringExtra("EVENT_NAME");
                String newEventTime = data.getStringExtra("EVENT_TIME");
                String fullEvent = newEventName + " at " + newEventTime;

                taskData.set(position, fullEvent);
                taskAdapter.notifyDataSetChanged();
                saveEvents();

                scheduleNotification(newEventName, newEventTime); // Schedule new notification
            }
        }
    }

    private void scheduleAllNotifications() {
        for (String event : taskData) {
            String[] parts = event.split(" at ");
            if (parts.length == 2) {
                String eventName = parts[0];
                String eventTime = parts[1];
                scheduleNotification(eventName, eventTime);
            }
        }
    }

    private void scheduleNotification(String eventName, String eventTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("EVENT_NAME", eventName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, eventName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Split the eventTime string (assumed format "HH:mm")
        String[] timeParts = eventTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Get the current calendar and set the time accordingly
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time is already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
            // Handle the case where the permission for exact alarms is not granted.
        }
    }


    // Request permission for exact alarms (for Android 12+)
    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
            Toast.makeText(this, "Grant exact alarm permission in settings.", Toast.LENGTH_LONG).show();
        }
    }

    private void cancelNotification(String eventName) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, eventName.hashCode(), intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
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
}
