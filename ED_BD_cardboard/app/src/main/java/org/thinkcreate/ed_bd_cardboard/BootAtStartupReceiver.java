package org.thinkcreate.ed_bd_cardboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by sundeqing on 6/11/15.
 */
public class BootAtStartupReceiver extends BroadcastReceiver {
    static final String TAG = "BootAtStartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "*** onReceive ACTION_BOOT_COMPLETED");

            Intent intent2 = new Intent(context,MainActivity.class);
			intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent2);

        }

        Log.d(TAG, "*** onReceive");
    }

}