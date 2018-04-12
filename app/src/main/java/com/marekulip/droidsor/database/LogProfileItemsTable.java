package com.marekulip.droidsor.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Class represents table single items in log profile
 * Created by Marek Ulip on 20.10.2017.
 */

public final class LogProfileItemsTable implements BaseColumns{
    public static final String TABLE_NAME = "Log_profile_items";

    public static final String SENSOR_TYPE = "sensor_type";
    public static final String PROFILE_ID = "profile_id";
    public static final String SCAN_PERIOD = "scan_period";

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;


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
