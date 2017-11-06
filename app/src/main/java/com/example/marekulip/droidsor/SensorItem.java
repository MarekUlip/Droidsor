package com.example.marekulip.droidsor;

/**
 * Created by Marek Ulip on 19-Sep-17.
 */

public class SensorItem {

    public String sensorName;
    public int sensorType;
    public String sensorValue;

    public SensorItem(String sensorName, String sensorValue){
        this.sensorName = sensorName;
        this.sensorValue = sensorValue;
    }
}
