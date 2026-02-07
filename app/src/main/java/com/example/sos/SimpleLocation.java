package com.example.sos;

import android.os.Parcel;
import android.os.Parcelable;

public class SimpleLocation implements Parcelable {
    public final double latitude;
    public final double longitude;

    public SimpleLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected SimpleLocation(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<SimpleLocation> CREATOR = new Creator<SimpleLocation>() {
        @Override
        public SimpleLocation createFromParcel(Parcel in) {
            return new SimpleLocation(in);
        }

        @Override
        public SimpleLocation[] newArray(int size) {
            return new SimpleLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
