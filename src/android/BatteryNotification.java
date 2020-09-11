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

import org.apache.cordova.batterynotification.BatteryNotificationService;

public class BatteryNotification extends CordovaPlugin {

    private int savedLevel = -1;
    private boolean savedIsPlugged = false;
    private int minLevel = 20;

    private static final String LOG_TAG = "BatteryNotification";

    BroadcastReceiver receiver;

    private CallbackContext batteryCallbackContext = null;

    /**
     * Constructor.
     */
    public BatteryListener() {
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
            Intent mIntent = new Intent(webView.getContext());
            mIntent.putExtra("minLevel", minLevel);
            webView.getContext().startService(mIntent, BatteryNotificationService.class);

            // Don't return any result
            callbackContext.success();
            return true;
        }

        else if (action.equals("stopService")) {
            Intent mIntent = new Intent(webView.getContext());
            webView.getContext().stopService(mIntent, BatteryNotificationService.class);

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
                webView.getContext().registerReceiver(this.receiver, intentFilter);
            }
        }

        else if (action.equals("stop")) {
            removeBatteryListener();
            this.sendUpdate(new JSONObject(), false); // release status callback in JS side
            this.batteryCallbackContext = null;
            callbackContext.success();
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
        JSONObject data = this.getBatteryInfo(batteryIntent);
        sendUpdate(data, true);
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

    private void sendNotification(JSONObject data) {
        if (data != null) {
            int level = -1;
            boolean isPlugged = false;
            try {
                level = (int) data.get("level");
                isPlugged = (boolean) data.get("isPlugged");
            } catch (Exception e) {
                LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                return;
            }

            if (level != savedLevel || isPlugged != savedIsPlugged) {
                savedLevel = level;
                savedIsPlugged = isPlugged;

                int KEY_NOTIFICATION_BATTERY = 8361;
                String message = "Es una prueba";
                LocalNotification localNotification = LocalNotification.getInstance();
                JSONArray toasts = new JSONArray();
                JSONObject toast = new JSONObject();
                JSONObject meta = new JSONObject();
                JSONObject trigger = new JSONObject();
                JSONObject progressBar = new JSONObject();
                try {
                    meta.put("plugin", "codova-plugin-local-notification");
                    meta.put("version", "0.9-beta.3");
                    toast.put("meta", meta);

                    trigger.put("type", "calendar");
                    toast.put("trigger", trigger);

                    progressBar.put("enabled", false);
                    progressBar.put("indeterminate", false);
                    progressBar.put("maxValue", 100);
                    progressBar.put("value", 0);
                    toast.put("progressBar", progressBar);

                    toast.put("actions", new JSONArray());
                    toast.put("attachments", new JSONArray());
                    toast.put("autoClear", false);
                    toast.put("clock", true);
                    toast.put("defaults", 0);
                    toast.put("groupSummary", false);
                    toast.put("launch", true);
                    toast.put("led", true);
                    toast.put("lockscreen", true);
                    toast.put("priority", 0);
                    toast.put("silent", false);
                    toast.put("smallIcon", "res://icon");
                    toast.put("sound", true);
                    toast.put("sticky", false);
                    toast.put("title", "");
                    toast.put("vibrate", true);
                    toast.put("wakeup", true);

                    toast.put("id", KEY_NOTIFICATION_BATTERY);
                    toast.put("text", message);

                    toasts.put(toast);
                } catch (Exception e) {
                    LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                    return;
                }
                try {

                    Log.d(LOG_TAG, "******* Enviar notificacion: check level: " + level + " *******");
                    if (!isPlugged && level <= 84) {
                        Log.d(LOG_TAG, "******* Enviar notificacion: OK *******");
                        localNotification.schedule(toasts, this.batteryCallbackContext);
                    } else {
                        Log.d(LOG_TAG, "******* Enviar notificacion: NO se cumplen condiciones *******");
                    }
                } catch (Exception e) {
                    LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
                }
            }
        }
    }

}
