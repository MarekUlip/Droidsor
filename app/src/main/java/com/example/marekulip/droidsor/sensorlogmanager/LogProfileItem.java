package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.List;

/**
 * Created by Fredred on 27.10.2017.
 */

public class LogProfileItem {
    private boolean isEnabled;
    private int sensorType;
    private int scanFrequency;
    private boolean saveGPSLocation;

    public LogProfileItem(boolean isEnabled, int sensorType, int scanFrequency, boolean saveGPSLocation){
        this.isEnabled = isEnabled;
        this.sensorType = sensorType;
        this.scanFrequency = scanFrequency;
        this.saveGPSLocation = saveGPSLocation;
    }

    public LogProfileItem(int sensorType){
        this(false,sensorType,0,false);
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

    public boolean isSaveGPSLocation() {
        return saveGPSLocation;
    }

    public void setSaveGPSLocation(boolean saveGPSLocation) {
        this.saveGPSLocation = saveGPSLocation;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
