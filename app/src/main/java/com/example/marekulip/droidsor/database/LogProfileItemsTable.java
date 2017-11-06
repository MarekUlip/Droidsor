package com.example.marekulip.droidsor.database;

import android.provider.BaseColumns;

/**
 * Created by Fredred on 20.10.2017.
 */

public class LogProfileItemsTable implements BaseColumns{
    public static final String TABLE_NAME = "Log_profile_items";

    public static final String SENSOR_TYPE = "sensor_type";
    public static final String PROFILE_ID = "profile_id";
    public static final String SCAN_PERIOD = "scan_period";


    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    SCAN_PERIOD + " INTEGER," +
                    SENSOR_TYPE + " INTEGER," +
                    PROFILE_ID + " INTEGER)";
    /**
     * Create table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private LogProfileItemsTable(){}

}
