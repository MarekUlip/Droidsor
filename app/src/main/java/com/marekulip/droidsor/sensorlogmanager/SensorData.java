package com.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;

import com.marekulip.droidsor.database.SensorDataTable;

/**
 * Class used for keeping sensor data at logical object so it is easier to transfer them between objects
 * Created by Marek Ulip on 24.08.2017.
 */

public class SensorData {
    // Access is public to make access to variable as fast as possible
    /**
     * Sensor type id
     */
    public final int sensorType;
    /**
     * Values of this sensor
     */
    public final Point3D values;
    /**
     * Time when this sensor data were captured
     */
    public final long time;

    /**
     * Indicates whether this data belongs to internal sensor
     */
    public final boolean isInternal;
    /**
     * GPS longitude
     */
    public double longitude;
    /**
     * GPS latitude
     */
    public double latitude;
    /**
     * GPS altitude
     */
    public double altitude;
    /**
     * GPS speed
     */
    public float speed;
    /**
     * GPS accuracy
     */
    public float accuracy;

    /**
     * Constructor
     * @param sensorType
     * @param v values
     * @param t time
     * @param longt longitude
     * @param lat latitude
     * @param altitude altitude
     * @param speed speed
     * @param accuracy accuracy
     * @param isInternal indicates whether this data belongs to internal or external sensor. Default is false.
     */
    public SensorData(int sensorType,Point3D v, long t, double longt, double lat, double altitude,float speed, float accuracy, boolean isInternal){
        this.sensorType = sensorType;
        values = v;
        time = t;
        longitude = longt;
        latitude = lat;
        this.altitude = altitude;
        this.speed = speed;
        this.accuracy = accuracy;
        //Default false because external sensors were untestable due to no BLE sensor to test the app with
        //while internal sensors could be tested an could use this constructor
        this.isInternal = isInternal;
    }

    /**
     * Constructor
     * @param sensorType
     * @param v values
     * @param t time
     * @param longt longitude
     * @param lat latitude
     * @param altitude altitude
     * @param speed speed
     * @param accuracy accuracy
     */
    public SensorData(int sensorType,Point3D v, long t, double longt, double lat, double altitude,float speed, float accuracy){
        this(sensorType,v,t,longt,lat,altitude,speed,accuracy,false);
    }

    /**
     * Creates object of SensorData with altitude speed and accuracy set to -1
     * @param sensorType
     * @param v values
     * @param t time
     * @param longt longitude
     * @param lat latitude
     */
    public SensorData(int sensorType,Point3D v, long t, double longt, double lat){
        this(sensorType,v,t,longt,lat,-1,-1,-1,false);
    }

    /**
     * Creates object of SensorData with longitude, latitude, altitude, speed and accuracy set to -1 and -1.0 respectively
     * @param sensorType
     * @param v values
     * @param t time
     * @param isInternal indicates whether this data belongs to internal or external sensor. Default is false.
     */
    public SensorData(int sensorType, Point3D v, long t, boolean isInternal){
        this(sensorType,v,t,-1.0,-1.0,-1.0,-1,-1,isInternal);
    }

    /**
     * Creates object of SensorData with longitude, latitude, altitude, speed and accuracy set to -1 and -1.0 respectively
     * @param sensorType
     * @param v values
     * @param t time
     */
    public SensorData(int sensorType, Point3D v, long t){
        this(sensorType,v,t,-1.0,-1.0,-1.0,-1,-1,false);
    }

    /**
     * Sets all GPS location data
     * @param longt longitude
     * @param lat latitude
     * @param altit altitude
     * @param speed
     * @param accuracy
     */
    public void setLocationData(double longt, double lat, double altit,float speed, float accuracy){
        longitude = longt;
        latitude = lat;
        altitude = altit;
        this.speed = speed;
        this.accuracy = accuracy;
    }

    /**
     * Sets basic GPS location data
     * @param longt longitude
     * @param lat latitude
     */
    public void setLocationData(double longt, double lat){
        this.setLocationData(longt,lat,-1,-1,-1);
    }



    /**
     * Creates base of ContentValues object which contains all data from this object so it can be
     * prepared for insertion into database. Note that created object should not be inserted into database itself
     * because it misses logid and weight and without this two values this object would be meaningless in database
     * @return ContentValues containing all items of this object
     */
    private ContentValues getInsertableFormat(){
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

    /**
     * Creates ContentValues object which can be inserted into database
     * @param logId id of log to which this sensor data belongs
     * @param weight Indicates significance of this object - if it is only normal item or it is 10th 100th etc object in the log
     * @return insertable ContentValues object
     */
    public ContentValues getInsertableFormat(long logId,int weight){
        ContentValues cv = getInsertableFormat();
        cv.put(SensorDataTable.LOG_ID,logId);
        cv.put(SensorDataTable.SAMPLE_WEIGHT,weight);
        return cv;
    }

    /**
     * Returns system time in milliseconds designed for use in database
     * @return system time in milliseconds
     */
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
