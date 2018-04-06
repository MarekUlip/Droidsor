package com.example.marekulip.droidsor.sensorlogmanager;

/**
 * Class representing one item in a list of {@link LogProfile} class.
 * Created by Marek Ulip on 27.10.2017.
 */

public class LogProfileItem {
    // access is public because this class is also used in list view where access should be as fast
    // as possible. Kept getters and setters for normal classes to keep encapsulation.
    /**
     * Indicates whether this sensor should be logged
     */
    public boolean isEnabled;
    /**
     * Sensor type id
     */
    public int sensorType;
    /**
     * Frequency at which data from sensor should be gathered
     */
    public int scanFrequency = 200;

    /**
     * Constructor.
     * @param isEnabled
     * @param sensorType id should be present in {@link SensorsEnum} enum.
     * @param scanFrequency values smaller thant 200 can cause great battery and memory consumption while logging
     */
    public LogProfileItem(boolean isEnabled, int sensorType, int scanFrequency){
        this.isEnabled = isEnabled;
        this.sensorType = sensorType;
        this.scanFrequency = scanFrequency;
    }

    /**
     * Initializes this item for specified sensor type which is NOT enabled and its frequency is set to 200 ms
     * @param sensorType
     */
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
