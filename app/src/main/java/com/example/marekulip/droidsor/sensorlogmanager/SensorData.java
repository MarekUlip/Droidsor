package com.example.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;

import com.example.marekulip.droidsor.database.SensorDataTable;

/**
 * Created by Fredred on 24.08.2017.
 */

public class SensorData {
    public final Point3D values;
    public final long time;
    public double longitude;
    public double latitude;
    public double altitude;
    public SensorData(Point3D v, long t, double longt, double lat,double altitude){
        values = v;
        time = t;
        longitude = longt;
        latitude = lat;
    }

    public SensorData(Point3D v, long t){
        this(v,t,0.0,0.0,0.0);
    }

    public void setLocationData(double longt, double lat, double altit){
        longitude = longt;
        latitude = lat;
        altitude = altit;
    }

    public ContentValues getInsertableFormat(){
        ContentValues cv = new ContentValues();
        cv.put(SensorDataTable.SENSOR_VALUE_X,values.x);
        cv.put(SensorDataTable.SENSOR_VALUE_Y,values.y);
        cv.put(SensorDataTable.SENSOR_VALUE_Z,values.z);
        cv.put(SensorDataTable.TIME_OF_LOG,time);
        cv.put(SensorDataTable.LONGITUDE,longitude);
        cv.put(SensorDataTable.LATITUDE,latitude);
        cv.put(SensorDataTable.ALTITUDE,altitude);
        return cv;
    }

    public ContentValues getInsertableFormat(int sensorType, int logId){
        ContentValues cv = getInsertableFormat();
        cv.put(SensorDataTable.SENSOR_TYPE,sensorType);
        cv.put(SensorDataTable.LOG_ID,logId);
        return cv;
    }

    public static long getTime(){
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");
        sb.append("x: ").append(+values.x).append("y: ").append(values.y).append("z: ").append(values.z);
        return sb.toString();
    }
}
