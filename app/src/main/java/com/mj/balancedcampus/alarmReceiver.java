package com.mj.balancedcampus;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * Created by MJ on 12/4/15.
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

        // Put here YOUR code.

        long yourmilliseconds = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date resultdate = new Date(yourmilliseconds);



        String st = String.valueOf(sdf.format(resultdate));

        Toast.makeText(context, st, Toast.LENGTH_LONG).show(); // For example

        wl.release();



        Intent i = new Intent();
        i.setClass(context, com.mj.balancedcampus.calendarActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        /**
        // Initialize credentials and service object.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mCredential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));


        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();


        Intent intent1 = mCredential.newChooseAccountIntent();
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent1);

        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Google Calendar API Android Quickstart")
                .build();

        if (isDeviceOnline(context)) {

            Toast.makeText(context, "i'm here", Toast.LENGTH_LONG).show(); // For example

            Event event = new Event()

                    .setSummary("just a test")
                    .setDescription("testing");
            java.util.Date today = new java.util.Date();

            java.sql.Timestamp timestamp = new java.sql.Timestamp(today.getTime());

            DateTime startDate = getStartDate(timestamp);
            DateTime endDate = getEndDate(timestamp);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDate)
                    .setTimeZone("America/New_York");
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(endDate)
                    .setTimeZone("America/New_York");
            event.setEnd(end);


            String calendarId ="primary";

            try {
                event = mService.events().insert(calendarId, event).execute();
            }catch(IOException e){

            }

        } else {
        }


**/

    }

    private DateTime getStartDate(java.sql.Timestamp timestamp){
        int year = timestamp.getYear();
        int month = timestamp.getMonth();
        int day = timestamp.getDay();
        int hour = timestamp.getHours();

        Date date= new java.util.Date(year, month, day-1, hour, 0);

        return new DateTime(date);
    }
    private DateTime getEndDate(java.sql.Timestamp timestamp){

        int year = timestamp.getYear();
        int month = timestamp.getMonth();
        int day = timestamp.getDay();
        int hour = timestamp.getHours();

        Date date= new java.util.Date(year, month, day-1, hour, 59);

        return new DateTime(date);

    }

    protected boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void SetAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, alarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 10, pi); // Millisec * Second * Minute
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, alarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }



}

