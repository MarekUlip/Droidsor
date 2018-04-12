package com.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;
import android.util.Log;
import android.util.SparseIntArray;

import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.SenorDataItemsCountTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing log.
 * Created by Marek Ulip on 22.08.2017.
 */

public class SensorLog {
    private static final String TAG = "SensorLog";
    // This class does not use Log Profile because only thing it needs to know is sensor type and
    // keeping all other information like frequency etc. would be wasting of memory.
    /**
     * Id of this log
     */
    private final long logId;
    /**
     * List of sensors that are part of this log
     */
    private List<Integer> sensorsToLog;
    /**
     * Sensor data that were gathered while logging this log
     */
    private List<SensorData> sensorDataList;
    /**
     * Count of items for each sensor used to determine which weight should be assigned to each item
     */
    private final SparseIntArray countOfItems = new SparseIntArray();
    /**
     * Manager of this object which takes care of proper start and end of log
     */
    private SensorLogManager sensorLogManager;
    /**
     * Array of all usable weights
     */
    public static final int[] weights = {10000,1000,100,10,1};

    /**
     * Constructor
     * @param slm sensor log manager
     * @param id id of log (log should be first added into database before it starts so all log items are assigned to correct log)
     * @param sensorTypes types of sensors to log
     */
    public SensorLog(SensorLogManager slm, long id, List<Integer> sensorTypes){
        logId = id;
        sensorLogManager = slm;
        initialize(sensorTypes);
    }

    /**
     * Initializes this object. It was created in case it would be required to change logged sensor types
     * @param sensorTypes
     */
    public void initialize(List<Integer> sensorTypes){
        sensorsToLog = sensorTypes;
        sensorDataList = new ArrayList<>();
    }

    /**
     * Attempts to add provided SensorData into this log. Only adds it if {@link #sensorsToLog} list contains this sensor.
     * @param d SensorData to add to this log
     */
    public void tryToAddItem(SensorData d){
        if(sensorsToLog.contains(d.sensorType)){
            //Log.d(TAG, "tryToAddItem: listening sensor found " + d.sensorType);
            sensorDataList.add(d);
        }
    }

    /**
     * Writes logged items into database.
     */
    public void writeToDatabase(){
        List<SensorData> dataList = new ArrayList<>();
        dataList.addAll(sensorDataList);
        sensorDataList.clear();
        List<ContentValues> bulk = new ArrayList<>();
        int count;
        for(SensorData s: dataList){
            //TODO possible problem with large bulks
                count = countOfItems.get(s.sensorType, 0) + 1;
                countOfItems.put(s.sensorType, count);
                for (Integer i : weights) {

                    if (count % i == 0) {
                        bulk.add(s.getInsertableFormat(logId, i));
                        break;
                    }

                }
        }
        Log.d(TAG, "writeToDatabase: count of entries to write in bulk"+bulk.size());
        sensorLogManager.getContext().getContentResolver().bulkInsert(DroidsorProvider.SENSOR_DATA_URI,bulk.toArray(new ContentValues[bulk.size()]));
    }

    /**
     * Creates basic weights so when log ends unexpectedly it can still be viewed without optimizations.
     */
    void initCountSensorLogItems(){
        ContentValues cv = new ContentValues();
        // Just parse all sensor types and insert count one for them into database
        for(int i = 0, size =sensorsToLog.size(); i<size;i++){
            cv.put(SenorDataItemsCountTable.SENSOR_TYPE,sensorsToLog.get(i));
            cv.put(SenorDataItemsCountTable.LOG_ID,logId);
            cv.put(SenorDataItemsCountTable.COUNT_OF_ITEMS,1);
            sensorLogManager.getContext().getContentResolver().insert(DroidsorProvider.SENSOR_DATA_COUNT_URI,cv);
        }
    }

    /**
     * Writes actual sensor items count into database. Should be used only at end of the log. But it
     * will work during log too. Only mind performance issues.
     */
    void countSensorLogItems(){
        ContentValues cv = new ContentValues();
        for(int i = 0, size =countOfItems.size(); i<size;i++){
            cv.put(SenorDataItemsCountTable.COUNT_OF_ITEMS,countOfItems.valueAt(i));
            sensorLogManager.getContext().getContentResolver().update(DroidsorProvider.SENSOR_DATA_COUNT_URI,cv,SenorDataItemsCountTable.LOG_ID+ " = ? and " + SenorDataItemsCountTable.SENSOR_TYPE + " = ?",new String[]{String.valueOf(logId),String.valueOf(countOfItems.keyAt(i))});
        }
    }


}
