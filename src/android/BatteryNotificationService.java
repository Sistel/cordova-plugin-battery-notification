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

import android.util.Log;

public class BatteryNotificationService extends Service {
    private static final String LOG_TAG = "BatteryNotification";
    private BroadcastReceiver receiver = null;
    private int minLevel = -1;
    private String notifMessage = "Wicharge";

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
//        Log.d(LOG_TAG, "onStartCommand");
        if (intent != null && intent.getExtras() != null) {
            minLevel = intent.getIntExtra("minLevel", 22);
            notifMessage = intent.getStringExtra("message");

            Log.d(LOG_TAG, "onStartCommand getIntExtra minLevel: " + minLevel);
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
    }

    private void registerBatteryListener() {
        if (this.receiver == null) {
            this.receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
//                Log.d(LOG_TAG, "received battery event");
                    if (minLevel != -1) {
                        NotificationService.getInstance(getApplicationContext()).sendNotification(minLevel, notifMessage);
                    }
                }
            };
            getApplicationContext().registerReceiver(this.receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }


}