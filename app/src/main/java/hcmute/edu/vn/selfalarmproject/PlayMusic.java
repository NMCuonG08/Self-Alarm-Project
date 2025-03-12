package hcmute.edu.vn.selfalarmproject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.Service.MusicService;

public class PlayMusic extends AppCompatActivity implements MusicService.MusicCallback {

    private DatabaseReference database;
    private List<MusicService.Song> playlist = new ArrayList<>();
    private String songToPlay = "Songs"; // Tên bài hát cần phát

    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrevious, btnReset, btnNext, btnSongList;

    private ImageView btnBack;
    private TextView txtSong;
    private Animation pulseAnimation;

    private Handler handler = new Handler();
    private Runnable seekBarUpdater;
    private Runnable stateChecker;

    // Service variables
    private MusicService musicService;
    private boolean serviceBound = false;
    private Intent serviceIntent;

    // Service connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(PlayMusic.this);
            serviceBound = true;

            // Check if service already has a current song
            MusicService.Song currentSong = musicService.getCurrentSong();

            if (currentSong != null) {
                // Service already has a song loaded
                Log.d("PlayMusic", "Service already has song: " + currentSong.getName());
                updateUI();
            } else {
                // Try to restore last session first
                boolean restored = musicService.restoreLastSession();

                // If we couldn't restore or there's no saved session, use playlist
                if (!restored && !playlist.isEmpty()) {
                    Log.d("PlayMusic", "Setting playlist to service");
                    musicService.setPlaylist(playlist);
                    findSongByName(songToPlay);
                }
            }

