/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.batterynotification;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import org.apache.cordova.LOG;

import java.util.concurrent.ExecutorService;

public class BatteryNotificationService extends Service {
    private static final String LOG_TAG = "BatteryNotificationService";
    private BroadcastReceiver receiver = null;
    private int savedLevel = -1;
    private boolean savedIsPlugged = false;
    private int minLevel;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");
        registerBatteryListener();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(LOG_TAG, "onStartCommand");
        int minLevel;
        if (intent != null && intent.getExtras() != null) {
            minLevel = myIntent.getExtras().getInt("minLevel");
            Log.d(LOG_TAG, "**minLevel read from intent: " + minLevel);
        } else {
            minLevel = 20;
            Log.d(LOG_TAG, "**minLevel NOT read from intent using default value: " + minLevel);
        }
    }

    registerBatteryListener();return START_REDELIVER_INTENT;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        // Stopping the player when service is destroyed
        if (this.receiver != null) {
            try {
                getApplicationContext().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
            }
        }
    }

    private void registerBatteryListener() {
        // We need to listen to power events to update battery status
        if (this.receiver == null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            this.receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    JSONObject data = getBatteryInfo(intent);
                    sendNotification(data);
                }
            };
            getApplicationContext().registerReceiver(this.receiver, intentFilter);
        }
    }

    private void sendNotification(JSONObject data) {
        if (data != null) {
            int level = -1;
            boolean isPlugged = false;
            try {
                level = (int) data.get("level");
                isPlugged = (boolean) data.get("isPlugged");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                return;
            }
            if (!isPlugged && level <= minLevel) {

                if (level != savedLevel || isPlugged != savedIsPlugged) {
                    savedLevel = level;
                    savedIsPlugged = isPlugged;

                    Log.d(LOG_TAG, "******* Enviar notificacion: OK *******");

                    // int KEY_NOTIFICATION_BATTERY = 8361;
                    // String message = "Es una prueba";
                    // LocalNotification localNotification = LocalNotification.getInstance();
                    // if (localNotification == null) {
                    //     localNotification = new LocalNotification();
                    //     FakeCordova fakeCodova = new FakeCordova();
                    //     localNotification.privateInitialize(localNotification.getClass().getName(), fakeCodova, null,
                    //             null);
                    // }

                    // JSONArray toasts = new JSONArray();
                    // JSONObject toast = new JSONObject();
                    // JSONObject meta = new JSONObject();
                    // JSONObject trigger = new JSONObject();
                    // JSONObject progressBar = new JSONObject();
                    // try {
                    //     meta.put("plugin", "codova-plugin-local-notification");
                    //     meta.put("version", "0.9-beta.3");
                    //     toast.put("meta", meta);

                    //     trigger.put("type", "calendar");
                    //     toast.put("trigger", trigger);

                    //     progressBar.put("enabled", false);
                    //     progressBar.put("indeterminate", false);
                    //     progressBar.put("maxValue", 100);
                    //     progressBar.put("value", 0);
                    //     toast.put("progressBar", progressBar);

                    //     toast.put("actions", new JSONArray());
                    //     toast.put("attachments", new JSONArray());
                    //     toast.put("autoClear", false);
                    //     toast.put("clock", true);
                    //     toast.put("defaults", 0);
                    //     toast.put("groupSummary", false);
                    //     toast.put("launch", true);
                    //     toast.put("led", true);
                    //     toast.put("lockscreen", true);
                    //     toast.put("priority", 0);
                    //     toast.put("silent", false);
                    //     toast.put("smallIcon", "res://icon");
                    //     toast.put("sound", true);
                    //     toast.put("sticky", false);
                    //     toast.put("title", "");
                    //     toast.put("vibrate", true);
                    //     toast.put("wakeup", true);

                    //     toast.put("id", KEY_NOTIFICATION_BATTERY);
                    //     toast.put("text", message);

                    //     toasts.put(toast);
                    // } catch (Exception e) {
                    //     LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                    //     return;
                    // }
                    // try {

                    //     // Log.d(LOG_TAG, "******* Enviar notificacion: OK *******");
                    //     localNotification.schedule(toasts, null);
                    // } catch (Exception e) {
                    //     LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                    // }
                }
            }
        }
    }

    private JSONObject getBatteryInfo(Intent batteryIntent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0));
            obj.put("isPlugged",
                    batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true : false);
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }

    class FakeCordova implements CordovaInterface {

        @Override
        public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {

        }

        @Override
        public void setActivityResultCallback(CordovaPlugin plugin) {

        }

        @Override
        public Activity getActivity() {
            return null;
        }

        @Override
        public Context getContext() {
            return getApplicationContext();
        }

        @Override
        public Object onMessage(String id, Object data) {
            return null;
        }

        @Override
        public ExecutorService getThreadPool() {
            return null;
        }

        @Override
        public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {

        }

        @Override
        public void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) {

        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }
    }
}