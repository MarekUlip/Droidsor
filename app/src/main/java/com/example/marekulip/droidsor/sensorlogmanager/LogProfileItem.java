package com.example.marekulip.droidsor.sensorlogmanager;

/**
 * Created by Fredred on 27.10.2017.
 */

public class LogProfileItem {
    public boolean isEnabled;
    public int sensorType;
    public int scanFrequency = 200;

    public LogProfileItem(boolean isEnabled, int sensorType, int scanFrequency){
        this.isEnabled = isEnabled;
        this.sensorType = sensorType;
        this.scanFrequency = scanFrequency;
    }

    public LogProfileItem(int sensorType){
        this(false,sensorType,200);
    }

    public int getSensorType() {
        return sensorType;
    }

    public void setSensorType(int sensorType) {
        this.sensorType = sensorType;
    }

    public int getScanFrequency() {
        return scanFrequency;
    }

    public void setScanFrequency(int scanFrequency) {
        this.scanFrequency = scanFrequency;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
