package hcmute.edu.vn.selfalarmproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    ImageButton btn_listen ;
    ImageView imgOptimizeBattery;
    ImageView img_schedule; // Changed to ImageView since it's imageView4 in XML

    ImageView smsCall ;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Find views in XML
        btn_listen = findViewById(R.id.listen_song);  // ImageButton for music
        img_schedule = findViewById(R.id.imageView4); // ImageView for schedule

        // Open PlayMusic Activity
        btn_listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayMusic.class);
                startActivity(intent);
            }
        });
        imgOptimizeBattery = findViewById(R.id.btn_optimizeBattery);
        imgOptimizeBattery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BatteryOptimization.class);
            startActivity(intent);
        });


        img_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManagePersonalScheduleActivity.class);
                startActivity(intent);
            }
        });

        smsCall = findViewById(R.id.smscall);

        smsCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SMSCallActivity.class);
                startActivity(intent);
            }
        });

    }
}
