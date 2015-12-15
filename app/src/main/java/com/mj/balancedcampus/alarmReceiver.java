package com.mj.balancedcampus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.calendar.CalendarScopes;

/**
 * Created by MJ on 12/4/15.
 */

/**
 *  recevier to initiate the calendarActivity class
 */
public class alarmReceiver extends BroadcastReceiver {
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private com.google.api.services.calendar.Calendar mService = null;



    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        wl.release();

        Intent i = new Intent();
        i.setClass(context, com.mj.balancedcampus.calendarActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

    }

}

