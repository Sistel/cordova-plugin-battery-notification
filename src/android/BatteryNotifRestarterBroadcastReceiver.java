import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryNotifRestarterBroadcastReceiver extends BroadcastReceiver {
 @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(BatteryNotifRestarterBroadcastReceiver.class.getSimpleName(), "BatteryNotificationService Service Stops! Oooooooooooooppppssssss!!!!");
        context.startService(new Intent(context, BatteryNotificationService.class));;
    }
}
