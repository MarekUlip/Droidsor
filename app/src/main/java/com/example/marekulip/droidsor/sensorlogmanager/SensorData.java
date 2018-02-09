package com.example.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;

import com.example.marekulip.droidsor.database.SensorDataTable;

/**
 * Created by Fredred on 24.08.2017.
 */

public class SensorData {
    public int sensorType;
    public final Point3D values;
    public final long time;
    public double longitude;
    public double latitude;
    public double altitude;
    public float speed;
    public float accuracy;

    public SensorData(int sensorType,Point3D v, long t, double longt, double lat, double altitude,float speed, float accuracy){
        this.sensorType = sensorType;
        values = v;
        time = t;
        longitude = longt;
        latitude = lat;
        this.altitude = altitude;
        this.speed = speed;
        this.accuracy = accuracy;
    }

    public SensorData(int sensorType,Point3D v, long t, double longt, double lat){
        this(sensorType,v,t,longt,lat,-1,-1,-1);
    }

    /*public SensorData(Point3D v, long t){
        this(v,t,0.0,0.0,0.0);
    }*/

    public SensorData(int sensorType, Point3D v, long t){
        this(sensorType,v,t,-1.0,-1.0,-1.0,-1,-1);
    }

    public void setLocationData(double longt, double lat, double altit,float speed, float accuracy){
        longitude = longt;
        latitude = lat;
        altitude = altit;
        this.speed = speed;
        this.accuracy = accuracy;
    }

    public void setLocationData(double longt, double lat){
        this.setLocationData(longt,lat,-1,-1,-1);
    }

    public ContentValues getInsertableFormat(){
        ContentValues cv = new ContentValues();
        cv.put(SensorDataTable.SENSOR_TYPE,sensorType);
        cv.put(SensorDataTable.SENSOR_VALUE_X,values.x);
        cv.put(SensorDataTable.SENSOR_VALUE_Y,values.y);
        cv.put(SensorDataTable.SENSOR_VALUE_Z,values.z);
        cv.put(SensorDataTable.TIME_OF_LOG,time);
        cv.put(SensorDataTable.LONGITUDE,longitude);
        cv.put(SensorDataTable.LATITUDE,latitude);
        cv.put(SensorDataTable.ALTITUDE,altitude);
        cv.put(SensorDataTable.SPEED,speed);
        cv.put(SensorDataTable.ACCURACY,accuracy);
        return cv;
    }

    public ContentValues getInsertableFormat(long logId,int weight){
        ContentValues cv = getInsertableFormat();
        cv.put(SensorDataTable.LOG_ID,logId);
        cv.put(SensorDataTable.SAMPLE_WEIGHT,weight);
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
