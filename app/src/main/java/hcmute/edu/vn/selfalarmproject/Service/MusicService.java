package hcmute.edu.vn.selfalarmproject.Service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.PlayMusic;
import hcmute.edu.vn.selfalarmproject.R;
import hcmute.edu.vn.selfalarmproject.model.Song;

public class MusicService extends Service {
    private static final String TAG = "MusicService";

    // Actions for notification controls
    public static final String ACTION_PLAY = "hcmute.edu.vn.selfalarmproject.action.PLAY";
    public static final String ACTION_PAUSE = "hcmute.edu.vn.selfalarmproject.action.PAUSE";
    public static final String ACTION_PREVIOUS = "hcmute.edu.vn.selfalarmproject.action.PREVIOUS";
    public static final String ACTION_NEXT = "hcmute.edu.vn.selfalarmproject.action.NEXT";
    public static final String ACTION_STOP = "hcmute.edu.vn.selfalarmproject.action.STOP";

    // Notification constants
    private static final String CHANNEL_ID = "music_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    // Media player
    private ExoPlayer exoPlayer;
    private List<Song> playlist = new ArrayList<>();
    private int currentSongIndex = -1;
    private Song currentSong;
    private boolean isPlaying = false;

    // MediaSession for lock screen controls
    private MediaSessionCompat mediaSession;

    // Wake lock to prevent sleep during playback
    private PowerManager.WakeLock wakeLock;

    // Binder for activity connection
    private final IBinder binder = new MusicBinder();

    // Callback for notifying activity of state changes
    private MusicCallback callback;

    // BroadcastReceiver for notification actions
    private BroadcastReceiver notificationReceiver;

    // Shared preferences for storing state
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService created");

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MusicService", MODE_PRIVATE);

        // Create notification channel
        createNotificationChannel();

        // Initialize MediaSession
        initMediaSession();

        // Register notification receiver
        registerNotificationReceiver();

