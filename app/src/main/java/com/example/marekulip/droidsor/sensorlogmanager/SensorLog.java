package com.example.marekulip.droidsor.sensorlogmanager;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.marekulip.droidsor.database.SensorDataTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 22.08.2017.
 */

public class SensorLog {
    private static final String TAG = "SensorLog";
    private final int logId;
    private List<Integer> sensorsToLog;
    private List<SensorData> sensorDataList;
    private SensorLogManager sensorLogManager;
    public SensorLog(SensorLogManager slm, int id, List<Integer> sensorTypes){
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
            Log.d(TAG, "tryToAddItem: listening sensor found " + d.sensorType);
            sensorDataList.add(d);
        }
    }

    public void writeToDatabase(SQLiteDatabase db){
        Log.d("sd", "writeToDatabase: Writting to database");
        int writtenItems = sensorLogManager.getCountOfWrittenItems();
        List<SensorData> dataList = new ArrayList<>();
        dataList.addAll(sensorDataList);
        sensorDataList.clear();
        for(SensorData s: dataList){
            Log.d("sd", "writeToDatabase: iterating");
            if(writtenItems>499){
                db.setTransactionSuccessful();
                db.endTransaction();
                db.beginTransaction();
                writtenItems = 0;
            }
            db.insert(SensorDataTable.TABLE_NAME,null,s.getInsertableFormat(logId));
            writtenItems++;
        }
        sensorLogManager.setCountOfWrittenItems(writtenItems);
    }


}
