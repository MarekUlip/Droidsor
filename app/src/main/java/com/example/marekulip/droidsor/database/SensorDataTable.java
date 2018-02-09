package com.example.marekulip.droidsor.database;

import android.content.ContentResolver;
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
    public static final String SPEED = "speed";
    public static final String ACCURACY = "accuracy";
    public static final String SAMPLE_WEIGHT = "sample_weight";
    public static final String LOG_ID = "log_id";

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;

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
                    ACCURACY + " REAL," +
                    SPEED + " REAL," +
                    SAMPLE_WEIGHT + " INTEGER," +
                    SENSOR_TYPE + " INTEGER," +
                    LOG_ID + " INTEGER)";
    /**
     * Delete table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private SensorDataTable(){}


}
