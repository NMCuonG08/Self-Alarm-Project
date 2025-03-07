package hcmute.edu.vn.selfalarmproject;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference database;
    private ExoPlayer player;
    private String songToPlay = "My Song"; // Tên bài hát cần phát

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance().getReference();

        // Tìm và phát bài hát theo tên
        findSongByName(songToPlay);

        // Nút Play thử nghiệm
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ImageButton btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> playMusic(null));
    }

    // Tìm bài hát theo tên trong Firebase
    private void findSongByName(String songName) {
        DatabaseReference songsRef = database.child("songs");

        songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot songSnapshot : snapshot.getChildren()) {
                    String name = songSnapshot.child("name").getValue(String.class);
                    String url = songSnapshot.child("url").getValue(String.class);

                    if (name != null && name.equalsIgnoreCase(songName)) {
                        Log.d("Firebase", "Found song: " + name);
                        playMusic(url);
                        return; // Dừng vòng lặp sau khi tìm thấy bài hát
                    }
                }
                Log.w("Firebase", "Song not found: " + songName);
                Toast.makeText(MainActivity.this, "Không tìm thấy bài hát!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Phát nhạc từ URL
    private void playMusic(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "URL nhạc không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
        }

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        Toast.makeText(this, "Đang phát: " + url, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
