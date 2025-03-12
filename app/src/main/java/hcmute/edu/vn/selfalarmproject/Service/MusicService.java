package hcmute.edu.vn.selfalarmproject.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.PlayMusic;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new MusicBinder();
    private ExoPlayer player;
    private Song currentSong;
    private List<Song> playlist = new ArrayList<>();
    private PowerManager.WakeLock wakeLock;
    private MusicCallback callback;

    // Interface for callbacks to activity
    public interface MusicCallback {
        void onDurationChanged(long duration);
        void onPositionChanged(long position);
        void onPlaybackStateChanged(boolean isPlaying);
        void onPlaybackCompleted();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel for Android O+
        createNotificationChannel();

        // Get wake lock to keep CPU running when screen is off
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicService::WakeLock");

        // Initialize player
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    if (callback != null) {
                        callback.onDurationChanged(player.getDuration());
                    }
                    updateNotification();
                } else if (state == Player.STATE_ENDED) {
                    if (callback != null) {
                        callback.onPlaybackCompleted();
                    }
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Show notification and start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        return START_NOT_STICKY;
    }

    // Set callback for updating UI
    public void setCallback(MusicCallback callback) {
        this.callback = callback;
    }

    // Set full playlist
    public void setPlaylist(List<Song> playlist) {
        this.playlist = new ArrayList<>(playlist);
    }

    // Check if playlist has content
    public boolean hasPlaylist() {
        return playlist != null && !playlist.isEmpty();
    }

    // Get current position
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    // Get total duration
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    // Check if music is playing
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    // Get current song
    public Song getCurrentSong() {
        return currentSong;
    }

    // Play a song
    public void playSong(Song song) {
        if (song == null || song.getUrl() == null || song.getUrl().isEmpty()) {
            Toast.makeText(this, "URL bài hát không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("MusicService", "Playing song: " + song.getName());
        currentSong = song;
        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(song.getUrl())));
        player.prepare();
        player.play();

        if (callback != null) {
            callback.onPlaybackStateChanged(true);
        }

        updateNotification();
    }

    // Toggle play/pause
    public void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }

        if (callback != null) {
            callback.onPlaybackStateChanged(player.isPlaying());
        }

        updateNotification();
    }

    // Seek to position
    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }

    // Play next song
    public Song playNextSong() {
        if (currentSong == null || playlist.isEmpty()) return null;

        int currentIndex = playlist.indexOf(currentSong);
        if (currentIndex < playlist.size() - 1) {
            currentSong = playlist.get(currentIndex + 1);
            playSong(currentSong);
            return currentSong;
        }
        return null;
    }

    // Play previous song
    public Song playPreviousSong() {
        if (currentSong == null || playlist.isEmpty()) return null;

        int currentIndex = playlist.indexOf(currentSong);
        if (currentIndex > 0) {
            currentSong = playlist.get(currentIndex - 1);
            playSong(currentSong);
            return currentSong;
        }
        return null;
    }

    // Create notification channel (required for Android O+)
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW); // Low importance to avoid sound
            channel.setDescription("Shows what's currently playing");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Create and update notification
    private Notification createNotification() {
        // Create intent for when user taps notification
        Intent notificationIntent = new Intent(this, PlayMusic.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong != null ? currentSong.getName() : "Music Player")
                .setContentText("Now Playing")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    // Save current playback state to SharedPreferences
    private void saveCurrentState() {
        if (currentSong == null) return;

        SharedPreferences prefs = getSharedPreferences("MusicServicePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastSongName", currentSong.getName());
        editor.putString("lastSongUrl", currentSong.getUrl());
        editor.putLong("lastPosition", getCurrentPosition());
        editor.apply();

        Log.d("MusicService", "Saved state: song=" + currentSong.getName() +
                ", position=" + getCurrentPosition());
    }

    // Restore last playback session
    public boolean restoreLastSession() {
        SharedPreferences prefs = getSharedPreferences("MusicServicePrefs", MODE_PRIVATE);
        String songName = prefs.getString("lastSongName", null);
        String songUrl = prefs.getString("lastSongUrl", null);
        long position = prefs.getLong("lastPosition", 0);

        if (songName != null && songUrl != null) {
            Log.d("MusicService", "Restoring song: " + songName + " at position " + position);
            Song song = new Song(songName, songUrl);
            playSong(song);
            seekTo(position);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        saveCurrentState();

        if (player != null) {
            player.release();
            player = null;
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        super.onDestroy();
    }

    // Song class (static inner class)
    public static class Song {
        private final String name;
        private final String url;

        public Song(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Song song = (Song) obj;
            return name.equals(song.name) && url.equals(song.url);
        }
    }
}