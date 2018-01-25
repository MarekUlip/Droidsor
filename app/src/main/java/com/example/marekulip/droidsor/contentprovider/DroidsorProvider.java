package com.example.marekulip.droidsor.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.example.marekulip.droidsor.database.LogProfileItemsTable;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.database.SensorLogsTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;

/**
 * Created by Marek Ulip on 23-Jan-18.
 */

public class DroidsorProvider extends ContentProvider {

    private SensorsDataDbHelper database;

    private static final int LOG_PROFILE = 1;
    private static final int LOG_PROFILE_ID = 2;
    private static final int LOG_PROFILE_ITEMS = 3;
    private static final int LOG_PROFILE_ITEMS_ID = 4;
    private static final int SENSOR_LOGS = 5;
    private static final int SENSOR_LOGS_ID = 6;
    private static final int SENSOR_DATA = 7;
    private static final int SENSOR_DATA_ID = 8;

    private static final String AUTHORITY = "com.example.marekulip.provider";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final Uri LOG_PROFILE_URI = Uri.parse("content://"+ AUTHORITY+"/"+LogProfilesTable.TABLE_NAME);
    public static final Uri LOG_PROFILE_ITEMS_URI = Uri.parse("content://"+ AUTHORITY+"/"+LogProfileItemsTable.TABLE_NAME);
    public static final Uri SENSOR_LOGS_URI = Uri.parse("content://"+ AUTHORITY+"/"+SensorLogsTable.TABLE_NAME);
    public static final Uri SENSOR_DATA_URI = Uri.parse("content://"+ AUTHORITY+"/"+SensorDataTable.TABLE_NAME);


    static {
        sUriMatcher.addURI(AUTHORITY, LogProfilesTable.TABLE_NAME,LOG_PROFILE);
        sUriMatcher.addURI(AUTHORITY, LogProfilesTable.TABLE_NAME+"/#",LOG_PROFILE_ID);

        sUriMatcher.addURI(AUTHORITY, LogProfileItemsTable.TABLE_NAME,LOG_PROFILE_ITEMS);
        sUriMatcher.addURI(AUTHORITY, LogProfileItemsTable.TABLE_NAME+"/#",LOG_PROFILE_ITEMS_ID);

        sUriMatcher.addURI(AUTHORITY, SensorLogsTable.TABLE_NAME,SENSOR_LOGS);
        sUriMatcher.addURI(AUTHORITY, SensorLogsTable.TABLE_NAME+"/#",SENSOR_LOGS_ID);

        sUriMatcher.addURI(AUTHORITY, SensorDataTable.TABLE_NAME,SENSOR_DATA);
        sUriMatcher.addURI(AUTHORITY, SensorDataTable.TABLE_NAME+"/#",SENSOR_DATA_ID);

    }


