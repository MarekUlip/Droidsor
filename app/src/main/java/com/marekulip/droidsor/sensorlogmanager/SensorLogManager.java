package com.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.SensorLogsTable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class used for managing logging lifecycle - obtaining wakelock, start, inserting into database, counting items, end and releasing wakelock
 * Created by Marek Ulip on 22.08.2017.
 */

public class SensorLogManager {
    private static final String TAG = "SensorLogManager";
   // private List<SensorLog> logs = new ArrayList<>(); //comment on release if more logs wont be necessary
    /**
     * Id of a log that is logged
     */
    private long logId = 0;
    /**
     * Object of the log that is logged
     */
    private SensorLog log;
    /**
     * Indicates whether logging is actually in progress
     */
    private boolean isLogging = false;

    /**
     * Indicates that log has been stopped including finalizing thread and that it is now save to start
     * new log. It is safety measure so no new log starts until old was stopped because stopping old
     * log sets log to null and this should not happen with new log.
     */
    private boolean isLogCompletelyStopped = true;
    /**
     * Context from service
     */
    private Context context;
    /**
     * Wakelock used to keep CPU awake while logging so data are correct.
     */
    private PowerManager.WakeLock wakeLock;
    /**
     * Timer used to periodically write items into database
     */
    private Timer timer;

    /**
     * Constructor
     * @param c Service context
     */
    public SensorLogManager(Context c){
        context = c;
    }

    /**
     * Returns context assigned to this manager
     * @return context assigned to this manager
     */
    public Context getContext(){
        return context;
    }

    /**
     * Starts log with specified name and sensors. Do NOT forget to stop log with {@link #endLog()} method
     * so wakelock that is acquired in this method can be properly released.
     * @param logName Name of this new log
     * @param sensorsToListen sensors this log should contain
     */
    public void startLog(String logName,List<Integer> sensorsToListen){
        //if(!isLogCompletelyStopped) return;
        // First get wakelock to ensure CPU does not turn off and sensors are logged correctly
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"DroidsorWakelockTag");
        wakeLock.acquire();
        // Insert empty log into database so id is known.
        ContentValues cv = new ContentValues();
        cv.put(SensorLogsTable.DATE_OF_START, System.currentTimeMillis());
        cv.put(SensorLogsTable.LOG_NAME,logName);
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

    /**
     * Ends logging and releases wakelock. Note that you should check if manager is really logging with {@link #isLogging()} method otherwise NullPointerException might occur.
     */
    public void endLog(){
        //Log.d(TAG, "endLog: ending logging");
        isLogging = false;
        timer.cancel();
        timer.purge();
        timer = null;
        isLogCompletelyStopped =false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Save remaining items into database
                log.writeToDatabase();
                ContentValues cv = new ContentValues();
                // Updated log with end time
                cv.put(SensorLogsTable.DATE_OF_END,System.currentTimeMillis());
                context.getContentResolver().update(DroidsorProvider.SENSOR_LOGS_URI,cv,SensorLogsTable._ID+" = ?",new String[]{String.valueOf(logId)});
                // Update weights with real values
                log.countSensorLogItems();
                log = null;
                wakeLock.release();
                isLogCompletelyStopped = true;
            }
        }).start();
    }


    /**
     * Indicates whether this manager is currently recording any log.
     * @return true if it is recording otherwise false.
     */
    public boolean isLogging(){
        return isLogging;
    }

    /**
     * Indicates whether previous log was correctly stopped
     * @return true if yes otherwise false
     */
    public boolean isLogCompletelyStopped(){
        return isLogCompletelyStopped;
    }

    /**
     * Sends new data to logged log.
     * @param data Data to be sent.
     */
    public void postNewData(SensorData data){
        log.tryToAddItem(data);
    }

}
