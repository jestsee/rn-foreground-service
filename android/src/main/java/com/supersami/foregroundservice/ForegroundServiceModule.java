package com.supersami.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
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
            foregroundEmitter(intent);
        }
    }

    private final ReactApplicationContext reactContext;
    private ForegroundReceiver foregroundReceiver = new ForegroundReceiver();
    private final OkHttpClient client;
    private static final String TAG = "BackgroundFetcher";

    public ForegroundServiceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        client = new OkHttpClient();
        
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

    private void sendEvent(String eventName, String data) {
        WritableMap params = Arguments.createMap();
        params.putString("data", data);
        try {
            getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send event: " + e.getMessage());
        }
    }

    @ReactMethod
    public void fetchData(String url, @Nullable ReadableMap options, Promise promise) {
        Log.d(TAG, "Fetching: " + url);

        if (options == null) {
            options = Arguments.createMap(); // empty map
        }

        // Default to GET
        String method = options.hasKey("method") ? options.getString("method").toUpperCase() : "GET";

        // Headers
        Headers.Builder headersBuilder = new Headers.Builder();
        if (options.hasKey("headers")) {
            ReadableMap headersMap = options.getMap("headers");
            ReadableMapKeySetIterator iter = headersMap.keySetIterator();
            while (iter.hasNextKey()) {
                String key = iter.nextKey();
                String value = headersMap.getString(key);
                headersBuilder.add(key, value);
            }
        }

        // Body
        RequestBody body = null;
        if (options.hasKey("body")) {
            body = RequestBody.create(
                options.getString("body"),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
            );
        }

        Request request = new Request.Builder()
                .url(url)
                .method(method, body)
                .headers(headersBuilder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Fetch failed: " + e.getMessage());
                sendEvent("BackgroundFetcher:error", e.getMessage());
                promise.reject("FETCH_ERROR", e.getMessage()); // JS Promise reject
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response body: " + responseBody);

                    // Create JS-like Response
                    Headers responseHeaders = response.headers();
                    WritableMap headersMap = Arguments.createMap();
                    for (String name : responseHeaders.names()) {
                        headersMap.putString(name, responseHeaders.get(name));
                    }
                    WritableMap result = Arguments.createMap();
                    result.putBoolean("ok", response.isSuccessful());
                    result.putInt("status", response.code());
                    result.putString("statusText", response.message());
                    result.putString("body", responseBody);
                    result.putMap("headers", headersMap);

                    promise.resolve(result);

                    // Emit event for background listeners
                    sendEvent("BackgroundFetcher:success", responseBody);
                } catch (IOException e) {
                    sendEvent("BackgroundFetcher:error", e.getMessage());
                    promise.reject("FETCH_ERROR", e.getMessage());
                }
            }
        });
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

    @ReactMethod
    public void updateMediaDisplayState(String state, Promise promise) {
        try {
            ForegroundService serviceInstance = ForegroundService.getInstance();
            if (serviceInstance != null) {
                serviceInstance.updatePlaybackStateForDisplay(state);
                promise.resolve("Display state updated to: " + state);
            } else {
                promise.reject("ERROR", "ForegroundService not running");
            }
        } catch (Exception e) {
            promise.reject("ERROR", "Failed to update display state: " + e.getMessage());
        }
    }

    public  void  foregroundEmitter(Intent intent){
    // this method is to send back data from java to javascript so one can easily
    // know which button from notification or the notification button is clicked
    String action = intent.getStringExtra("action");
    String  main = intent.getStringExtra("mainOnPress");
    String  btn = intent.getStringExtra("buttonOnPress");
    String  btn2 = intent.getStringExtra("button2OnPress");
    String  btn3 = intent.getStringExtra("button3OnPress");

    Log.d("ForegroundService", "Button pressed - action: " + action + ", main=" + main + ", button=" + btn + ", button2=" + btn2 + ", button3=" + btn3);
    
    WritableMap  map = Arguments.createMap();
    
    // Use the action to determine which button was pressed more reliably
    if ("button1".equals(action) && btn != null) {
        map.putString("event", btn);
    } else if ("button2".equals(action) && btn2 != null) {
        map.putString("event", btn2);
    } else if ("button3".equals(action) && btn3 != null) {
        map.putString("event", btn3);
    } else if (main != null) {
        // Main notification press (no action extra)
        map.putString("event", main);
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
