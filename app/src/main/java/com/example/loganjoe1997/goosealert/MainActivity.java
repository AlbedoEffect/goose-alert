package com.example.loganjoe1997.goosealert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private LatLng mLocation;
    private Marker mSelf;
    private BroadcastReceiver mReceiver;
    private ArrayList<Nest> mNests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            makeRequest();
        }

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLocation = new LatLng(intent.getDoubleExtra("latitude", 0f),
                        intent.getDoubleExtra("longitude", 0f));
                if (mSelf == null) {
                    mSelf = mMap.addMarker(new MarkerOptions().position(mLocation));
                } else {
                    mSelf.setPosition(mLocation);
                }
            }
        };

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
                            mNests.add(new Nest(999, new LatLng(43.47320f, -80.54387f), "TEST", null));

                            for (Nest nest : mNests) {
                                mMap.addMarker(new MarkerOptions()
                                        .position(nest.getLatLng())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_goose)));
                            }

                            if (!GooseService.getIsRunning()) {
                                Intent intent = new Intent(MainActivity.this, GooseService.class);
                                intent.putParcelableArrayListExtra("nests", mNests);
                                startService(intent);
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
    protected void onDestroy() {
        super.onDestroy();

//        stopService(new Intent(this, GooseService.class));
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
}
