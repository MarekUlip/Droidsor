package com.marekulip.droidsor.androidsensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.marekulip.droidsor.droidsorservice.DroidsorSensorManager;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 22.10.2017.
 * Class for listening sensors on Android device. Ensures that it registers only available sensors. Returns results as a broadcast to a service provided in constructor
 */

public class AndroidSensorManager extends DroidsorSensorManager implements SensorEventListener{
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
     * SparseArray of all hardware available sensors
     */
    private SparseBooleanArray presentSensors = new SparseBooleanArray();
    /**
     * Time from last processing (sending broadcast) of SensorEvent of a particular sensor
     */
    private final SparseLongArray lastSensorsTime = new SparseLongArray();
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
        if(time - lastSensorsTime.get(sensorType) > listenedSensors.get(sensorType, DroidsorSensorManager.defaultSensorFrequency)){
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
            if(time - lastSensorsTime.get(orientationId) > listenedSensors.get(orientationId, DroidsorSensorManager.defaultSensorFrequency)) {
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
     * Starts listening to sensors set via {@link #setSensorsToListen(SparseIntArray)} or {@link #setSensorsToListenSafe(SparseIntArray)} method.
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
    }

    /**
     * Registers listeners for sensors with ids that are contained in {@link #listenedSensors} array.
     */
    private void registerListeners(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasAcc = false, hasMag = false;
                //Iterate through all available sensors and set those contained in toListenIds as active
                for(int i = 0; i<toListen.size();i++){
                    if(containsSensor(toListen.get(i).getType(),listenedSensors)){
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType)hasMag = true;
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType)hasAcc = true;
                        mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                hasOrientation = containsSensor(SensorsEnum.INTERNAL_ORIENTATION.sensorType,listenedSensors);
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
                        listenedSensors.put(toListen.get(i).getType(),listenedSensors.get(SensorsEnum.INTERNAL_ORIENTATION.sensorType,500));
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
     * Set sensors to listen with check if that sensor is really available at the device. This method is slower and only should be used if there is a possibility to set sensors which are not present on the device.
     * @param sensorsToListen ids and frequencies of sensors to listen. Provided ids should be supported by {@link SensorsEnum} enum.
     */
    public void setSensorsToListenSafe(SparseIntArray sensorsToListen){
        //listenFrequencies.clear();
        listenedSensors.clear();
        for (int i = 0,type; i<sensorsToListen.size();i++) {
            type = sensorsToListen.keyAt(i);
            if(presentSensors.get(type,false)){
                listenedSensors.put(type,sensorsToListen.valueAt(i));
            }
        }
    }

    /**
     * Initializes toListenIds list with ids of all supported but also possibly not present sensors
     */
    private void initToListenIds(){
        listenedSensors = new SparseIntArray();
        listenedSensors.put(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_GYROSCOPE.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_LIGHT.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_GRAVITY.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_HUMIDITY.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_BAROMETER.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_TEMPERATURE.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_ORIENTATION.sensorType, DroidsorSensorManager.defaultSensorFrequency);
    }

    /**
     * Filters sensors that are supported but are not present on the device
     */
    private void filterToListenIds(){
        //If this has been done before just reset listenedSensors with all present sensors
        if(!toListen.isEmpty()){
            listenedSensors.clear();
            for(int i = 0; i<presentSensors.size();i++){
                listenedSensors.put(presentSensors.keyAt(i), DroidsorSensorManager.defaultSensorFrequency);
            }
            return;
        }
        //Otherwise find all present sensors
        toListen.clear();
        presentSensors = new SparseBooleanArray();
        List<Sensor>  sensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s: sensors){
            //Ensure that sensor is found only once
            if(!presentSensors.get(s.getType(),false) && containsSensor(s.getType(),listenedSensors)){
                toListen.add(s);
                presentSensors.put(s.getType(),true);
            }
        }
        listenedSensors.clear();
        for(int i = 0; i<toListen.size();i++){
            listenedSensors.put(toListen.get(i).getType(), DroidsorSensorManager.defaultSensorFrequency);
        }
        // If accelerometer and magentometer are present orientation can be measured too
        if(presentSensors.get(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType,false) &&presentSensors.get(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType,false)){
            presentSensors.put(orientationId,true);
            listenedSensors.put(orientationId, DroidsorSensorManager.defaultSensorFrequency);
        }
    }

    @Override
    public void setSensorsToListen(SparseIntArray sensors) {
        listenedSensors = sensors;
    }

    /**
     * Fills provided list with ids of sensors that are actually listened
     * @param sensorTypes List to fill
     */
    public void getListenedSensorTypes(List<Integer> sensorTypes){
        for(int i = 0; i < listenedSensors.size(); i++) {
            sensorTypes.add(listenedSensors.keyAt(i));
        }
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
     * Determines whether provided sensor id is available on the device
     * @param sensorType id to be determined
     * @return true if found otherwise false
     */
    public boolean isSensorPresent(int sensorType){
        return presentSensors.get(sensorType,false);
    }
}
