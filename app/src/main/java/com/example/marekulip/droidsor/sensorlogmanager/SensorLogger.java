package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 23.08.2017.
 */

public class SensorLogger {
    private final int sensorType;
    private List<SensorData> datas = new ArrayList<>();
    public SensorLogger(int sType){
        sensorType = sType;
    }

    public int getSensorType() {
        return sensorType;
    }

    public List<SensorData> getDatas() {
        return datas;
    }

    public List<SensorData> getDatasToWrite(){
        List<SensorData> lst = new ArrayList<>();
        lst.addAll(datas);
        datas.clear();
        return lst;
    }

    public void addItem(SensorData data){
        datas.add(data);
    }

    /*private class SensorData{
        private final double value;
        private final String time;
        private double longitude;
        private double latitude;
        public SensorData(double v, String t, double longt, double lat){
            value = v;
            time = t;
            longitude = longt;
            latitude = lat;
        }
    }*/
}
