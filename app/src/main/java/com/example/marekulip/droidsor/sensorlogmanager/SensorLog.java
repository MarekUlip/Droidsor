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
    private String time;
    private final List<SensorLogger> sensors = new ArrayList<>();
    private SensorLogManager sensorLogManager;
    public SensorLog(SensorLogManager slm, int id, List<Integer> sensorTypes){
        logId = id;
        sensorLogManager = slm;
        initialize(sensorTypes);
    }

    public void initialize(List<Integer> sensorTypes){
        for(Integer i: sensorTypes){
            sensors.add(new SensorLogger(i));
        }
    }

    public void tryToAddItem(SensorData d){
        if(d.sensorType==0) try {//TODO only for test puprposes
            throw new Exception("Id not set for sensor Data");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Log.d(TAG, "tryToAddItem: "+sensorId);
        //Log.d(TAG, "tryToAddItem: "+sensors.size());
        for(SensorLogger l:sensors){
            //Log.d(TAG, "tryToAddItem: Searching for listening sensor");
            if(l.getSensorType()==d.sensorType){
                Log.d(TAG, "tryToAddItem: listening sensor found " + d.sensorType);
                l.addItem(d);
                break;
            }
        }
    }

    public void writeToDatabase(SQLiteDatabase db){
        Log.d("sd", "writeToDatabase: Writting to database");
        int writtenItems = sensorLogManager.getCountOfWrittenItems();
        int sensorType;
        List<SensorData> datas;
        for(SensorLogger s: sensors){
            sensorType = s.getSensorType();
            datas = s.getDatasToWrite();
            for(SensorData d: datas){
                Log.d("sd", "writeToDatabase: iterating");
                if(writtenItems>499){
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.beginTransaction();
                    writtenItems = 0;
                }
                db.insert(SensorDataTable.TABLE_NAME,null,d.getInsertableFormat(sensorType,logId));
                writtenItems++;
            }
        }
        sensorLogManager.setCountOfWrittenItems(writtenItems);
    }


}
