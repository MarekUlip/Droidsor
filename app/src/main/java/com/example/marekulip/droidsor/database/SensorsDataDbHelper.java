package com.example.marekulip.droidsor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Fredred on 21.08.2017.
 */

public class SensorsDataDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "SensorsData.db";
    private static SensorsDataDbHelper mHelper = null;


    private SensorsDataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);//TODO make settings to external SD
    }

    public static SensorsDataDbHelper getInstance(Context context){
        if(mHelper != null) return mHelper;
        else return mHelper = new SensorsDataDbHelper(context);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SensorLogsTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(SensorDataTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogProfilesTable.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogProfileItemsTable.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //TODO make sure that data are transferred on upgrade
        sqLiteDatabase.execSQL(SensorLogsTable.DELETE_TABLE);
        sqLiteDatabase.execSQL(SensorDataTable.DELETE_TABLE);
        sqLiteDatabase.execSQL(LogProfilesTable.DELETE_TABLE);
        sqLiteDatabase.execSQL(LogProfileItemsTable.DELETE_TABLE);
        onCreate(sqLiteDatabase);
    }
}
