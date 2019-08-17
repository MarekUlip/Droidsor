package com.marekulip.droidsor.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

public final class NotificationsSettingsTable implements BaseColumns {
    public static final String TABLE_NAME = "Notifications_settings";

    public static final String SENSOR_ID = "sensor_id";
    public static final String VALUE_NUMBER = "value_number";
    public static final String IS_DISPLAY_VALUE = "is_display_value";
    public static final String IS_THRESHOLD= "is_threshold";
    public static final String TRESHOLD_VAL = "threshold_val";
    public static final String DATA_NAME = "data_name";



    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"+ TABLE_NAME;

    /**
     * Create table statement for SQLite database.
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    SENSOR_ID + " INTEGER," +
                    VALUE_NUMBER+ " INTEGER," +
                    IS_THRESHOLD+ " INTEGER," +
                    TRESHOLD_VAL+ " FLOAT," +
                    DATA_NAME+ " TEXT," +
                    IS_DISPLAY_VALUE + " INTEGER)";


    private NotificationsSettingsTable(){}
}
