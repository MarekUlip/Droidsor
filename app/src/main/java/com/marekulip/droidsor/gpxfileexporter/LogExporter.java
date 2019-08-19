package com.marekulip.droidsor.gpxfileexporter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.SensorDataTable;
import com.marekulip.droidsor.database.SensorLogsTable;
import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Class used to export Sensor logs.
 * Created by Marek Ulip on 05-Feb-18.
 */

public class LogExporter {

    /**
     * Exports log or part of it based on provided id and sensor types
     * @param ctxt context
     * @param id Id of the log to export
     * @param sensorTypes types of sensors to be exported. The rest of sensor data wont be exported. If null whole log will be exported
     */
    public static void exportLog(final Context ctxt,final long id, @Nullable final List<Integer> sensorTypes){
        if(!DroidsorExporter.checkForWritePermission(ctxt))return;
        final Context appContext = ctxt.getApplicationContext();
        Toast.makeText(appContext, R.string.started_exporting,Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c;
                List<SensorData> data = new ArrayList<>();
                if(sensorTypes != null){
                    String[] params= new String[sensorTypes.size()+1];
                    params[0] = String.valueOf(id);
                    makeParameters(params,sensorTypes);
                    c = appContext.getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,SensorDataTable.LOG_ID+ " = ? AND "+SensorDataTable.SENSOR_TYPE+" IN ("+makePlaceholders(sensorTypes.size())+")",params,null);
                }else{
                    c = appContext.getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null, SensorDataTable.LOG_ID+ " = ?",new String[]{String.valueOf(id)},null);
                }
                if(c!=null && c.moveToFirst()){
                    do{
                        addItemToListFromCursor(c,data);
                    }while(c.moveToNext());
                    c.close();
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); // Quoted "Z" to indicate UTC, no timezone offset
                    df.setTimeZone(tz);
                    String name = "Log";
                    c = appContext.getContentResolver().query(DroidsorProvider.SENSOR_LOGS_URI, new String[]{SensorLogsTable.LOG_NAME},SensorLogsTable._ID + " = ?",new String[]{String.valueOf(id)},null);
                    if(c != null && c.moveToFirst()){
                        name = c.getString(c.getColumnIndexOrThrow(SensorLogsTable.LOG_NAME));
                        c.close();
                    }
                    GPXExporter.exportLogItems(data,  id +"_"+name + "_Exported_at_"+df.format(new Date(System.currentTimeMillis())), appContext);
                    toast(appContext,appContext.getString(R.string.exporting_done) +" " + Environment.getExternalStorageDirectory() + "/"+ appContext.getString(R.string.app_name),Toast.LENGTH_LONG);
                }

            }
        }).start();
    }

    /**
     * Heleper method to add item from cursor into the list.
     * @param c cursor with items
     * @param data list to which data from cursor should be added
     */
    private static void addItemToListFromCursor(Cursor c, List<SensorData> data){
        data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                ,new Point3D(
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
        ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE)),
                c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SPEED)),
                c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.ACCURACY))
        ));
    }

    /**
     * Helper method to display toast from other than UI thread
     * @param context should be application context
     * @param text text to be shown
     * @param length duration of toast
     */
    private static void toast(final Context context, final String text, final int length) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, length).show();
            }
        });
    }

    /**
     * Used for making placeholder parameters in SQLite select where clause IN is used
     * @param len number of placeholders
     * @return placeholder string
     */
    private static String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    /**
     * Used to set parameters for placeholders in SQLITE IN clause
     * @param params array to which params should be added
     * @param idList source of params
     */
    private static void makeParameters(String[] params,List<Integer> idList) {
        if (params.length < 2) {
            // It will lead to an invalid query
            throw new RuntimeException("No placeholders");
        } else {
            for (int i = 1; i <= idList.size(); i++) {
                params[i] = String.valueOf(idList.get(i-1));
            }
        }
    }
}
