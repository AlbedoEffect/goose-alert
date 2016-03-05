package com.example.loganjoe1997.goosealert;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

public class Nest implements Parcelable {

    private int mId;
    private LatLng mLatLng;
    private String mLocationDescription;
    private String mUpdated;

    public Nest(int id, LatLng latLng, String locationDescription, String updated) {
        mId = id;
        mLatLng = latLng;
        mLocationDescription = locationDescription;
        mUpdated = updated;
    }

    public Nest(JSONObject json) {
        try {
            mId = json.getInt("id");
            mLatLng = new LatLng(json.getDouble("latitude"), json.getDouble("longitude"));
            mLocationDescription = json.getString("location");
            mUpdated = json.getString("updated");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected Nest(Parcel in) {
        mId = in.readInt();
        mLatLng = in.readParcelable(LatLng.class.getClassLoader());
        mLocationDescription = in.readString();
        mUpdated = in.readString();
    }

    public static final Creator<Nest> CREATOR = new Creator<Nest>() {
        @Override
        public Nest createFromParcel(Parcel in) {
            return new Nest(in);
        }

        @Override
        public Nest[] newArray(int size) {
            return new Nest[size];
        }
    };

    public int getId() {
        return mId;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public String getLocationDescription() {
        return mLocationDescription;
    }

    public String getUpdated() {
        return mUpdated;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeParcelable(mLatLng, flags);
        dest.writeString(mLocationDescription);
        dest.writeString(mUpdated);
    }
}