    @Override
    public boolean onCreate() {
        database = new SensorsDataDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)){
            case LOG_PROFILE:
                queryBuilder.setTables(LogProfilesTable.TABLE_NAME);
                break;
            case LOG_PROFILE_ID:
                queryBuilder.setTables(LogProfilesTable.TABLE_NAME);
                queryBuilder.appendWhere(LogProfilesTable._ID + " = " + uri.getLastPathSegment());
                break;
            case LOG_PROFILE_ITEMS:
                queryBuilder.setTables(LogProfileItemsTable.TABLE_NAME);
                break;
            case LOG_PROFILE_ITEMS_ID:
                queryBuilder.setTables(LogProfileItemsTable.TABLE_NAME);
                queryBuilder.appendWhere(LogProfileItemsTable._ID + " = " + uri.getLastPathSegment());
                break;
            case SENSOR_LOGS:
                queryBuilder.setTables(SensorLogsTable.TABLE_NAME);
                break;
            case SENSOR_LOGS_ID:
                queryBuilder.setTables(SensorLogsTable.TABLE_NAME);
                queryBuilder.appendWhere(SensorLogsTable._ID + " = " + uri.getLastPathSegment());
                break;
            case SENSOR_DATA:
                queryBuilder.setTables(SensorDataTable.TABLE_NAME);
                break;
            case SENSOR_DATA_ID:
                queryBuilder.setTables(SensorDataTable.TABLE_NAME);
                queryBuilder.appendWhere(SensorDataTable._ID + " = " + uri.getLastPathSegment());
                break;

            default: throw new IllegalArgumentException("Unknown URI: "+ uri);
        }
        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db,projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)){
            case LOG_PROFILE:
                return LogProfilesTable.CONTENT_TYPE;
            case LOG_PROFILE_ID:
                return LogProfilesTable.CONTENT_ITEM_TYPE;
            case LOG_PROFILE_ITEMS:
                return LogProfileItemsTable.CONTENT_TYPE;
            case LOG_PROFILE_ITEMS_ID:
                return LogProfileItemsTable.CONTENT_ITEM_TYPE;
            case SENSOR_LOGS:
                return SensorLogsTable.CONTENT_TYPE;
            case SENSOR_LOGS_ID:
                return SensorLogsTable.CONTENT_ITEM_TYPE;
            case SENSOR_DATA:
                return SensorDataTable.CONTENT_TYPE;
            case SENSOR_DATA_ID:
                return SensorDataTable.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Uri _uri = null;
        SQLiteDatabase sqlDB = database.getReadableDatabase();
        long id;
        switch (sUriMatcher.match(uri)){
            case LOG_PROFILE:
                id = sqlDB.insert(LogProfilesTable.TABLE_NAME,null,contentValues);
                if(id>0){
                    _uri = ContentUris.withAppendedId((LOG_PROFILE_URI),id);
                }
                break;
            case LOG_PROFILE_ITEMS:
                id = sqlDB.insert(LogProfileItemsTable.TABLE_NAME,null,contentValues);
                if(id>0){
                    _uri = ContentUris.withAppendedId((LOG_PROFILE_ITEMS_URI),id);
                }
                break;
            case SENSOR_LOGS:
                id = sqlDB.insert(SensorLogsTable.TABLE_NAME,null,contentValues);
                if(id>0){
                    _uri = ContentUris.withAppendedId((SENSOR_LOGS_URI),id);
                }
                break;
            case SENSOR_DATA:
                id = sqlDB.insert(SensorDataTable.TABLE_NAME,null,contentValues);
                if(id>0){
                    _uri = ContentUris.withAppendedId((SENSOR_DATA_URI),id);
                }
                break;
            default: throw new SQLException("Failed to insert row into " + uri);

        }

        getContext().getContentResolver().notifyChange(_uri,null);
        return _uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsDeleted;
        SQLiteDatabase db = database.getWritableDatabase();
        switch (sUriMatcher.match(uri)){
            case LOG_PROFILE:
                rowsDeleted = db.delete(LogProfilesTable.TABLE_NAME, selection,selectionArgs);
                break;
            case LOG_PROFILE_ID:
                rowsDeleted = db.delete(LogProfilesTable.TABLE_NAME,LogProfilesTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case LOG_PROFILE_ITEMS:
                rowsDeleted = db.delete(LogProfileItemsTable.TABLE_NAME, selection,selectionArgs);
                break;
            case LOG_PROFILE_ITEMS_ID:
                rowsDeleted = db.delete(LogProfileItemsTable.TABLE_NAME,LogProfileItemsTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case SENSOR_LOGS:
                rowsDeleted = db.delete(SensorLogsTable.TABLE_NAME, selection,selectionArgs);
                break;
            case SENSOR_LOGS_ID:
                rowsDeleted = db.delete(SensorLogsTable.TABLE_NAME,SensorLogsTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case SENSOR_DATA:
                rowsDeleted = db.delete(SensorDataTable.TABLE_NAME, selection,selectionArgs);
                break;
            case SENSOR_DATA_ID:
                rowsDeleted = db.delete(SensorDataTable.TABLE_NAME,SensorDataTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsUpdated;
        SQLiteDatabase db = database.getWritableDatabase();
        switch (sUriMatcher.match(uri)){
            case LOG_PROFILE:
                rowsUpdated = db.update(LogProfilesTable.TABLE_NAME,contentValues, selection,selectionArgs);
                break;
            case LOG_PROFILE_ID:
                rowsUpdated = db.update(LogProfilesTable.TABLE_NAME,contentValues,LogProfilesTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case LOG_PROFILE_ITEMS:
                rowsUpdated = db.update(LogProfileItemsTable.TABLE_NAME, contentValues,selection,selectionArgs);
                break;
            case LOG_PROFILE_ITEMS_ID:
                rowsUpdated = db.update(LogProfileItemsTable.TABLE_NAME, contentValues,LogProfileItemsTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case SENSOR_LOGS:
                rowsUpdated = db.update(SensorLogsTable.TABLE_NAME, contentValues, selection,selectionArgs);
                break;
            case SENSOR_LOGS_ID:
                rowsUpdated = db.update(SensorLogsTable.TABLE_NAME,contentValues,SensorLogsTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            case SENSOR_DATA:
                rowsUpdated = db.update(SensorDataTable.TABLE_NAME, contentValues, selection,selectionArgs);
                break;
            case SENSOR_DATA_ID:
                rowsUpdated = db.update(SensorDataTable.TABLE_NAME,contentValues,SensorDataTable._ID +"="+uri.getLastPathSegment(),(TextUtils.isEmpty(selection)?null:selectionArgs));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return rowsUpdated;
    }
}