        // Set up ExoPlayer listeners
        setupExoPlayerListeners();

        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicService::WakeLock");
    }

    private void setupExoPlayerListeners() {
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Song completed
                    if (callback != null) {
                        callback.onPlaybackCompleted();
                    } else {
                        // If activity is not connected, auto-play next song
                        playNextSong();
                    }
                } else if (playbackState == Player.STATE_READY) {
                    // Update duration when media is loaded
                    if (callback != null) {
                        callback.onDurationChanged(exoPlayer.getDuration());
                    }
                    // Update notification
                    updateNotification();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;

                // Update callback
                if (callback != null) {
                    callback.onPlaybackStateChanged(playing);
                }

                // Update MediaSession state
                updateMediaSessionPlaybackState();

                // Update notification
                updateNotification();

                // Handle wake lock
                if (playing && !wakeLock.isHeld()) {
                    wakeLock.acquire(3600000); // 1 hour max
                } else if (!playing && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        });
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicService");

        // Set media session callback
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNextSong();
            }

            @Override
            public void onSkipToPrevious() {
                playPreviousSong();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo(pos);
            }
        });

        // Enable callbacks from media buttons and transport controls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Start the session
        mediaSession.setActive(true);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case ACTION_PLAY:
                            play();
                            break;
                        case ACTION_PAUSE:
                            pause();
                            break;
                        case ACTION_PREVIOUS:
                            playPreviousSong();
                            break;
                        case ACTION_NEXT:
                            playNextSong();
                            break;
                        case ACTION_STOP:
                            stop();
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_STOP);

        // Fix: Add RECEIVER_NOT_EXPORTED flag for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Phát nhạc",
                    NotificationManager.IMPORTANCE_LOW); // LOW importance to avoid sound/vibration

            channel.setDescription("Điều khiển phát nhạc");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        // Handle action if intent contains one
        if (intent != null && intent.getAction() != null) {
            handleIntentAction(intent.getAction());
        }

        // Return sticky so service restarts if killed
        return START_STICKY;
    }

    private void handleIntentAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                play();
                break;
            case ACTION_PAUSE:
                pause();
                break;
            case ACTION_PREVIOUS:
                playPreviousSong();
                break;
            case ACTION_NEXT:
                playNextSong();
                break;
            case ACTION_STOP:
                stop();
                break;
        }
    }

    public void playSong(Song song) {
        if (song == null) return;

        currentSong = song;

        // Find index of the song in playlist
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getUrl().equals(song.getUrl())) {
                currentSongIndex = i;
                break;
            }
        }

        try {
            // Prepare the player with the song's URL
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(song.getUrl()));
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();

            // Start playback automatically
            exoPlayer.play();

            // Update and show notification
            updateNotification();

            // Save current song to prefs
            saveCurrentSession();

            Log.d(TAG, "Started playing song: " + song.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error playing song: " + e.getMessage());
        }
    }

    private void play() {
        if (exoPlayer != null) {
            exoPlayer.play();
            isPlaying = true;
            updateNotification();
            updateMediaSessionPlaybackState();
        }
    }

    private void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
            isPlaying = false;
            updateNotification();
            updateMediaSessionPlaybackState();
        }
    }

    public void togglePlayPause() {
        if (exoPlayer != null) {
            if (isPlaying) {
                pause();
            } else {
                play();
            }
        }
    }

    public void seekTo(long position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position);

            // Update media session
            updateMediaSessionPlaybackState();

            // Notify callback
            if (callback != null) {
                callback.onPositionChanged(position);
            }
        }
    }

    public Song playPreviousSong() {
        if (playlist.isEmpty() || currentSongIndex <= 0) {
            return null;
        }

        currentSongIndex--;
        Song song = playlist.get(currentSongIndex);
        playSong(song);
        return song;
    }

    public Song playNextSong() {
        if (playlist.isEmpty() || currentSongIndex >= playlist.size() - 1) {
            return null;
        }

        currentSongIndex++;
        Song song = playlist.get(currentSongIndex);
        playSong(song);
        return song;
    }

    public void setPlaylist(List<Song> playlist) {
        this.playlist = new ArrayList<>(playlist);
    }

    public void setCallback(MusicCallback callback) {
        this.callback = callback;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public long getCurrentPosition() {
        if (exoPlayer != null) {
            return exoPlayer.getCurrentPosition();
        }
        return 0;
    }

    public long getDuration() {
        if (exoPlayer != null) {
            return exoPlayer.getDuration();
        }
        return 0;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    private void saveCurrentSession() {
        if (currentSong == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastSongName", currentSong.getName());
        editor.putString("lastSongUrl", currentSong.getUrl());
        editor.putInt("lastPosition", (int) getCurrentPosition());
        editor.putInt("currentSongIndex", currentSongIndex);
        editor.apply();
    }

    public boolean restoreLastSession() {
        String lastSongName = prefs.getString("lastSongName", null);
        String lastSongUrl = prefs.getString("lastSongUrl", null);
        int lastPosition = prefs.getInt("lastPosition", 0);
        int lastIndex = prefs.getInt("currentSongIndex", -1);

        if (lastSongName != null && lastSongUrl != null) {
            currentSongIndex = lastIndex;
            currentSong = new Song(lastSongName, lastSongUrl);

            try {
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(lastSongUrl));
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.seekTo(lastPosition);

                // Don't auto-play when restoring
                updateNotification();

                Log.d(TAG, "Restored session: " + lastSongName + " at position " + lastPosition);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error restoring session: " + e.getMessage());
            }
        }

        return false;
    }

    private void updateMediaSessionPlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                        isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        getCurrentPosition(),
                        1.0f
                );

        mediaSession.setPlaybackState(stateBuilder.build());

        // Update metadata
        if (currentSong != null) {
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getName())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.getName())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

            // Add album art if available
            Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_play);
            if (albumArt != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, albumArt);
            }

            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    private void updateNotification() {
        if (currentSong == null) return;

        // Create notification with media controls
        Notification notification = createNotification();

        // Update foreground service
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification createNotification() {
        // Intent for launching the activity when notification is clicked
        Intent contentIntent = new Intent(this, PlayMusic.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Create action intents
        PendingIntent playPendingIntent = createActionPendingIntent(ACTION_PLAY);
        PendingIntent pausePendingIntent = createActionPendingIntent(ACTION_PAUSE);
        PendingIntent prevPendingIntent = createActionPendingIntent(ACTION_PREVIOUS);
        PendingIntent nextPendingIntent = createActionPendingIntent(ACTION_NEXT);
        PendingIntent stopPendingIntent = createActionPendingIntent(ACTION_STOP);

        // Create media style notification
        MediaStyle mediaStyle = new MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2); // Previous, Play/Pause, Next

        // Create notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle(currentSong.getName())
                .setContentText("Đang phát nhạc")
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(mediaStyle)
                .setOngoing(isPlaying);

        // Add media control actions
        builder.addAction(R.drawable.ic_previous, "Previous", prevPendingIntent);

        // Add play or pause button based on current state
        if (isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent);
        } else {
            builder.addAction(R.drawable.ic_play, "Play", playPendingIntent);
        }

        builder.addAction(R.drawable.ic_next, "Next", nextPendingIntent);
        builder.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent);

        return builder.build();
    }

    private PendingIntent createActionPendingIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this,
                getActionRequestCode(action),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private int getActionRequestCode(String action) {
        // Each action needs a unique request code
        switch (action) {
            case ACTION_PLAY:
                return 1;
            case ACTION_PAUSE:
                return 2;
            case ACTION_PREVIOUS:
                return 3;
            case ACTION_NEXT:
                return 4;
            case ACTION_STOP:
                return 5;
            default:
                return 0;
        }
    }

    private void stop() {
        // Stop playback
        if (exoPlayer != null) {
            exoPlayer.stop();
        }

        // Release wake lock if held
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Save current position before stopping
        saveCurrentSession();

        // Stop foreground service and remove notification
        stopForeground(true);

        // Stop the service
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MusicService destroyed");

        // Save current session before destroying
        saveCurrentSession();

        // Release ExoPlayer
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }

        // Release wake lock if held
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Unregister notification receiver
        if (notificationReceiver != null) {
            try {
                unregisterReceiver(notificationReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }

        // Release media session
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }

        super.onDestroy();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public interface MusicCallback {
        void onDurationChanged(long duration);
        void onPositionChanged(long position);
        void onPlaybackStateChanged(boolean isPlaying);
        void onPlaybackCompleted();
    }

    // Song class if you don't want to use the model class
    public static class Song {
        private String name;
        private String url;

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
    }
}