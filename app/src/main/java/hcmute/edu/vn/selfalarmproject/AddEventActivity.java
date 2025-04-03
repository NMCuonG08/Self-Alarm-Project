package hcmute.edu.vn.selfalarmproject;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {

    private EditText eventName, eventTime;
    private Button btnSaveEvent;
    private String selectedTime = "";
    private boolean isEdit = false; // Flag to check if it's edit mode
    private String oldTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // Initialize UI components
        eventName = findViewById(R.id.eventName);
        eventTime = findViewById(R.id.eventTime);
        btnSaveEvent = findViewById(R.id.btnSaveEvent);

        // Check if it's an edit operation
        Intent intent = getIntent();
        if (intent.hasExtra("EDIT_TASK")) {
            isEdit = true; // It's an edit mode
            oldTask = intent.getStringExtra("EDIT_TASK");

            // Populate fields with the existing task data
            String[] taskParts = oldTask.split(" at ");
            eventName.setText(taskParts[0]);  // Get event name
            eventTime.setText(taskParts[1]);  // Get event time
            selectedTime = taskParts[1];  // Set selected time
            btnSaveEvent.setText("Update Event"); // Change button text for editing
        }

        // Open time picker when clicking on eventTime field
        eventTime.setOnClickListener(view -> showTimePicker());

        // Save event and send data back
        btnSaveEvent.setOnClickListener(view -> saveEvent());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                    eventTime.setText(selectedTime);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void saveEvent() {
        String name = eventName.getText().toString().trim();
        if (name.isEmpty() || selectedTime.isEmpty()) {
            eventName.setError("Enter event name and time!");
            return;
        }

        // Prepare the data to send back to ViewScheduleActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("EVENT_NAME", name);
        resultIntent.putExtra("EVENT_TIME", selectedTime);

        // If editing, send the old task for updating
        if (isEdit) {
            resultIntent.putExtra("EDIT_TASK", oldTask); // Pass old task for updating
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