            // Start periodic state checking
            startStateChecker();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
            stopStateChecker();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.play_music);

        // Load the pulse animation
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.text_pulse);

        // Start and bind to music service
        serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference();

        // Map UI elements
        btnPlayPause = findViewById(R.id.btnPlay);
        seekBar = findViewById(R.id.seekBar);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnReset = findViewById(R.id.btnReset);
        btnNext = findViewById(R.id.btnNext);
        txtSong = findViewById(R.id.txtSong);
        btnSongList = findViewById(R.id.btn_songlist);
        btnBack = findViewById(R.id.btn_back);

        // Load song playlist
        loadPlaylist();

        // Button click listeners
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnReset.setOnClickListener(v -> resetSong());
        btnPrevious.setOnClickListener(v -> playPreviousSong());
        btnNext.setOnClickListener(v -> playNextSong());
        btnSongList.setOnClickListener(v -> openSongList());

        btnBack.setOnClickListener(v -> {
            finish();
        });

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Initialize seekbar updater
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                if (serviceBound && musicService.isPlaying()) {
                    seekBar.setProgress((int) musicService.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        };

        // Initialize state checker runnable
        stateChecker = new Runnable() {
            @Override
            public void run() {
                if (serviceBound && musicService != null) {
                    boolean isPlaying = musicService.isPlaying();
                    updatePlayPauseButton(isPlaying);
                    updateTextAnimation(isPlaying);
                }
                handler.postDelayed(this, 1000); // Check every second
            }
        };
    }

    // Start periodic state checking
    private void startStateChecker() {
        stopStateChecker(); // Clear any existing callbacks
        handler.post(stateChecker);
    }

    // Stop periodic state checking
    private void stopStateChecker() {
        handler.removeCallbacks(stateChecker);
    }

    // Update play/pause button based on current state
    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
    }

    // Update text animation based on current state
    private void updateTextAnimation(boolean isPlaying) {
        if (isPlaying) {
            if (txtSong.getAnimation() == null) {
                txtSong.startAnimation(pulseAnimation);
            }
        } else {
            txtSong.clearAnimation();
        }
    }

    private void openSongList() {
        Intent intent = new Intent(PlayMusic.this, SongListActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String songName = data.getStringExtra("songName");
            String songUrl = data.getStringExtra("songUrl");

            if (songName != null && songUrl != null && serviceBound) {
                MusicService.Song song = new MusicService.Song(songName, songUrl);
                musicService.seekTo(0);
                musicService.playSong(song);
                updateUI();
            }
        }
    }

    private void loadPlaylist() {
        database.child("songs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                playlist.clear();

                for (DataSnapshot songSnapshot : snapshot.getChildren()) {
                    String name = songSnapshot.child("name").getValue(String.class);
                    String url = songSnapshot.child("url").getValue(String.class);

                    if (name != null && url != null) {
                        playlist.add(new MusicService.Song(name, url));
                    }
                }

                if (playlist.isEmpty()) {
                    Toast.makeText(PlayMusic.this, "Danh sách bài hát trống!", Toast.LENGTH_SHORT).show();
                } else if (serviceBound) {
                    musicService.setPlaylist(playlist);
                    findSongByName(songToPlay);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Lỗi: " + error.getMessage());
                Toast.makeText(PlayMusic.this, "Lỗi tải danh sách nhạc!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findSongByName(String songName) {
        if (!serviceBound) return;

        for (MusicService.Song song : playlist) {
            if (song.getName().equalsIgnoreCase(songName)) {
                musicService.playSong(song);
                updateUI();
                return;
            }
        }

        Toast.makeText(this, "Không tìm thấy bài hát!", Toast.LENGTH_SHORT).show();
    }

    private void togglePlayPause() {
        if (serviceBound) {
            musicService.togglePlayPause();
            boolean isPlaying = musicService.isPlaying();
            updatePlayPauseButton(isPlaying);
            updateTextAnimation(isPlaying);

            if (isPlaying) {
                startSeekBarUpdate();
            } else {
                stopSeekBarUpdate();
            }
        }
    }

    private void resetSong() {
        if (serviceBound) {
            musicService.seekTo(0);
        }
    }

    private void playPreviousSong() {
        if (serviceBound) {
            MusicService.Song previousSong = musicService.playPreviousSong();
            if (previousSong == null) {
                Toast.makeText(this, "Đây là bài đầu tiên!", Toast.LENGTH_SHORT).show();
            } else {
                updateUI();
            }
        }
    }

    private void playNextSong() {
        if (serviceBound) {
            MusicService.Song nextSong = musicService.playNextSong();
            if (nextSong == null) {
                Toast.makeText(this, "Đây là bài cuối cùng!", Toast.LENGTH_SHORT).show();
            } else {
                updateUI();
            }
        }
    }

    private void updateUI() {
        if (!serviceBound || musicService == null) return;

        MusicService.Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            txtSong.setText(currentSong.getName());

            boolean isPlaying = musicService.isPlaying();
            updatePlayPauseButton(isPlaying);
            updateTextAnimation(isPlaying);

            // Update seekbar max value
            long duration = musicService.getDuration();
            if (duration > 0) {
                seekBar.setMax((int) duration);
                seekBar.setProgress((int) musicService.getCurrentPosition());
            }

            // Start seekbar updates if playing
            if (isPlaying) {
                startSeekBarUpdate();
            } else {
                stopSeekBarUpdate();
            }

            Log.d("PlayMusic", "UI updated with song: " + currentSong.getName() +
                    ", playing: " + isPlaying + ", position: " + musicService.getCurrentPosition());
        }
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate(); // Clear any existing runnables
        handler.post(seekBarUpdater);
    }

    private void stopSeekBarUpdate() {
        handler.removeCallbacks(seekBarUpdater);
    }

    // Callback implementations
    @Override
    public void onDurationChanged(long duration) {
        seekBar.setMax((int) duration);
    }

    @Override
    public void onPositionChanged(long position) {
        seekBar.setProgress((int) position);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
        updateTextAnimation(isPlaying);

        if (isPlaying) {
            startSeekBarUpdate();
        } else {
            stopSeekBarUpdate();
        }
    }

    @Override
    public void onPlaybackCompleted() {
        playNextSong();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // When returning to the app, ensure UI is synced with service
        if (serviceBound && musicService != null) {
            Log.d("PlayMusic", "onStart: Updating UI from service state");
            updateUI();
            startStateChecker();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSeekBarUpdate();
        stopStateChecker();
    }

    @Override
    protected void onDestroy() {
        stopSeekBarUpdate();
        stopStateChecker();
        if (serviceBound) {
            musicService.setCallback(null);
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }
}