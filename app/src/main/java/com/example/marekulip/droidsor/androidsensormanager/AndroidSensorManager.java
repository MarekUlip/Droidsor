package com.example.marekulip.droidsor.androidsensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.example.marekulip.droidsor.SensorService;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 22.10.2017.
 * Class for listening sensors on Android device. Ensures that it registers only available sensors. Returns results as a broadcast to a service provided in constructor
 */

public class AndroidSensorManager implements SensorEventListener{
    private final SensorService sensorService;
    private final SensorManager mSensorManager;
    private final List<Sensor> toListen = new ArrayList<>();
    private List<Integer> toListenIds;
    private final SparseIntArray listenFrequencies = new SparseIntArray();
    private final SparseLongArray lastSensorsTime = new SparseLongArray();
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
    public AndroidSensorManager(SensorService service){
        sensorService = service;
        mSensorManager = (SensorManager)service.getSystemService(Context.SENSOR_SERVICE);
        resetManager();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long time = System.currentTimeMillis();
        if(time - lastSensorsTime.get(sensorType) > listenFrequencies.get(sensorType,baseListenFrequency)){
            //Log.d("Start", "onSensorChanged: "+sensorEvent.sensor.getType());
            lastSensorsTime.put(sensorType,time);
            List<SensorData> sensorDataList = new ArrayList<>();
            SensorsEnum.resolveSensor(sensorEvent,sensorDataList);
            sensorService.broadcastUpdate(SensorService.ACTION_DATA_AVAILABLE,sensorDataList);
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

        if(hasOrientation &&isAccelSet && isMagFieldSet) {
            if(time - lastSensorsTime.get(orientationId) > listenFrequencies.get(orientationId,baseListenFrequency)) {
                lastSensorsTime.put(orientationId,time);
                updateOrientationAngles();
                List<SensorData> sensorDataList = new ArrayList<>();
                SensorsEnum.INTERNAL_ORIENTATION.resolveSensor(sensorDataList, mOrientationAngles);
                sensorService.broadcastUpdate(SensorService.ACTION_DATA_AVAILABLE, sensorDataList);
                isMagFieldSet = false;
                isAccelSet = false;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,mAccelerometerReading, mMagnetometerReading);
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
    }


    public void startListening(){
        initListeners();
    }

    public void stopListening(){
        endListeners();
    }

    public void resetManager(){
        initSensorsToListenIds();
        initSensorsToListen();
        listenFrequencies.clear();
    }

    /**
     * Registers listeners for sensors with ids that are contained in toListenIds list.
     */
    private void initListeners(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasAcc = false, hasMag = false;
                for(int i = 0; i<toListen.size();i++){
                    //if(toListen.get(i).getType()==orientationId)hasOrientation = true;
                    if(toListenIds.contains(toListen.get(i).getType())){
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType)hasMag = true;
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType)hasAcc = true;
                        mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                hasOrientation = toListenIds.contains(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
                if(hasOrientation && !(hasAcc && hasMag)){
                    //Using loop to ensure that enabled sensors are supported by app
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

    private void endListeners(){
        mSensorManager.unregisterListener(this);
    }

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
     * Initializes toListenIds list with ids of all supported sensors
     */
    private void initSensorsToListenIds(){
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
    private void initSensorsToListen(){
        toListen.clear();
        SparseBooleanArray foundSensors = new SparseBooleanArray();
        List<Sensor>  sensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s: sensors){
            if(!foundSensors.get(s.getType(),false) && toListenIds.contains(s.getType())){
                toListen.add(s);
                foundSensors.put(s.getType(),true);
            }
        }
        toListenIds.clear();
        for(int i = 0; i<toListen.size();i++){
            toListenIds.add(toListen.get(i).getType());
        }
        if(foundSensors.get(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType,false) &&foundSensors.get(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType,false))toListenIds.add(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
    }

    public void giveMeYourSensorTypes(List<Integer> sensorTypes){
        sensorTypes.addAll(toListenIds);
    }
}
