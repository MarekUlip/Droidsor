package com.example.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.SensorLogsTable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Fredred on 22.08.2017.
 */

public class SensorLogManager {
    private static final String TAG = "SensorLogManager";
   // private List<SensorLog> logs = new ArrayList<>(); //comment on release if more logs wont be necessary
    private int countOfWrittenItems = 0;
    private long logId = 0;
    private SensorLog log;
    private boolean isLogging = false;
    private Context context;
    private PowerManager.WakeLock wakeLock;
    //private SQLiteDatabase db;
    private Timer timer;
    public SensorLogManager(Context c){
        context = c;
    }



    /*public void writeToDatabase(){
        if(db == null){
            db = openDatabase();
        }
        db.beginTransaction();
        for(SensorLog sensorLog: logs){
            sensorLog.writeToDatabase(db);
        }
    }*/

    public Context getContext(){
        return context;
    }

    public void startLog(String logName,List<Integer> sensorsToListen){
        /*if(logs.size() >= 10){
            return;
        }*/
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag");
        wakeLock.acquire();
        ContentValues cv = new ContentValues();
        //Instant.
        cv.put(SensorLogsTable.DATE_OF_START, System.currentTimeMillis());
        cv.put(SensorLogsTable.LOG_NAME,logName);
        /*if(db == null){
            db = openDatabase();
        }*/
        //SensorLog sensorLog = new SensorLog(this,(int)db.insert(SensorLogsTable.TABLE_NAME,null,cv),sensorsToListen);
        //logs.add(sensorLog);
        logId = Long.parseLong(context.getContentResolver().insert(DroidsorProvider.SENSOR_LOGS_URI,cv).getLastPathSegment()); //db.insert(SensorLogsTable.TABLE_NAME,null,cv);
        log = new SensorLog(this,logId,sensorsToListen);
        log.initCountSensorLogItems();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "run: timed databes write");
                if(log!=null)log.writeToDatabase();
            }
        },0,10000);
        Log.d(TAG, "startLog: Log started");
        isLogging = true;
    }

    public void endLog(){
        //logs.remove(logToRemove);
        wakeLock.release();
        Log.d(TAG, "endLog: ending logging");
        isLogging = false;
        timer.cancel();
        timer.purge();
        timer = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.writeToDatabase();
                ContentValues cv = new ContentValues();
                cv.put(SensorLogsTable.DATE_OF_END,System.currentTimeMillis());
                context.getContentResolver().update(DroidsorProvider.SENSOR_LOGS_URI,cv,SensorLogsTable._ID+" = ?",new String[]{String.valueOf(logId)});
                log.countSensorLogItems();
                log = null;
            }
        }).start();

    }



    public boolean isLogging(){
        return isLogging;
    }

    public void postNewData(SensorData data){
        log.tryToAddItem(data);
    }

    /*private SQLiteDatabase openDatabase(){
        return SensorsDataDbHelper.getInstance(context).getWritableDatabase();
    }

    private void closeDatabase(){
//        db.setTransactionSuccessful();
       // db.endTransaction();
        if(db != null){
            db.close();
            db = null;
        }
        if (dbHelper != null){
            dbHelper.close();
            dbHelper = null;
        }


    }*/

}
