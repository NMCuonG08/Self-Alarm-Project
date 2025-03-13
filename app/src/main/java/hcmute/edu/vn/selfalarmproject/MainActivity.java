package hcmute.edu.vn.selfalarmproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ImageButton btn_listen ;
    ImageView imgOptimizeBattery;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);

        btn_listen = findViewById(R.id.listen_song);

        btn_listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayMusic.class);
                startActivity(intent);
            }
        });
        ImageView imgOptimizeBattery = findViewById(R.id.imageView5);
        imgOptimizeBattery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BatteryOptimization.class);
            startActivity(intent);
        });



    }

}
