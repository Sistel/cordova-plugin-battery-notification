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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

// import org.apache.cordova.batterynotification.BatteryNotificationService;

import java.util.concurrent.TimeUnit;

public class BatteryNotification extends CordovaPlugin {

    private int savedLevel = -1;
    private boolean savedIsPlugged = false;
    private int minLevel = 99;
    private String notifMessage = "Wicharge test";

    private static final String LOG_TAG = "BatteryNotification";

    BroadcastReceiver receiver;

    private CallbackContext batteryCallbackContext = null;

    /**
     * Constructor.
     */
    public BatteryNotification() {
        this.receiver = null;
    }

    /**
     * Executes the request.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback context used when calling back into
     *                        JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        if (action.equals("startService")) {
            startService(args);

            // Don't return any result
            callbackContext.success();
            return true;
        }

        else if (action.equals("stopService")) {
            // Intent mIntent = new Intent(webView.getContext(),
            // BatteryNotificationService.class);
            // webView.getContext().stopService(mIntent);
            stopService();
            // Don't return any result
            callbackContext.success();
            return true;
        }

        else if (action.equals("start")) {
            // We need to listen to power events to update battery status
            if (this.receiver == null) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                this.receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateBatteryInfo(intent);
                    }
                };
                webView.getContext().getApplicationContext().registerReceiver(this.receiver, intentFilter);
            }
            // Don't return any result now, since status results will be sent when events
            // come in from broadcast receiver
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        else if (action.equals("stop")) {
            removeBatteryListener();
            this.sendUpdate(new JSONObject(), false); // release status callback in JS side
            this.batteryCallbackContext = null;
            callbackContext.success();
            NotificationService.getInstance(webView.getContext().getApplicationContext()).setNotificationSent(false);
            return true;
        }

        else if (action.equals("isCharging")) {
            callbackContext.success(isCharging() ? 1 : 0);
            return true;
        }

        else if (action.equals("getDataBatteryInfo")) {
            callbackContext.success(getDataBatteryInfo());
            return true;
        }

        return false;
    }

    /**
     * Stop battery receiver.
     */
    public void onDestroy() {
        removeBatteryListener();
    }

    /**
     * Stop battery receiver.
     */
    public void onReset() {
        removeBatteryListener();
    }

    /**
     * Stop the battery receiver and set it to null.
     */
    private void removeBatteryListener() {
        if (this.receiver != null) {
            try {
                webView.getContext().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a JSONObject with the current battery information
     *
     * @param batteryIntent the current battery information
     * @return a JSONObject containing the battery status information
     */
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

    /**
     * Updates the JavaScript side whenever the battery changes
     *
     * @param batteryIntent the current battery information
     * @return
     */
    private void updateBatteryInfo(Intent batteryIntent) {
        sendUpdate(this.getBatteryInfo(batteryIntent), true);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param connection the network info to set as navigator.connection
     */
    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.batteryCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.batteryCallbackContext.sendPluginResult(result);
        }
    }

    public boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent currentBatteryStatusIntent = webView.getContext().registerReceiver(null, ifilter);
        int batteryStatus = currentBatteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || batteryStatus == BatteryManager.BATTERY_STATUS_FULL
                || batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING;
    }

    public JSONObject getDataBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = webView.getContext().registerReceiver(null, ifilter);
        JSONObject info = new JSONObject();
        try {
            info.put("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0));
            info.put("isPlugged",
                    batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true : false);
            info.put("status", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1));
        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
        }
        return info;
    }

    private void startService(JSONArray args) {
        // Service
        try {
            minLevel = args.getInt(0);
            notifMessage = args.getString(1);
        } catch (JSONException e) {
            LOG.e(LOG_TAG, "Error in startService reading args: " + e.getMessage(), e);
        }
        Context context = webView.getContext().getApplicationContext();

        NotificationService.getInstance(context).setNotificationSent(false);

        Intent mIntent = new Intent(context, BatteryNotificationService.class);
        mIntent.putExtra("minLevel", minLevel);
        mIntent.putExtra("message", notifMessage);
        context.startService(mIntent);

        // WorkManager
        WorkManager mWorkManager = WorkManager.getInstance(context);

        mWorkManager.cancelUniqueWork("check_battery");
        Data data = new Data.Builder().putString("message", notifMessage).putInt("minLevel", minLevel).build();
        PeriodicWorkRequest periodicSyncDataWork = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15,
                TimeUnit.MINUTES).setInputData(data).setInitialDelay(15, TimeUnit.SECONDS).build();
        mWorkManager.enqueueUniquePeriodicWork("check_battery", ExistingPeriodicWorkPolicy.KEEP, // Existing Periodic
                                                                                                 // Work policy
                periodicSyncDataWork // work request
        );
        // Log.e(LOG_TAG, "startService: enqueueUniquePeriodicWork done");
    }

    private void stopService() {
        Context context = webView.getContext().getApplicationContext();

        NotificationService.getInstance(context).setNotificationSent(false);

        // Service
        Intent mIntent = new Intent(context, BatteryNotificationService.class);
        context.stopService(mIntent);

        // WorkManager
        WorkManager mWorkManager = WorkManager.getInstance(context);
        mWorkManager.cancelUniqueWork("check_battery");

    }

}
