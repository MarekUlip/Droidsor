package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 21.10.2017.
 */

public class SensorDataPackage {
    private List<SensorData> datas = new ArrayList<>();
    private List<Integer> sensorTypes = new ArrayList<>();

    public List<SensorData> getDatas() {
        return datas;
    }

    public List<Integer> getSensorTypes() {
        return sensorTypes;
    }

    public SensorDataPackage(){

    }

    public SensorDataPackage(SensorData data, int sensorType){
        datas.add(data);
        sensorTypes.add(sensorType);
    }

    public void clearPackage(){
        datas.clear();
        sensorTypes.clear();
    }
}
