package com.example.loganjoe1997.goosealert;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class GooseService extends Service {

    public static final String TAG = "GooseService";

    public static final String ACTION_LOCATION_CHANGED = "action_location_changed";

    private static boolean sIsRunning;

    private LocationManager mLocationManager;
    private LocationListener mListener;
    private LocalBroadcastManager mBroadcaster;
    private Location mLastLocation;

    private List<Nest> mNests;
    private boolean mInDanger;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE STARTED");

        sIsRunning = true;

        mNests = intent.getParcelableArrayListExtra("nests");
        mInDanger = false;

        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mListener = new LocationListener();
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.1f, mListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        mBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sIsRunning = false;

        Log.d(TAG, "SERVICE ENDED");
        try {
            mLocationManager.removeUpdates(mListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static boolean getIsRunning() {
        return sIsRunning;
    }

    private class LocationListener implements android.location.LocationListener {

        public LocationListener() {}

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude());
            Toast.makeText(GooseService.this, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            mLastLocation = location;

            Intent intent = new Intent(ACTION_LOCATION_CHANGED);
            intent.putExtra("latitude", mLastLocation.getLatitude());
            intent.putExtra("longitude", mLastLocation.getLongitude());
            mBroadcaster.sendBroadcast(intent);

            boolean prevInDanger = mInDanger;

            boolean inDanger = false;
            for (Nest nest : mNests) {
                if (distanceFormula(nest.getLatLng(),
                        new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())) < 0.0001) {
                    inDanger = true;
                }
            }
            mInDanger = inDanger;
            if (mInDanger) {
                Log.d(TAG, "IN DANGER ZONE");
            }
            if (!prevInDanger && mInDanger) {
//                MainActivity.startActivity(GooseService.this);
                Log.d(TAG, "NOTIFICATION");

                Intent launchActivity = new Intent(getApplicationContext(), MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(GooseService.this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(launchActivity);

                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(GooseService.this);
                builder.setSmallIcon(R.drawable.ic_play_light)
                        .setContentTitle("WARNING")
                        .setContentText("WOW")
                        .setContentIntent(pendingIntent);

                NotificationManager notificationManager =
                        (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(123, builder.build());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private double distanceFormula(LatLng a, LatLng b) {
        return Math.sqrt(sqr(a.latitude - b.latitude) + sqr(a.longitude - b.longitude));
    }

    private double sqr(double a) {
        return a * a;
    }
}
