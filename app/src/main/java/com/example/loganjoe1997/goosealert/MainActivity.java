package com.example.loganjoe1997.goosealert;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private Location mLastLocation;
    private Marker mSelf;
    private LocationManager mLocationManager;
    private LocationListener mListener;
    private ArrayList<Nest> mNests;

    private boolean mInDanger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            makeRequest();
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mListener = new LocationListener();
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.1f, mListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        mNests = new ArrayList<>();
    }

    private void makeRequest() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        String requestUrl = "https://api.uwaterloo.ca/v2/resources/goosewatch.json?" +
                "key=" + getString(R.string.api_key);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            mMap.clear();

                            JSONObject responseObject = new JSONObject(response);
                            JSONArray dataArray = responseObject.getJSONArray("data");
                            if (dataArray != null) {
                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject item = dataArray.getJSONObject(i);
                                    mNests.add(new Nest(item));
                                }
                            }
                            Log.d(TAG, "Received api response");

                            // MOCK DATA
                            mNests.add(new Nest(999, new LatLng(43.47315f, -80.54387f), "TEST", null));

                            for (Nest nest : mNests) {
                                mMap.addMarker(new MarkerOptions()
                                        .position(nest.getLatLng())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_goose)));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Response error: " + error.getMessage());
                    }
                });

        requestQueue.add(stringRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(43.47327f, -80.54385f), 15));
    }

    private class LocationListener implements android.location.LocationListener {

        public LocationListener() {}

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude());
            Toast.makeText(MainActivity.this, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            mLastLocation = location;
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            boolean prevInDanger = mInDanger;

            boolean inDanger = false;
            for (Nest nest : mNests) {
                if (distanceFormula(nest.getLatLng(), latLng) < 0.0001) {
                    inDanger = true;
                }
            }

            if (mSelf == null) {
                mSelf = mMap.addMarker(new MarkerOptions().position(latLng));
            } else {
                mSelf.setPosition(latLng);
            }

            mInDanger = inDanger;
            if (mInDanger) {
                Log.d(TAG, "IN DANGER ZONE");
            }
            if (!prevInDanger && mInDanger) {
//                MainActivity.startActivity(GooseService.this);
                Log.d(TAG, "NOTIFICATION");

                Intent launchActivity = new Intent(getApplicationContext(), MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(MainActivity.this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(launchActivity);

                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.goose_sound);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);
                builder.setSmallIcon(R.drawable.ic_goose)
                        .setContentTitle("WARNING")
                        .setContentText("WOW")
                        .setContentIntent(pendingIntent)
                        .setSound(sound)
                        .setAutoCancel(true);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(123, builder.build());

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(new long[] {500, 500, 500, 500, 500, 500}, -1);


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
