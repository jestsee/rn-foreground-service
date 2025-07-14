package com.supersami.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import static com.supersami.foregroundservice.Constants.ERROR_INVALID_CONFIG;
import static com.supersami.foregroundservice.Constants.ERROR_SERVICE_ERROR;
import static com.supersami.foregroundservice.Constants.NOTIFICATION_CONFIG;
import static com.supersami.foregroundservice.Constants.TASK_CONFIG;

public class ForegroundServiceModule extends ReactContextBaseJavaModule {

    class ForegroundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ForegroundService", "msk kah " + intent.getAction());
            foregroundEmitter(intent);
        }
    }

    private final ReactApplicationContext reactContext;
    private ForegroundReceiver foregroundReceiver = new ForegroundReceiver();

    public ForegroundServiceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        
        // Register receiver immediately when module is created
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.supersami.foregroundservice.BUTTON_ACTION");
        try {
            reactContext.registerReceiver(foregroundReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            Log.d("ForegroundService", "Receiver registered successfully");
        } catch (Exception e) {
            Log.e("ForegroundService", "Failed to register receiver: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return "ForegroundService";
    }

    private boolean isRunning() {
        // Get the ForegroundService running value
        ForegroundService instance = ForegroundService.getInstance();
        int res = 0;
        if (instance != null) {
            res = instance.isRunning();
        }
        return res > 0;
    }

    @ReactMethod
    public void startService(ReadableMap notificationConfig, Promise promise) {
        if (notificationConfig == null) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: Notification config is invalid");
            return;
        }

        if (!notificationConfig.hasKey("id")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: id is required");
            return;
        }

        if (!notificationConfig.hasKey("title")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: title is reqired");
            return;
        }

        if (!notificationConfig.hasKey("message")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: message is required");
            return;
        }

        if (!notificationConfig.hasKey("ServiceType")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: ServiceType is required");
            return;
        }

        try {
            Intent intent = new Intent(getReactApplicationContext(), ForegroundService.class);
            intent.setAction(Constants.ACTION_FOREGROUND_SERVICE_START);
            intent.putExtra(NOTIFICATION_CONFIG, Arguments.toBundle(notificationConfig));
            ForegroundService.setReactContext(getReactApplicationContext());
            ComponentName componentName = getReactApplicationContext().startService(intent);

            if (componentName != null) {
                promise.resolve(null);
            } else {
                promise.reject(ERROR_SERVICE_ERROR, "ForegroundService: Foreground service failed to start.");
            }
        } catch (IllegalStateException e) {
            promise.reject(ERROR_SERVICE_ERROR, "ForegroundService: Foreground service failed to start.");
        }
    }

    @ReactMethod
    public void updateNotification(ReadableMap notificationConfig, Promise promise) {
        if (notificationConfig == null) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: Notification config is invalid");
            return;
        }

        if (!notificationConfig.hasKey("id")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: id is required");
            return;
        }

        if (!notificationConfig.hasKey("title")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: title is reqired");
            return;
        }

        if (!notificationConfig.hasKey("message")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: message is required");
            return;
        }

        try {

            Intent intent = new Intent(getReactApplicationContext(), ForegroundService.class);
            intent.setAction(Constants.ACTION_UPDATE_NOTIFICATION);
            intent.putExtra(NOTIFICATION_CONFIG, Arguments.toBundle(notificationConfig));
            ForegroundService.setReactContext(getReactApplicationContext());
            ComponentName componentName = getReactApplicationContext().startService(intent);

            if (componentName != null) {
                promise.resolve(null);
            } else {
                promise.reject(ERROR_SERVICE_ERROR, "Update notification failed.");
            }
        } catch (IllegalStateException e) {
            promise.reject(ERROR_SERVICE_ERROR, "Update notification failed, service failed to start.");
        }
    }

    // helper to dismiss a notification. Useful if we used multiple notifications
    // for our service since stopping the foreground service will only dismiss one notification
    @ReactMethod
    public void cancelNotification(ReadableMap notificationConfig, Promise promise) {
        if (notificationConfig == null) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: Notification config is invalid");
            return;
        }

        if (!notificationConfig.hasKey("id")) {
            promise.reject(ERROR_INVALID_CONFIG, "ForegroundService: id is required");
            return;
        }

        try {
            int id = (int) notificationConfig.getDouble("id");

            NotificationManager mNotificationManager = (NotificationManager) this.reactContext.getSystemService(this.reactContext.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(id);

            promise.resolve(null);
        } catch (Exception e) {
            promise.reject(ERROR_SERVICE_ERROR, "Failed to cancel notification.");
        }
    }

    @ReactMethod
    public void stopService(Promise promise) {

        // stop main service
        Intent intent = new Intent(getReactApplicationContext(), ForegroundService.class);
        intent.setAction(Constants.ACTION_FOREGROUND_SERVICE_STOP);

        //getReactApplicationContext().stopService(intent);
        // Looks odd, but we do indeed send the stop flag with a start command
        // if it fails, use the violent stop service instead
        try {
            getReactApplicationContext().startService(intent);
        } catch (IllegalStateException e) {
            try {
                getReactApplicationContext().stopService(intent);
            } catch (Exception e2) {
                promise.reject(ERROR_SERVICE_ERROR, "Service stop failed: " + e2.getMessage());
                return;
            }
        }

        // Also stop headless tasks, should be noop if it's not running.
        // TODO: Not working, headless task must finish regardless. We have to rely on JS code being well done.
        // intent = new Intent(getReactApplicationContext(), ForegroundServiceTask.class);
        // getReactApplicationContext().stopService(intent);
        promise.resolve(null);
    }

    @ReactMethod
    public void stopServiceAll(Promise promise) {

        // stop main service with all action
        Intent intent = new Intent(getReactApplicationContext(), ForegroundService.class);
        intent.setAction(Constants.ACTION_FOREGROUND_SERVICE_STOP_ALL);

        try {
            getReactApplicationContext().startService(intent);
        } catch (IllegalStateException e) {
            try {
                getReactApplicationContext().stopService(intent);
            } catch (Exception e2) {
                promise.reject(ERROR_SERVICE_ERROR, "Service stop all failed: " + e2.getMessage());
                return;
            }
        }

        promise.resolve(null);
    }

    @ReactMethod
    public void runTask(ReadableMap taskConfig, Promise promise) {

        if (!taskConfig.hasKey("taskName")) {
            promise.reject(ERROR_INVALID_CONFIG, "taskName is required");
            return;
        }

        if (!taskConfig.hasKey("delay")) {
            promise.reject(ERROR_INVALID_CONFIG, "delay is required");
            return;
        }

        try {

            Intent intent = new Intent(getReactApplicationContext(), ForegroundService.class);
            intent.setAction(Constants.ACTION_FOREGROUND_RUN_TASK);
            intent.putExtra(TASK_CONFIG, Arguments.toBundle(taskConfig));

            ComponentName componentName = getReactApplicationContext().startService(intent);

            if (componentName != null) {
                promise.resolve(null);
            } else {
                promise.reject(ERROR_SERVICE_ERROR, "Failed to run task: Service did not start");
            }
        } catch (IllegalStateException e) {
            promise.reject(ERROR_SERVICE_ERROR, "Failed to run task: Service did not start");
        }
    }

    @ReactMethod
    public void isRunning(Promise promise) {

        // Get the ForegroundService running value
        ForegroundService instance = ForegroundService.getInstance();
        int res = 0;
        if (instance != null) {
            res = instance.isRunning();
        }

        promise.resolve(res);
    }

    public  void  foregroundEmitter(Intent intent){
    // this method is to send back data from java to javascript so one can easily
    // know which button from notification or the notification button is clicked
    String  main = intent.getStringExtra("mainOnPress");
    String  btn = intent.getStringExtra("buttonOnPress");
    String  btn2 = intent.getStringExtra("button2OnPress");
    String  btn3 = intent.getStringExtra("button3OnPress");
    WritableMap  map = Arguments.createMap();
    if (main != null) {
        map.putString("main", main);
    }
    if (btn != null) {
        map.putString("button", btn);
    }
    if (btn2 != null) {
        map.putString("button2", btn2);
    }
    if (btn3 != null) {
        map.putString("button3", btn3);
    }
    try {
        getReactApplicationContext()
        // .getReactInstanceManager().getCurrentReactContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit("notificationClickHandle", map);
    } catch (Exception  e) {
    Log.e("ForegroundService", "Caught Exception: " + e.getMessage());
    }
  }

  @Override
  public void onCatalystInstanceDestroy() {
      super.onCatalystInstanceDestroy();
      try {
          getReactApplicationContext().unregisterReceiver(foregroundReceiver);
          Log.d("ForegroundService", "Receiver unregistered");
      } catch (Exception e) {
          Log.e("ForegroundService", "Error unregistering receiver: " + e.getMessage());
      }
  }
}
