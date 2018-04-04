package com.example.marekulip.droidsor.androidsensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.example.marekulip.droidsor.DroidsorService;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 22.10.2017.
 * Class for listening sensors on Android device. Ensures that it registers only available sensors. Returns results as a broadcast to a service provided in constructor
 */

public class AndroidSensorManager implements SensorEventListener{
    /**
     * Service that should receive broadcasts
     */
    private final DroidsorService droidsorService;
    /**
     * SensorManager from Android system
     */
    private final SensorManager mSensorManager;
    /**
     * List of all sensors available on the smartphone, used for registering listeners where
     * reference to Sensor object is required
     */
    private final List<Sensor> toListen = new ArrayList<>();
    /**
     * List of ids to be listened. Ids should match real sensors ids only exception is orientation
     * sensor
     */
    private List<Integer> toListenIds;
    /**
     * Frequencies for sensors to be applied
     */
    private final SparseIntArray listenFrequencies = new SparseIntArray();
    /**
     * SparseArray of all hardware available sensors
     */
    private SparseBooleanArray presentSensors = new SparseBooleanArray();
    /**
     * Time from last processing (sending broadcast) of SensorEvent of a particular sensor
     */
    private final SparseLongArray lastSensorsTime = new SparseLongArray();
    /**
     * Basic listening frequency when no logging is active
     */
    private final int baseListenFrequency = 500;
    /**
     * Indicates if new data from accelerometer has been processed
     */
    private boolean isAccelSet = false;
    /**
     * Indicates if new data from magnetometer has been processed
     */
    private boolean isMagFieldSet = false;
    /**
     * Indicates if orientation is listened. Also used to reduce list iteration.
     */
    private boolean hasOrientation =false;
    private final int orientationId = SensorsEnum.INTERNAL_ORIENTATION.sensorType;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];


    /**
     * Creates an instance of AndroidSensorManager and starts listening to all sensors. New data are sent to a service via broadcast.
     * @param service Service which should process the broadcast
     */
    public AndroidSensorManager(DroidsorService service){
        droidsorService = service;
        mSensorManager = (SensorManager)service.getSystemService(Context.SENSOR_SERVICE);
        initManager();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long time = System.currentTimeMillis();
        //If elapsed time is greater than set frequency process this event
        if(time - lastSensorsTime.get(sensorType) > listenFrequencies.get(sensorType,baseListenFrequency)){
            lastSensorsTime.put(sensorType,time);
            List<SensorData> sensorDataList = new ArrayList<>();
            SensorsEnum.resolveSensor(sensorEvent,sensorDataList);
            droidsorService.broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE,sensorDataList);
        }
        if (!isAccelSet && sensorType == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            isAccelSet=true;
        }
        else if (!isMagFieldSet && sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
            isMagFieldSet = true;
        }

        //Process orientation
        if(hasOrientation &&isAccelSet && isMagFieldSet) {
            if(time - lastSensorsTime.get(orientationId) > listenFrequencies.get(orientationId,baseListenFrequency)) {
                lastSensorsTime.put(orientationId,time);
                updateOrientationAngles();
                List<SensorData> sensorDataList = new ArrayList<>();
                SensorsEnum.INTERNAL_ORIENTATION.resolveSensor(sensorDataList, mOrientationAngles);
                droidsorService.broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE, sensorDataList);
                isMagFieldSet = false;
                isAccelSet = false;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Counts orientation of the device
     */
    private void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,mAccelerometerReading, mMagnetometerReading);
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
    }


    /**
     * Starts listening to sensors set via {@link #setSensorsToListen(List, List)} or {@link #setSensorsToListenSafe(List, List)} method.
     */
    public void startListening(){
        registerListeners();
    }

    /**
     * Stops listening to all sensors. Set sensors are kept. For reseting set sensors call {@link #resetManager()} method.
     */
    public void stopListening(){
        endListeners();
    }

    /**
     * Initializes this manager by finding all available sensors
     */
    private void initManager(){
        initToListenIds();
        filterToListenIds();
    }

    /**
     * Resets sensors by setting all available sensors to listen.
     */
    public void resetManager(){
        filterToListenIds();
        listenFrequencies.clear();
    }

    /**
     * Registers listeners for sensors with ids that are contained in {@link #toListenIds} list.
     */
    private void registerListeners(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasAcc = false, hasMag = false;
                //Iterate through all available sensors and set those contained in toListenIds as active
                for(int i = 0; i<toListen.size();i++){
                    if(toListenIds.contains(toListen.get(i).getType())){
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType)hasMag = true;
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType)hasAcc = true;
                        mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                hasOrientation = toListenIds.contains(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
                // If orientation is required and one of sensors required to count orientation
                // is not set then set it here
                if(hasOrientation && !(hasAcc && hasMag)){
                    // Using loop to ensure that enabled sensors are supported by app (in case of two
                    // sensors of the same type)
                    for(int i = 0; i<toListen.size();i++){
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType){
                            if(hasMag)continue;
                            hasMag = true;
                        }
                        else if(toListen.get(i).getType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType){
                            if(hasAcc)continue;
                            hasAcc = true;
                        }
                        else continue;
                        mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_NORMAL);
                        listenFrequencies.put(toListen.get(i).getType(),listenFrequencies.get(SensorsEnum.INTERNAL_ORIENTATION.sensorType,500));
                        if(hasAcc  && hasMag)break;
                    }
                }
            }
        }).start();
    }

    /**
     * This has same effect as {@link #stopListening()} method. It is kept back in case that stopListening
     * method would do additional things than just call this method in the future.
     */
    private void endListeners(){
        mSensorManager.unregisterListener(this);
    }

    /**
     * Sets sensors to listen without checking if they are truly present on device.
     * @param sensorsToListen ids of sensors to listen. Provided ids should be supported by {@link SensorsEnum} enum.
     * @param listeningFrequencies Frequencies in which this manager should send broadcasts. Frequencies should have same index as sensor id.
     */
    public void setSensorsToListen(List<Integer> sensorsToListen, List<Integer> listeningFrequencies){
        //toListen.clear();
        listenFrequencies.clear();
        toListenIds = sensorsToListen;
        for (int i = 0; i<sensorsToListen.size();i++) {
            //toListen.add(mSensorManager.getDefaultSensor(sensorsToListen.get(i)));
            listenFrequencies.put(sensorsToListen.get(i),listeningFrequencies.get(i));
        }
    }

    /**
     * Set sensors to listen with check if that sensor is really available at the device. This method is slower and only should be used if there is a possibility to set sensors which are not present on the device.
     * @param sensorsToListen ids of sensors to listen. Provided ids should be supported by {@link SensorsEnum} enum.
     * @param listeningFrequencies Frequencies in which this manager should send broadcasts. Frequencies should have same index as sensor id.
     */
    public void setSensorsToListenSafe(List<Integer> sensorsToListen, List<Integer> listeningFrequencies){
        listenFrequencies.clear();
        toListenIds.clear();
        int type;
        for (int i = 0; i<sensorsToListen.size();i++) {
            type = sensorsToListen.get(i);
            if(presentSensors.get(type,false)){
                toListenIds.add(type);
                listenFrequencies.put(type,listeningFrequencies.get(i));
            }
        }
    }

    /**
     * Initializes toListenIds list with ids of all supported but also possibly not present sensors
     */
    private void initToListenIds(){
        toListenIds = new ArrayList<>();
        toListenIds.add(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_GYROSCOPE.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_LIGHT.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_GRAVITY.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_HUMIDITY.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_BAROMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_TEMPERATURE.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
    }

    /**
     * Filters sensors that are supported but are not present on the device
     */
    private void filterToListenIds(){
        //If this has been done before just reset toListenIds with all present sensors
        if(!toListen.isEmpty()){
            toListenIds.clear();
            getAllAvailableSensorTypes(toListenIds);
            return;
        }
        //Otherwise find all present sensors
        toListen.clear();
        presentSensors = new SparseBooleanArray();
        List<Sensor>  sensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s: sensors){
            if(!presentSensors.get(s.getType(),false) && toListenIds.contains(s.getType())){
                toListen.add(s);
                presentSensors.put(s.getType(),true);
            }
        }
        toListenIds.clear();
        for(int i = 0; i<toListen.size();i++){
            toListenIds.add(toListen.get(i).getType());
        }
        // If accelerometer and magentometer are present orientation can be measured too
        if(presentSensors.get(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType,false) &&presentSensors.get(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType,false)){
            presentSensors.put(orientationId,true);
            toListenIds.add(orientationId);
        }
    }

    /**
     * Fills provided list with ids of sensors that are actually listened
     * @param sensorTypes List to fill
     */
    public void getListenedSensorTypes(List<Integer> sensorTypes){
        sensorTypes.addAll(toListenIds);
    }

    /**
     * Fills provided list with ids of all available sensors on the device
     * @param sensorTypes List to fill
     */
    public void getAllAvailableSensorTypes(List<Integer> sensorTypes){
        for(int i = 0; i < presentSensors.size(); i++) {
            sensorTypes.add(presentSensors.keyAt(i));
        }
    }

    /**
     * Determines wheter provided sensor id is available on the device
     * @param sensorType id to be determined
     * @return true if found otherwise false
     */
    public boolean isSensorPresent(int sensorType){
        return presentSensors.get(sensorType,false);
    }
}
