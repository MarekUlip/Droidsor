package com.example.marekulip.droidsor.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Contains information about each user started sensor data logging.
 * Columns are id of log, date of start, date of end
 */

public final class SensorLogsTable implements BaseColumns{

    public static final String TABLE_NAME = "Logs";

    public static final String DATE_OF_START = "date_of_start";
    public static final String DATE_OF_END = "date_of_end";
    public static final String LOG_NAME = "log_name";

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + SensorLogsTable.TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    LOG_NAME + " TEXT," +
                    DATE_OF_START + " INTEGER," +
                    DATE_OF_END + " INTEGER)";
    /**
     * Create table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + SensorLogsTable.TABLE_NAME;

    private SensorLogsTable(){}

}
