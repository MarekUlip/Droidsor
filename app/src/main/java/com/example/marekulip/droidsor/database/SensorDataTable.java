package com.example.marekulip.droidsor.database;

import android.provider.BaseColumns;

/**
 * Contains sensor's data from each user started log. Each log can contain data from multiple sensors.
 * Columns are id, time of logging(only hours, dates are in log table), sensor value, sensor type, associated log id,
 */

public final class SensorDataTable implements BaseColumns{

    public static final String TABLE_NAME = "Sensor_data";

    public static final String TIME_OF_LOG = "time_of_log";
    public static final String SENSOR_VALUE_X = "sensor_value_x";
    public static final String SENSOR_VALUE_Y = "sensor_value_y";
    public static final String SENSOR_VALUE_Z = "sensor_value_z";
    public static final String SENSOR_TYPE = "sensor_type";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String ALTITUDE = "altitude";
    public static final String LOG_ID = "log_id";

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    TIME_OF_LOG + " INTEGER," +
                    SENSOR_VALUE_X + " REAL," +
                    SENSOR_VALUE_Y + " REAL," +
                    SENSOR_VALUE_Z + " REAL," +
                    LONGITUDE + " REAL," +
                    LATITUDE + " REAL," +
                    ALTITUDE + " REAL," +
                    SENSOR_TYPE + " INTEGER," +
                    LOG_ID + " INTEGER)";
    /**
     * Create table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private SensorDataTable(){}


}
