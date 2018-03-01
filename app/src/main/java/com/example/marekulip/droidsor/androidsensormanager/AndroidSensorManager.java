package com.example.marekulip.droidsor.androidsensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.example.marekulip.droidsor.SensorService;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Fredred on 22.10.2017.
 */

public class AndroidSensorManager implements SensorEventListener{
    private SensorService sensorService;
    private SensorManager mSensorManager;
    private List<Sensor> toListen = new ArrayList<>();
    private List<Integer> toListenIds;
    private SparseIntArray listenFrequencies = new SparseIntArray();
    private SparseLongArray lastSensorsTime = new SparseLongArray();
    private final int baseListenFrequency = 500;
    private boolean isAccelSet = false;
    private boolean isMagFieldSet = false;
    private final int orientationId = SensorsEnum.INTERNAL_ORIENTATION.sensorType;

    //private static Timer orientationTimer = new Timer();

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];


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

            /*if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                        0, mAccelerometerReading.length);
                isAccelSet=true;
            }
            else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                        0, mMagnetometerReading.length);
                isMagFieldSet = true;
            }*/

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

        if(toListenIds.contains(orientationId) &&isAccelSet && isMagFieldSet) {
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

    private void initListeners(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasAcc = false, hasMag = false, hasOrientation = false;
                for(int i = 0; i<toListen.size();i++){
                    //if(toListen.get(i).getType()==orientationId)hasOrientation = true;
                    if(toListenIds.contains(toListen.get(i).getType())){
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType)hasMag = true;
                        if(toListen.get(i).getType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType)hasAcc = true;
                        mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                if(toListenIds.contains(SensorsEnum.INTERNAL_ORIENTATION.sensorType))hasOrientation = true;
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

                /*if(toListenIds.contains(SensorsEnum.INTERNAL_ORIENTATION.sensorType)){//TODO make required sensor run
                    orientationTimer = new Timer();
                    Log.d("tst", "run: "+listenFrequencies.get(SensorsEnum.INTERNAL_ORIENTATION.sensorType,baseListenFrequency));
                    orientationTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.d("OrientationTimer", "run: ");
                        if(isAccelSet && isMagFieldSet) {
                            updateOrientationAngles();
                            List<SensorData> sensorDataList = new ArrayList<>();
                            SensorsEnum.INTERNAL_ORIENTATION.resolveSensor(sensorDataList, mOrientationAngles);
                            sensorService.broadcastUpdate(SensorService.ACTION_DATA_AVAILABLE, sensorDataList);
                        }
                    }
                    },listenFrequencies.get(SensorsEnum.INTERNAL_ORIENTATION.sensorType,baseListenFrequency),listenFrequencies.get(SensorsEnum.INTERNAL_ORIENTATION.sensorType,baseListenFrequency));
                }*/
            }
        }).start();
    }

    private void endListeners(){
        mSensorManager.unregisterListener(this);
        /*if(orientationTimer != null) {
            orientationTimer.cancel();
            orientationTimer.purge();
            orientationTimer = null;
        }*/
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

    //used to listen to basic sensors
    private void initSensorsToListenIds(){
        toListenIds = new ArrayList<>();
        toListenIds.add(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_MAGNETOMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_GYROSCOPE.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_LIGHT.sensorType);
        //toListenIds.add(Sensor.TYPE_PROXIMITY);
        toListenIds.add(SensorsEnum.INTERNAL_GRAVITY.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_HUMIDITY.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_BAROMETER.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_TEMPERATURE.sensorType);
        toListenIds.add(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
    }

    //filter sensor that are desired but are not present on the device
    private void initSensorsToListen(){//TODO optimize
        toListen.clear();
        List<Sensor>  sensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s: sensors){
            if(toListenIds.contains(s.getType())){
                toListen.add(s);
            }
        }
        if(toListenIds.size() != toListen.size()-1){
            toListenIds.clear();
            for(int i = 0; i<toListen.size();i++){
                toListenIds.add(toListen.get(i).getType());
            }
            toListenIds.add(SensorsEnum.INTERNAL_ORIENTATION.sensorType);
        }
    }

    public void giveMeYourSensorTypes(List<Integer> sensorTypes){
        sensorTypes.addAll(toListenIds);
    }
}
