package com.supersami.foregroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.facebook.react.R;

class NotificationHelper {
    private static final String TAG = "ForegroundService";
    private static final String NOTIFICATION_CHANNEL_ID = "com.supersami.foregroundservice.channel";

    private static NotificationHelper instance = null;
    private NotificationManager mNotificationManager;

    PendingIntent pendingBtnIntent;
    PendingIntent pendingBtn2Intent;
    PendingIntent pendingBtn3Intent;
    private Context context;
    private NotificationConfig config;
    private MediaSessionCompat mediaSession;

    public static synchronized NotificationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
        return instance;
    }

    private NotificationHelper(Context context) {
        mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        this.config = new NotificationConfig(context);
        
        // Initialize MediaSession if needed
        mediaSession = new MediaSessionCompat(context, "ForegroundService");
        mediaSession.setActive(true);
    }

    // Get the appropriate PendingIntent flags based on Android version
    private int getPendingIntentFlags(boolean isMutable) {
        // For Android 12+, we need to explicitly specify mutability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return isMutable ? PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_IMMUTABLE;
        } 
        // For Android 6.0+, use FLAG_UPDATE_CURRENT for compatibility
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
        // Fallback for older versions
        else {
            return 0;
        }
    }

    Notification buildNotification(Context context, Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "buildNotification: invalid config");
            return null;
        }
        Class mainActivityClass = getMainActivityClass(context);
        if (mainActivityClass == null) {
            return null;
        }

        // Main notification intent
        Intent notificationIntent = new Intent(context, mainActivityClass);
        notificationIntent.putExtra("mainOnPress", bundle.getString("mainOnPress"));
        int uniqueInt1 = (int) (System.currentTimeMillis() & 0xfffffff);

        // For the main intent we might need it to be mutable depending on the use case
        boolean mainIntentMutable = bundle.getBoolean("mainIntentMutable", false);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            uniqueInt1, 
            notificationIntent, 
            getPendingIntentFlags(mainIntentMutable)
        );

        // First button intent (if enabled)
        if (bundle.getBoolean("button", false)) {
            Intent notificationBtnIntent = new Intent("com.supersami.foregroundservice.BUTTON_ACTION");
            notificationBtnIntent.putExtra("buttonOnPress", bundle.getString("buttonOnPress"));
            notificationBtnIntent.putExtra("action", "button1"); // Add this!
            notificationBtnIntent.setPackage(context.getPackageName()); // Add this for security!
            int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);

            boolean buttonMutable = bundle.getBoolean("buttonMutable", false);
            pendingBtnIntent = PendingIntent.getBroadcast(
                context, 
                uniqueInt, 
                notificationBtnIntent, 
                getPendingIntentFlags(buttonMutable)
            );
        }

        // Second button intent (if enabled)
        if (bundle.getBoolean("button2", false)) {
            Intent notificationBtn2Intent = new Intent("com.supersami.foregroundservice.BUTTON_ACTION");
            notificationBtn2Intent.putExtra("button2OnPress", bundle.getString("button2OnPress"));
            notificationBtn2Intent.putExtra("action", "button2"); // Add this!
            notificationBtn2Intent.setPackage(context.getPackageName()); // Add this for security!
            int uniqueInt2 = (int) (System.currentTimeMillis() & 0xfffffff);

            boolean button2Mutable = bundle.getBoolean("button2Mutable", false);
            pendingBtn2Intent = PendingIntent.getBroadcast(
                context, 
                uniqueInt2, 
                notificationBtn2Intent, 
                getPendingIntentFlags(button2Mutable)
            );
        }
        
        // Third button intent (if enabled)
        if (bundle.getBoolean("button3", false)) {
            Intent notificationBtn3Intent = new Intent("com.supersami.foregroundservice.BUTTON_ACTION");
            notificationBtn3Intent.putExtra("button3OnPress", bundle.getString("button3OnPress"));
            notificationBtn3Intent.putExtra("action", "button3"); // Add this!
            notificationBtn3Intent.setPackage(context.getPackageName()); // Add this for security!
            int uniqueInt3 = (int) (System.currentTimeMillis() & 0xfffffff);

            boolean button3Mutable = bundle.getBoolean("button3Mutable", false);
            pendingBtn3Intent = PendingIntent.getBroadcast(
                context, 
                uniqueInt3, 
                notificationBtn3Intent, 
                getPendingIntentFlags(button3Mutable)
            );
        }

        String title = bundle.getString("title");

        int priority = NotificationCompat.PRIORITY_HIGH;
        final String priorityString = bundle.getString("importance");

        if (priorityString != null) {
            switch(priorityString.toLowerCase()) {
                case "max":
                    priority = NotificationCompat.PRIORITY_MAX;
                    break;
                case "high":
                    priority = NotificationCompat.PRIORITY_HIGH;
                    break;
                case "low":
                    priority = NotificationCompat.PRIORITY_LOW;
                    break;
                case "min":
                    priority = NotificationCompat.PRIORITY_MIN;
                    break;
                case "default":
                    priority = NotificationCompat.PRIORITY_DEFAULT;
                    break;
                default:
                    priority = NotificationCompat.PRIORITY_HIGH;
            }
        }

        int visibility = NotificationCompat.VISIBILITY_PRIVATE;
        String visibilityString = bundle.getString("visibility");

        if (visibilityString != null) {
            switch(visibilityString.toLowerCase()) {
                case "private":
                    visibility = NotificationCompat.VISIBILITY_PRIVATE;
                    break;
                case "public":
                    visibility = NotificationCompat.VISIBILITY_PUBLIC;
                    break;
                case "secret":
                    visibility = NotificationCompat.VISIBILITY_SECRET;
                    break;
                default:
                    visibility = NotificationCompat.VISIBILITY_PRIVATE;
            }
        }

        // Create notification channel for Android 8.0+
        checkOrCreateChannel(mNotificationManager, bundle);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setVisibility(visibility)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setOngoing(bundle.getBoolean("ongoing", false))
            .setContentText(bundle.getString("message"));

        // Add action buttons if configured
        int actionCount = 0;
        if (bundle.getBoolean("button", false)) {
            // Get custom icon or use default
            String buttonIcon = bundle.getString("buttonIcon", "ic_prev");
            int iconResId = getResourceIdForResourceName(context, buttonIcon);
            if (iconResId == 0) {
                if ("pause".equals(bundle.getString("buttonText"))) {
                    iconResId = android.R.drawable.ic_media_pause;
                } else {
                    iconResId = android.R.drawable.ic_media_rew; // fallback to system icon
                }
            }
            
            notificationBuilder.addAction(
                iconResId, 
                bundle.getString("buttonText", "Button"), 
                pendingBtnIntent
            );
            actionCount++;
        }

        if (bundle.getBoolean("button2", false)) {
            String button2Icon = bundle.getString("button2Icon", "ic_pause");
            int iconResId = getResourceIdForResourceName(context, button2Icon);
            if (iconResId == 0) {
                iconResId = android.R.drawable.ic_media_pause; // fallback to system icon
            }
            
            notificationBuilder.addAction(
                iconResId,
                bundle.getString("button2Text", "Button"), 
                pendingBtn2Intent
            );
            actionCount++;
        }

        if (bundle.getBoolean("button3", false)) {
            String button3Icon = bundle.getString("button3Icon", "ic_next");
            int iconResId = getResourceIdForResourceName(context, button3Icon);
            if (iconResId == 0) {
                iconResId = android.R.drawable.ic_media_ff; // fallback to system icon
            }
            
            notificationBuilder.addAction(
                iconResId, 
                bundle.getString("button3Text", "Button"), 
                pendingBtn3Intent
            );
            actionCount++;
        }
        
        // Set notification color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(this.config.getNotificationColor());
        }
        
        String color = bundle.getString("color");
        if (color != null) {
            try {
                notificationBuilder.setColor(Color.parseColor(color));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid color format: " + color);
            }
        }

        // Choose style based on configuration
        boolean useMediaStyle = bundle.getBoolean("useMediaStyle", true);
        boolean showActionsInCompact = bundle.getBoolean("showActionsInCompact", true);
        
        if (useMediaStyle && actionCount > 0) {
            // Use MediaStyle with actions in compact view
            MediaStyle mediaStyle = new MediaStyle()
                .setMediaSession(mediaSession.getSessionToken());
            
            if (showActionsInCompact) {
                // Show up to 3 actions in compact view
                if (actionCount == 1) {
                    mediaStyle.setShowActionsInCompactView(0);
                } else if (actionCount == 2) {
                    mediaStyle.setShowActionsInCompactView(0, 1);
                } else if (actionCount >= 3) {
                    mediaStyle.setShowActionsInCompactView(0, 1, 2);
                }
            }
            
            notificationBuilder.setStyle(mediaStyle);
        } else {
            // Use big text style for better readability
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bundle.getString("message")));
        }

        // Set small icon
        String iconName = bundle.getString("icon");
        if (iconName == null) {
            iconName = "ic_launcher";
        }
        notificationBuilder.setSmallIcon(getResourceIdForResourceName(context, iconName));

        // Set large icon
        String largeIconName = bundle.getString("largeIcon");
        if (largeIconName == null) {
            largeIconName = "ic_launcher";
        }

        int largeIconResId = getResourceIdForResourceName(context, largeIconName);
        if (largeIconResId != 0) {
            try {
                Bitmap largeIconBitmap = BitmapFactory.decodeResource(context.getResources(), largeIconResId);
                notificationBuilder.setLargeIcon(largeIconBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set large icon: " + e.getMessage());
            }
        }

        // Set number badge if provided
        String numberString = bundle.getString("number");
        if (numberString != null) {
            try {
                int numberInt = Integer.parseInt(numberString);
                if (numberInt > 0) {
                    notificationBuilder.setNumber(numberInt);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format: " + numberString);
            }
        }

        // Set progress bar if enabled
        Boolean progress = bundle.getBoolean("progressBar");
        if (progress) {
            double max = bundle.getDouble("progressBarMax");
            double curr = bundle.getDouble("progressBarCurr");
            notificationBuilder.setProgress((int)max, (int)curr, false);
        }

        // Prevent duplicate sound/vibration when updating
        notificationBuilder.setOnlyAlertOnce(true);

        return notificationBuilder.build();
    }

    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null || launchIntent.getComponent() == null) {
            Log.e(TAG, "Failed to get launch intent or component");
            return null;
        }
        try {
            return Class.forName(launchIntent.getComponent().getClassName());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to get main activity class");
            return null;
        }
    }

    private int getResourceIdForResourceName(Context context, String resourceName) {
        int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        if (resourceId == 0) {
            resourceId = context.getResources().getIdentifier(resourceName, "mipmap", context.getPackageName());
        }
        return resourceId;
    }

    private static boolean channelCreated = false;
    private void checkOrCreateChannel(NotificationManager manager, Bundle bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        if (channelCreated)
            return;
        if (manager == null)
            return;

        int importance = NotificationManager.IMPORTANCE_HIGH;
        final String importanceString = bundle.getString("importance");

        if (importanceString != null) {
            switch(importanceString.toLowerCase()) {
                case "default":
                    importance = NotificationManager.IMPORTANCE_DEFAULT;
                    break;
                case "max":
                    importance = NotificationManager.IMPORTANCE_MAX;
                    break;
                case "high":
                    importance = NotificationManager.IMPORTANCE_HIGH;
                    break;
                case "low":
                    importance = NotificationManager.IMPORTANCE_LOW;
                    break;
                case "min":
                    importance = NotificationManager.IMPORTANCE_MIN;
                    break;
                case "none":
                    importance = NotificationManager.IMPORTANCE_NONE;
                    break;
                case "unspecified":
                    importance = NotificationManager.IMPORTANCE_UNSPECIFIED;
                    break;
                default:
                    importance = NotificationManager.IMPORTANCE_HIGH;
            }
        }
        
        NotificationChannel channel = new NotificationChannel(
            NOTIFICATION_CHANNEL_ID, 
            this.config.getChannelName(), 
            importance
        );
        
        channel.setDescription(this.config.getChannelDescription());
        channel.enableLights(true);
        channel.enableVibration(bundle.getBoolean("vibration"));
        channel.setShowBadge(true);

        try {
            manager.createNotificationChannel(channel);
            Log.d("ForegroundService", "Notification built successfully");
        } catch (Exception e) {
            Log.e("ForegroundService", "Failed to build notification", e);
            throw e;
        }
        channelCreated = true;
    }
    
    // Clean up MediaSession when done
    public void cleanup() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }
}