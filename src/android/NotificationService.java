package org.apache.cordova.batterynotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import es.wicharge.mobileapp.MainActivity;

import org.json.JSONObject;

public class NotificationService {
    public static final String SHARED_PREFERENCES = "es.wicharge.preferences";
    public static final String NOTIFICATION_SENT_VALUE = "es.wicharge.notification.sent";

    private static final String LOG_TAG = "BatteryNotifService";
    private static NotificationService instance = null;
    private static Context context = null;
    private static SharedPreferences sharedPref = null;

    private NotificationService() {}

    public static NotificationService getInstance(Context aContext) {
        if (instance == null) {
            synchronized(NotificationService.class) {
                instance = new NotificationService();
                context = aContext;
                sharedPref = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            }
        }
        return instance;
    }

    public void sendNotification(int minLevel, String notifMessage) {
        JSONObject data = getDataBatteryInfo();
        if (data != null) {
            int level = -1;
            boolean isPlugged = false;
            try {
                level = (int) data.get("level");
                isPlugged = (boolean) data.get("isPlugged");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error reading JSON data: " + e.getMessage(), e);
                return;
            }
            Log.e(LOG_TAG, "sendNotification isPlugged: " + isPlugged + "  level: " + level   +  "  minLevel: " + minLevel);
            int mNotificationId = 6213;
            Notification notification = getActiveNotification(mNotificationId);
            if (!isPlugged && level <= minLevel) {
                if (!isNotificationSent() && notification == null) {
                    setNotificationSent(true);
                    NotificationManager manager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    String channelId = "task_batt_channel";
                    String channelName = "task_batt_name";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                                NotificationManager.IMPORTANCE_DEFAULT);
                        manager.createNotificationChannel(channel);
                    }


                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(getIcon("ic_launcher"))
                            .setContentTitle("Wicharge")
                            .setContentText(notifMessage);
                    // Set the intent to fire when the user taps on notification.
                    Intent resultIntent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
                    mBuilder.setContentIntent(pendingIntent);
                    // Sets an ID for the notification

                    // It will display the notification in notification bar
                    manager.notify(mNotificationId, mBuilder.build());
                }

            } else {
                if (level > minLevel && notification != null) {
                    NotificationManager manager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(mNotificationId);
                }
                if (level > minLevel && isNotificationSent()) {
                    setNotificationSent(false);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private Notification getActiveNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] barNotifications = notificationManager.getActiveNotifications();
        for(StatusBarNotification notification: barNotifications) {
            if (notification.getId() == notificationId) {
                return notification.getNotification();
            }
        }
        return null;
    }

    private int getIcon(String icon) {
        return getResId(icon);
    }

    public int getResId(String resPath) {
        int resId = getResId(context.getResources(), resPath);

        return resId;
    }

    private int getResId(Resources res, String resPath) {
        String pkgName = getPkgName(res);
        String resName = getBaseName(resPath);
        int resId;

        resId = res.getIdentifier(resName, "mipmap", pkgName);

        if (resId == 0) {
            resId = res.getIdentifier(resName, "drawable", pkgName);
        }

        if (resId == 0) {
            resId = res.getIdentifier(resName, "raw", pkgName);
        }

        return resId;
    }

    private String getBaseName (String resPath) {
        String drawable = resPath;

        if (drawable.contains("/")) {
            drawable = drawable.substring(drawable.lastIndexOf('/') + 1);
        }

        if (resPath.contains(".")) {
            drawable = drawable.substring(0, drawable.lastIndexOf('.'));
        }

        return drawable;
    }

    private String getPkgName (Resources res) {
        return res == Resources.getSystem() ? "android" : context.getPackageName();
    }

    private JSONObject getDataBatteryInfo() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.registerReceiver(null, iFilter);
        JSONObject info = new JSONObject();
        try {
            info.put("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0));
            info.put("isPlugged",
                    batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true : false);
            info.put("status", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
        }
        return info;
    }

    public void setNotificationSent(boolean value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(NOTIFICATION_SENT_VALUE, value);
        editor.apply();
    }

    private boolean isNotificationSent() {
        return sharedPref.getBoolean(NOTIFICATION_SENT_VALUE, false);
    }

}
