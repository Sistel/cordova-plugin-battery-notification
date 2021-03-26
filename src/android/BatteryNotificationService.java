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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.util.Log;

public class BatteryNotificationService extends Service {
    private static String SHARED_PREFERENCES = "org.batterynotification.preferences";
    private static String NOTIFICATION_MINLEVEL_VALUE = "org.batterynotification.minlevel";
    private static String NOTIFICATION_NOTIFMESSAGE_VALUE = "org.batterynotification.notifmessage";
    
    private static final String LOG_TAG = "BatteryNotification";
    private BroadcastReceiver receiver = null;
    private int minLevel = -1;
    private String notifMessage = "Wicharge";
    private SharedPreferences sharedPref = null;

    @Override
    public IBinder onBind(Intent intent) {
//        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
//        Log.d(LOG_TAG, "onCreate");
        registerBatteryListener();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        Log.d(LOG_TAG, "onStartCommand");
        if (intent != null && intent.getExtras() != null) {            
            minLevel = intent.getIntExtra("minLevel", 20);
            notifMessage = intent.getStringExtra("message");

            setMinLevel(minLevel);
            setNotifMessage(notifMessage);

            Log.d(LOG_TAG, "onStartCommand getIntExtra minLevel: " + minLevel);
        } else {
            minLevel = getMinLevel();
            notifMessage = getNotifMessage();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        if (this.receiver != null) {
            try {
                getApplicationContext().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
            }
        }
        Intent broadcastIntent = new Intent(this, BatteryNotifRestarterBroadcastReceiver.class);       
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 5000,
            restartServicePendingIntent);

        Intent broadcastIntent = new Intent(this, BatteryNotifRestarterBroadcastReceiver.class);       
        sendBroadcast(broadcastIntent);

        super.onTaskRemoved(rootIntent);
    }

    private void registerBatteryListener() {
        if (this.receiver == null) {

            this.receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
//                Log.d(LOG_TAG, "received battery event");
                    if (minLevel == -1) {
                        minLevel = getMinLevel();
                        notifMessage = getNotifMessage();
                    }    
                    NotificationService.getInstance(getApplicationContext()).sendNotification(minLevel, notifMessage);
                    
                }
            };
            getApplicationContext().registerReceiver(this.receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    private SharedPreferences getSharedPref() {
        if (sharedPref == null) {
            sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        }
        return sharedPref;
    }

    private void setMinLevel(int level) {
        SharedPreferences.Editor editor = getSharedPref().edit();
        editor.putInt(NOTIFICATION_MINLEVEL_VALUE, level);
        editor.apply();
    }

    private void setNotifMessage(String message) {
        SharedPreferences.Editor editor = getSharedPref().edit();
        editor.putString(NOTIFICATION_NOTIFMESSAGE_VALUE, message);
        editor.apply();
    }

    private int getMinLevel() {
        return sharedPref.getInt(NOTIFICATION_MINLEVEL_VALUE, 20);
    }

    private String getNotifMessage() {
        return sharedPref.getString(NOTIFICATION_NOTIFMESSAGE_VALUE, "Batería baja, use wicharge para localizar el punto de carga más cercano");
    }

}