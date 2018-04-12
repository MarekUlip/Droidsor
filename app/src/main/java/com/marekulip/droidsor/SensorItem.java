package com.marekulip.droidsor;

/**
 * Created by Marek Ulip on 19-Sep-17.
 * Class used to hold data from sensor so they can be shown in the list.
 */

public class SensorItem {

    public String sensorName;
    public int sensorType;
    /**
     * Formatted string of all sensor data that should be shown.
     */
    public String sensorValue;

    public SensorItem(String sensorName, String sensorValue){
        this(sensorName,sensorValue,0);
    }

    /**
     * @param sensorValue Formatted string of all sensor data that should be shown.
     */
    public SensorItem(String sensorName, String sensorValue,int sensorType){
        this.sensorName = sensorName;
        this.sensorValue = sensorValue;
        this.sensorType = sensorType;
    }
}
