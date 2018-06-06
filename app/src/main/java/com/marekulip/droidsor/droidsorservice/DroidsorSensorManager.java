package com.marekulip.droidsor.droidsorservice;

import android.util.SparseIntArray;

import java.util.List;

/**
 * Base class to all classes that work with sensors or thins related with sensors (microphone)
 */
public abstract class DroidsorSensorManager {
    /**
     * Default sensor frequency that will be set if no other frequency is provided
     */
    protected static int defaultSensorFrequency = 500;
    /**
     * Ids and frequencies of sensors to be listened. All contained ids should be part of
     * {@link com.marekulip.droidsor.sensorlogmanager.SensorsEnum}
     */
    protected SparseIntArray listenedSensors = new SparseIntArray();
    /**
     * Sets sensor to be listened. To start listening call {@link #startListening()} method.
     * @param sensors id and frequency of sensors to be listened
     */
    public abstract void setSensorsToListen(SparseIntArray sensors);

    /**
     * Fills provided list with ids of sensors that are currently listened by this manager
     * @param sensors list to be filled
     */
    public abstract void getListenedSensorTypes(List<Integer> sensors);

    /**
     * Starts listening of sensors provided by {@link #setSensorsToListen(SparseIntArray)} method.
     * If {@link #setSensorsToListen(SparseIntArray)} was never called this method will start listening
     * all available sensors.
     */
    public abstract void startListening();

    /**
     * Stops listening all listened sensors.
     */
    public abstract void stopListening();

    /**
     * Returns all sensors that are available by this manager whether they are listened or not.
     * @param sensors
     */
    public abstract void getAllAvailableSensorTypes(List<Integer> sensors);

    /**
     * Helper method. Determines whether provided array contains provided id.
     * @param type id of sensor to be found
     * @param sensorArray id to be searched
     * @return true if id is present otherwise false
     */
    protected boolean containsSensor(int type, SparseIntArray sensorArray){
            return sensorArray.get(type,-1) != -1;
    }
}
