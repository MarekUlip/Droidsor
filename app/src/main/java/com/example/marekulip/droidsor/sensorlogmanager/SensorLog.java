package com.example.marekulip.droidsor.sensorlogmanager;

import android.content.ContentValues;
import android.util.Log;
import android.util.SparseIntArray;

import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.SenorDataItemsCountTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 22.08.2017.
 */

public class SensorLog {
    private static final String TAG = "SensorLog";
    private final long logId;
    private List<Integer> sensorsToLog;
    private List<SensorData> sensorDataList;
    private SparseIntArray countOfItems = new SparseIntArray();
    private SensorLogManager sensorLogManager;
    public static final int[] weights = {10000,1000,100,10,1};
    public SensorLog(SensorLogManager slm, long id, List<Integer> sensorTypes){
        logId = id;
        sensorLogManager = slm;
        initialize(sensorTypes);
    }

    public void initialize(List<Integer> sensorTypes){
        sensorsToLog = sensorTypes;
        sensorDataList = new ArrayList<>();
    }

    public void tryToAddItem(SensorData d){
        if(d.sensorType==0) try {//TODO only for test puprposes
            throw new Exception("Id not set for sensor Data");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(sensorsToLog.contains(d.sensorType)){
            //Log.d(TAG, "tryToAddItem: listening sensor found " + d.sensorType);
            sensorDataList.add(d);
        }
    }

    public void writeToDatabase(){
        Log.d(TAG, "writeToDatabase: Writting to database");
        //int writtenItems = sensorLogManager.getCountOfWrittenItems();
        List<SensorData> dataList = new ArrayList<>();
        dataList.addAll(sensorDataList);
        sensorDataList.clear();
        List<ContentValues> bulk = new ArrayList<>();
        int count;
        for(SensorData s: dataList){
            //Log.d(TAG, "writeToDatabase: iterating");
            //TODO possible problem with large bulks
            /*if(writtenItems>499){
                db.setTransactionSuccessful();
                db.endTransaction();
                db.beginTransaction();
                writtenItems = 0;
            }*/
                count = countOfItems.get(s.sensorType, 0) + 1;
                countOfItems.put(s.sensorType, count);
                for (Integer i : weights) {

                    if (count % i == 0) {
                        bulk.add(s.getInsertableFormat(logId, i));
                        break;
                    }

                }

            //bulk.add(s.getInsertableFormat(logId));
            //db.insert(SensorDataTable.TABLE_NAME,null,s.getInsertableFormat(logId));
            //writtenItems++;
        }
        Log.d(TAG, "writeToDatabase: count of entries to write in bulk"+bulk.size());
        sensorLogManager.getContext().getContentResolver().bulkInsert(DroidsorProvider.SENSOR_DATA_URI,bulk.toArray(new ContentValues[bulk.size()]));
        //sensorLogManager.setCountOfWrittenItems(writtenItems);
    }

    public void countSensorLogItems(){
        ContentValues cv = new ContentValues();
        for(int i = 0, size =countOfItems.size(); i<size;i++){
            cv.put(SenorDataItemsCountTable.SENSOR_TYPE,countOfItems.keyAt(i));
            cv.put(SenorDataItemsCountTable.LOG_ID,logId);
            cv.put(SenorDataItemsCountTable.COUNT_OF_ITEMS,countOfItems.valueAt(i));
            sensorLogManager.getContext().getContentResolver().insert(DroidsorProvider.SENSOR_DATA_COUNT_URI,cv);
        }



    }


}
