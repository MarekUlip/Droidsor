package com.marekulip.droidsor.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Class representing table with count of sensor items in log. This table is used for faster load of log summary.
 * Created by Marek Ulip on 28-Jan-18.
 */

public final class SenorDataItemsCountTable implements BaseColumns {
    public static final String TABLE_NAME = "Sensor_data_count";

    public static final String SENSOR_TYPE = "sensor_type";
    public static final String LOG_ID = "log_id";
    public static final String COUNT_OF_ITEMS = "count_of_items";

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    SENSOR_TYPE + " INTEGER," +
                    COUNT_OF_ITEMS + " INTEGER," +
                    LOG_ID + " INTEGER)";
    /**
     * Delete table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private SenorDataItemsCountTable(){}

}
