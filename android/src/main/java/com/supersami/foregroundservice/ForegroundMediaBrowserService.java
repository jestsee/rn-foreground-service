package com.supersami.foregroundservice;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * MediaBrowserServiceCompat implementation for Android Auto compatibility.
 * This service exposes the media session to Android Auto and other media browsers.
 */
public class ForegroundMediaBrowserService extends MediaBrowserServiceCompat {
    private static final String TAG = "MediaBrowserService";
    private static final String MEDIA_ROOT_ID = "root";
    private static final String EMPTY_MEDIA_ROOT_ID = "empty_root";
    
    private static ForegroundMediaBrowserService instance;
    private MediaSessionCompat mediaSession;
    
    public static ForegroundMediaBrowserService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, TAG);
        
        // Set up the media session callbacks
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "MediaSession onPlay called - forwarding to app");
                emitMediaSessionEvent("media_play");
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
            
            @Override
            public void onPause() {
                Log.d(TAG, "MediaSession onPause called - forwarding to app");
                emitMediaSessionEvent("media_pause");
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            
            @Override
            public void onSkipToNext() {
                Log.d(TAG, "MediaSession onSkipToNext called - forwarding to app");
                emitMediaSessionEvent("media_next");
            }
            
            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "MediaSession onSkipToPrevious called - forwarding to app");
                emitMediaSessionEvent("media_previous");
            }
            
            @Override
            public void onStop() {
                Log.d(TAG, "MediaSession onStop called - forwarding to app");
                emitMediaSessionEvent("media_stop");
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
            
            @Override
            public void onSeekTo(long pos) {
                Log.d(TAG, "MediaSession onSeekTo called - forwarding to app");
                // Can be extended if needed
            }
        });
        
        // Set initial playback state
        updatePlaybackState(PlaybackStateCompat.STATE_NONE);
        
        // Make the session active
        mediaSession.setActive(true);
        
        // Set the session token so clients can communicate with it
        setSessionToken(mediaSession.getSessionToken());
        
        Log.d(TAG, "MediaBrowserService created and MediaSession initialized");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        instance = null;
        Log.d(TAG, "MediaBrowserService destroyed");
    }
    
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Allow Android Auto and other trusted clients to connect
        // For production, you should implement proper package validation
        Log.d(TAG, "onGetRoot: clientPackageName=" + clientPackageName + ", clientUid=" + clientUid);
        
        // Return a valid root for Android Auto and other media browsers
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }
    
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Android Auto will call this to browse media content
        // For a foreground service that just displays media controls, 
        // we can return an empty list or minimal content
        Log.d(TAG, "onLoadChildren: parentId=" + parentId);
        
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        
        if (MEDIA_ROOT_ID.equals(parentId)) {
            // Return empty list - we're just providing controls, not browseable content
            result.sendResult(mediaItems);
        } else {
            // Return empty for any other parent
            result.sendResult(mediaItems);
        }
    }
    
    /**
     * Get the MediaSession instance
     */
    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }
    
    /**
     * Update the playback state
     */
    public void updatePlaybackState(int state) {
        if (mediaSession == null) {
            Log.w(TAG, "MediaSession is null, cannot update playback state");
            return;
        }
        
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        
        // Set available actions
        playbackStateBuilder.setActions(
            PlaybackStateCompat.ACTION_PLAY |
            PlaybackStateCompat.ACTION_PAUSE |
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
            PlaybackStateCompat.ACTION_STOP |
            PlaybackStateCompat.ACTION_SEEK_TO
        );
        
        playbackStateBuilder.setState(
            state,
            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            1.0f
        );
        
        mediaSession.setPlaybackState(playbackStateBuilder.build());
        Log.d(TAG, "Playback state updated to: " + state);
    }
    
    /**
     * Emit media session events to React Native
     */
    private void emitMediaSessionEvent(String eventType) {
        // Forward to ForegroundService to emit to React Native
        ForegroundService serviceInstance = ForegroundService.getInstance();
        if (serviceInstance != null) {
            serviceInstance.emitMediaSessionEvent(eventType);
        } else {
            Log.w(TAG, "Cannot emit event - ForegroundService instance is null");
        }
    }
}
