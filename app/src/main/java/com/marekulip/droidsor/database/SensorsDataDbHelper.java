package com.marekulip.droidsor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database helper for Droidsor database.
 * Created by Marek Ulip on 21.08.2017.
 */

public class SensorsDataDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "SensorsData.db";

    public SensorsDataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SensorLogsTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(SensorDataTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogProfilesTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogProfileItemsTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(SenorDataItemsCountTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(NotificationsSettingsTable.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if( oldVersion < 7 && newVersion == 7 ){
            sqLiteDatabase.execSQL(NotificationsSettingsTable.CREATE_TABLE);
        }
        //onCreate(sqLiteDatabase);
    }
}
