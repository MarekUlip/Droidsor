package com.example.marekulip.droidsor.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Class represents table for log profiles
 * Created by Marek Ulip on 20.10.2017.
 */

public final class LogProfilesTable  implements BaseColumns{

    public static final String TABLE_NAME = "Log_profiles";

    public static final String PROFILE_NAME = "profile_name";
    /**
     * Indicator whether to GPS location or not.
     */
    public static final String SAVE_LOCATION = "save_location";
    public static final String GPS_FREQUENCY = "gps_frequency";

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    PROFILE_NAME + " TEXT," +
                    GPS_FREQUENCY + " INTEGER," +
                    SAVE_LOCATION + " INTEGER)";
    /**
     * Delete table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private LogProfilesTable(){}

}
