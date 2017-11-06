package com.example.marekulip.droidsor.database;

import android.provider.BaseColumns;

/**
 * Created by Fredred on 20.10.2017.
 */

public class LogProfilesTable  implements BaseColumns{

    public static final String TABLE_NAME = "Log_profiles";

    public static final String PROFILE_NAME = "profile_name";
    /**
     * GPS location
     */
    public static final String SAVE_LOCATION = "save_location";

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    PROFILE_NAME + " TEXT," +
                    SAVE_LOCATION + " INTEGER)";
    /**
     * Create table statement for SQLite database.
     */
    static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private LogProfilesTable(){}

}
