# Android Auto Integration

## Overview

This library has been updated to support Android Auto by implementing a `MediaBrowserServiceCompat`. This allows your media controls to appear in Android Auto's interface while maintaining backward compatibility with existing functionality.

## Changes Made

### 1. New MediaBrowserService Implementation

**File:** `ForegroundMediaBrowserService.java`

A new service class that extends `MediaBrowserServiceCompat` has been created. This service:

- Exposes the media session to Android Auto and other media browsers
- Handles media control callbacks (play, pause, next, previous, stop)
- Forwards all media events to your React Native app via the existing event system
- Manages playback state for display in Android Auto

### 2. AndroidManifest.xml Updates

The manifest now includes the MediaBrowserService declaration with the proper intent filter:

```xml
<service
    android:name=".ForegroundMediaBrowserService"
    android:exported="true">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>
```

### 3. NotificationHelper Integration

The `NotificationHelper` class has been updated to:

- Prefer using the MediaSession from `MediaBrowserService` when available
- Fall back to creating its own MediaSession for backward compatibility
- Properly delegate playback state updates to the MediaBrowserService
- Avoid releasing the MediaSession if it's managed by the MediaBrowserService

### 4. ForegroundService Lifecycle Management

The `ForegroundService` now:

- Starts the `MediaBrowserService` when the foreground service starts
- Stops the `MediaBrowserService` when the foreground service is destroyed
- Ensures proper coordination between both services

## How It Works

1. When your foreground service starts, it automatically starts the `ForegroundMediaBrowserService`
2. The MediaBrowserService creates and manages a MediaSession that's compatible with Android Auto
3. Both the notification controls and Android Auto interface use the same MediaSession
4. All media control events (play, pause, next, previous) are forwarded to your React Native app through the existing event system
5. You can still update playback states from your React Native code, and they'll reflect in both the notification and Android Auto

## Compatibility

- ✅ **Backward Compatible**: Existing code continues to work without changes
- ✅ **Android Auto**: Your app will now appear in Android Auto with media controls
- ✅ **Lock Screen Controls**: Existing lock screen controls continue to work
- ✅ **Notification Controls**: Existing notification controls continue to work

## Testing

To test Android Auto integration:

1. Enable Android Auto developer mode on your device
2. Connect your device to a car with Android Auto, or use the Android Auto desktop head unit (DHU)
3. Start your foreground service with media controls
4. Your app should appear in the Android Auto media section
5. Media controls in Android Auto should trigger the same events as notification controls

## No Code Changes Required

Your existing React Native code doesn't need any changes. All the media control events (`media_play`, `media_pause`, `media_next`, `media_previous`, `media_stop`) work exactly as before, but now they'll also work from Android Auto!
