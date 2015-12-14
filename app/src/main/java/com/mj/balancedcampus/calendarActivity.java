package com.mj.balancedcampus;

/**
 * Created by MJ on 11/30/15.
 */
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
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;
import com.google.api.services.calendar.model.Calendar;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.*;

public class calendarActivity extends Activity {
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

        // Initialize credentials and service object.
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

    }


    /**
     * Called whenever this activity is pushed to the foreground, such as after
     * a call to onCreate().
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            mOutputText.setText("Google Play Services required: " +
                    "after installing, close and relaunch this app.");
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    mOutputText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempt to get a set of data from the Google Calendar API to display. If the
     * email address isn't known yet, then call chooseAccount() method so the
     * user can pick an account.
     */
    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                new MakeRequestTask(mCredential).execute();
            } else {
                mOutputText.setText("No network connection available.");
            }
        }
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    protected boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                calendarActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    protected class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                //mOutputText.setText("I'm here");


                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {



            Event event = new Event()

                    .setSummary("just testing")
                    .setDescription("testing");

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.util.Date now = calendar.getTime();
            java.sql.Timestamp last = new java.sql.Timestamp(2015, 12, 8, 15, 0, 0,0);

            DateTime input = new DateTime(now);

            DateTime startDate = getStartDate(last);
            DateTime endDate = getEndDate(last);


            EventDateTime start = new EventDateTime()
                    .setDateTime(input)
                    .setTimeZone("America/New_York");
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(input)
                    .setTimeZone("America/New_York");
            event.setEnd(end);

            String calendarId = "primary";
/**
            try {
                mOutputText.setText("good");
                event = mService.events().insert(calendarId, event).execute();

            }catch(IOException e){
                mOutputText.setText("There is an error");
            }

 **/
            List<String> basic = null;
            //write to Calendar


            //data from band.db

            String [] tableColumns = new String[3];
            tableColumns[0] = bandProvider.Band_Data.TIMESTAMP;
            tableColumns[1] = bandProvider.Band_Data._Value;
            tableColumns[2] = bandProvider.Band_Data.TYPE;



            //get last update

            double lastUpdate = 0;

            String [] arg = new String[1];
            arg[0] = "lastUpdate";
            Uri content_uri = Uri.parse("content://" + "com.aware.plugin.plugin_band.provider.band" + "/band");
            Cursor band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data.moveToLast();
            //band_data.moveToPrevious();
            try{
                lastUpdate = band_data.getDouble(0);
            }catch(Exception e){
                band_data = getContentResolver().query(content_uri, tableColumns, null, null, null);
                band_data.moveToFirst();
                lastUpdate = band_data.getDouble(0);
            }

            //Calories
            arg[0] = "calories";
            band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data=updateOld(band_data, lastUpdate);
            uploadData(band_data,"1");


            //distance
            arg[0] = "distance";
            band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data=updateOld(band_data, lastUpdate);
            uploadData(band_data,"3");

            //skinTemp
            arg[0] = "skinTemp";
            band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data=updateOld(band_data, lastUpdate);
            uploadData(band_data, "4");
/**
            //accelerometer
            arg[0] = "accelerometer";
            band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data=updateOld(band_data, lastUpdate);
            uploadData(band_data,"5");

**/
            //pedometer
            arg[0] = "pedometer";
            band_data = getContentResolver().query(content_uri, tableColumns, "type=?", arg, null);
            band_data=updateOld(band_data, lastUpdate);
            uploadData(band_data,"2");

            ContentValues new_data = new ContentValues();
            new_data.put(bandProvider.Band_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            band_data.moveToPrevious();
            new_data.put(bandProvider.Band_Data.TIMESTAMP, band_data.getDouble(0));
            new_data.put(bandProvider.Band_Data._Value, 0);
            new_data.put(bandProvider.Band_Data.TYPE, "lastUpdate");
            getContentResolver().insert(bandProvider.Band_Data.CONTENT_URI, new_data);

            return basic;
        }

        private Cursor updateOld(Cursor band_data, double lastUpdate){
            band_data.moveToFirst();

            java.sql.Timestamp last = new java.sql.Timestamp((long)lastUpdate);
            int lastyear = last.getYear();
            int lastmonth = last.getMonth();
            int lastday = last.getDay();
            int lasthour = last.getHours();

            int year = 0;
            int month = 0;
            int day = 0;
            int hour = 0;

            while(band_data.isLast()== false  && band_data.getDouble(0)<= lastUpdate){
                band_data.moveToNext();
            }
            java.sql.Timestamp timestamp = new java.sql.Timestamp((long)band_data.getDouble(0));
            year = timestamp.getYear();
            month = timestamp.getMonth();
            day = timestamp.getDay();
            hour = timestamp.getHours();

            while(band_data.isLast()== false && lastyear==year && lastmonth==month &&
                    lastday == day && lasthour==hour){
                band_data.moveToNext();
                timestamp = new java.sql.Timestamp((long)band_data.getDouble(0));
                year = timestamp.getYear();
                month = timestamp.getMonth();
                day = timestamp.getDay();
                hour = timestamp.getHours();

            }
            return band_data;
        }
        private void uploadData(Cursor band_data, String colorId){


            java.sql.Timestamp timestamp = new java.sql.Timestamp((long)band_data.getDouble(0));

            int year = timestamp.getYear();
            int month = timestamp.getMonth();
            int day = timestamp.getDay();
            int hour = timestamp.getHours();

            DateTime startDate = getStartDate(timestamp);
            DateTime endDate = getEndDate(timestamp);

            float sum = band_data.getFloat(1);
            int count = 1;
            while(band_data.moveToNext()) {

                java.sql.Timestamp timestamp1 = new java.sql.Timestamp((long)band_data.getDouble(0));

                int year1 = timestamp1.getYear();
                int month1 = timestamp1.getMonth();
                int day1 = timestamp1.getDay();
                int hour1 = timestamp1.getHours();

                if(year == year1 && month == month1 && day == day1 && hour == hour1){
                    sum += band_data.getFloat(1);
                    count ++;
                }
                if(band_data.isLast() || year != year1 || month != month1 ||
                        day != day1 || hour != hour1) {
                    Event event = new Event()

                            .setSummary(String.valueOf(sum / count))
                            .setDescription(band_data.getString(2));

                    EventDateTime start = new EventDateTime()
                            .setDateTime(startDate)
                            .setTimeZone("America/New_York");
                    event.setStart(start);

                    EventDateTime end = new EventDateTime()
                            .setDateTime(endDate)
                            .setTimeZone("America/New_York");
                    event.setEnd(end);

                    event.setColorId(colorId);

                    String calendarId = getCalendarId("band");

                    try {
                        event = mService.events().insert(calendarId, event).execute();
                    }catch(IOException e){

                    }
                    timestamp = new java.sql.Timestamp((long) band_data.getDouble(0));

                    year = timestamp.getYear();
                    month = timestamp.getMonth();
                    day = timestamp.getDay();
                    hour = timestamp.getHours();

                    startDate = getStartDate(timestamp);
                    endDate = getEndDate(timestamp);
                    sum = band_data.getFloat(1);
                    count = 1;
                }
            }



        }

        private String getCalendarId(String id){

            CalendarList calendarList = null;
            List<CalendarListEntry> list = null;
            String calendarId = null;
            try {
                calendarList = mService.calendarList().list().execute();
            } catch (IOException e) {
            }
            list = calendarList.getItems();
            for (CalendarListEntry item : list) {
                if (item.getSummary().equals(id)) {
                    calendarId = item.getId();
                }
            }
            if (calendarId == null) {
                com.google.api.services.calendar.model.Calendar newCal = new com.google.api.services.calendar.model.Calendar();
                newCal.setSummary(id);
                try {
                    com.google.api.services.calendar.model.Calendar createEntry = mService.calendars().insert(newCal).execute();
                } catch (IOException e) {
                }
            }
            try {
                calendarList = mService.calendarList().list().execute();
            } catch (IOException e) {
            }
            list = calendarList.getItems();
            for (CalendarListEntry item : list) {
                if (item.getSummary().equals(id)) {
                    calendarId = item.getId();
                }
            }
            return calendarId;
        }

        private DateTime getStartDate(java.sql.Timestamp timestamp){
            int year = timestamp.getYear();
            int month = timestamp.getMonth();
            int day = timestamp.getDay()+7;
            int hour = timestamp.getHours();

            Date date= new java.util.Date(year, month, day-1, hour, 0);

            return new DateTime(date);
        }
        private DateTime getEndDate(java.sql.Timestamp timestamp){

            int year = timestamp.getYear();
            int month = timestamp.getMonth();
            int day = timestamp.getDay()+7;
            int hour = timestamp.getHours();

            Date date= new java.util.Date(year, month, day-1, hour, 59);

            return new DateTime(date);

        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}