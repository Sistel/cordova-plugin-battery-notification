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

import org.json.JSONObject;

public class NotificationService {
    private static String SHARED_PREFERENCES = "org.batterynotification.preferences";
    private static String NOTIFICATION_SENT_VALUE = "org.batterynotification.notification.sent";
    private static String NOTIFICATION_LASTLEVEL_VALUE = "org.batterynotification.lastlevel";

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
                SHARED_PREFERENCES = context.getApplicationContext().getPackageName() + ".preferences";
                NOTIFICATION_SENT_VALUE = context.getApplicationContext().getPackageName() + ".notification.sent";
                sharedPref = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            }
        }
        return instance;
    }

    private void executeNotification(int mNotificationId, String notifMessage) {
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

        Intent resultIntent = new Intent(context, getMainActivityClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
        mBuilder.setContentIntent(pendingIntent);
        // Sets an ID for the notification

        // It will display the notification in notification bar
        manager.notify(mNotificationId, mBuilder.build());        
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
                    executeNotification(mNotificationId,notifMessage);
                } else {
                    if (level > getLastBatteryLevel() && notification == null) {
                        executeNotification(mNotificationId, notifMessage);
                    }
                }
                setLastBatteryLevel(level);

            } else {
                if (notification != null) {
                    NotificationManager manager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(mNotificationId);
                }
                setNotificationSent(false);
                setLastBatteryLevel(level);
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

    private void setLastBatteryLevel(int level) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(NOTIFICATION_LASTLEVEL_VALUE, level);
        editor.apply();

    }

    private int getLastBatteryLevel() {
        return sharedPref.getInt(NOTIFICATION_LASTLEVEL_VALUE, 20);
    }

    private boolean isNotificationSent() {
        return sharedPref.getBoolean(NOTIFICATION_SENT_VALUE, false);
    }

    private Class getMainActivityClass() {
        Class mainActivity = null;
        Context context = this.context.getApplicationContext();
        String  packageName = context.getPackageName();
        Intent  launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String  className = launchIntent.getComponent().getClassName();

        try {
            //loading the Main Activity to not import it in the plugin
            mainActivity = Class.forName(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mainActivity;        
    }

}
