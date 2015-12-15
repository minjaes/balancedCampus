package com.mj.balancedcampus;

/**
 * Created by MJ on 11/30/15.
 */

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.services.calendar.CalendarScopes;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import android.text.method.ScrollingMovementMethod;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.*;

/**
 * This is the Main class that runs starts the alarmservice.
 *
 */
public class MainActivity extends Activity {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };


    private PendingIntent pendingIntent;
    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        activityLayout.addView(mOutputText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        setContentView(activityLayout);


        Intent alarmIntent = new Intent(MainActivity.this, alarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this,0,alarmIntent,0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long interval = AlarmManager.INTERVAL_HOUR;

        TimeZone eastern = TimeZone.getTimeZone("US/Eastern");

        java.util.Calendar input = java.util.Calendar.getInstance(eastern);

        int hour = input.get(java.util.Calendar.HOUR);
        int min = input.get(java.util.Calendar.MINUTE);

        if( min > 50){
            hour ++;
        }

        //it sets up the the alarmservice to start 50 min of next hour and
        //repeat hourly.

        input.set(java.util.Calendar.HOUR_OF_DAY, hour);
        input.set(java.util.Calendar.MINUTE, 50);
        input.set(java.util.Calendar.SECOND, 00);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, input.getTimeInMillis(), interval, pendingIntent);
    }
}