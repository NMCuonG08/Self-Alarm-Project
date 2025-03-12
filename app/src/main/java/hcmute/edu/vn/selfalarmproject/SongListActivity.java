package hcmute.edu.vn.selfalarmproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.Adapter.SongAdapter;
import hcmute.edu.vn.selfalarmproject.model.Song;

public class SongListActivity extends AppCompatActivity {

    private RecyclerView recyclerSongs;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private ImageView btnBack;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_list_activity);

        recyclerSongs = findViewById(R.id.songListView);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongAdapter(this, songList, song -> {
            // Gửi kết quả về PlayMusicActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("songName", song.getName());
            resultIntent.putExtra("songUrl", song.getUrl());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        recyclerSongs.setAdapter(adapter);
        btnBack = findViewById(R.id.btn_back_music);
        btnBack.setOnClickListener(v -> {
            finish();
        });
        loadSongsFromFirebase();
    }

    private void loadSongsFromFirebase() {
        FirebaseDatabase.getInstance().getReference().child("songs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        songList.clear();
                        for (DataSnapshot songSnap : snapshot.getChildren()) {
                            Song song = songSnap.getValue(Song.class);
                            if (song != null) {
                                songList.add(song);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SongListActivity.this, "Lỗi tải bài hát", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Error: " + error.getMessage());
                    }
                });
    }
}
