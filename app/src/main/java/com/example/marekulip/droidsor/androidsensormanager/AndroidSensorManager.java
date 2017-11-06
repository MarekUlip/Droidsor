package com.example.marekulip.droidsor.androidsensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.view.OrientationEventListener;

import com.example.marekulip.droidsor.SensorService;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Fredred on 22.10.2017.
 */

public class AndroidSensorManager implements SensorEventListener{
    private SensorService sensorService;
    private SensorManager mSensorManager;
    private OrientationEventListener orientationEventListener;
    private List<Sensor> toListen = new ArrayList<>();
    private List<Integer> toListenIds;
    private SparseIntArray listenFrequencies = new SparseIntArray();
    private SparseLongArray lastSensorsTime = new SparseLongArray();
    private final int baseListenFrequency = 500;

    public AndroidSensorManager(SensorService service){
        sensorService = service;
        mSensorManager = (SensorManager)service.getSystemService(Context.SENSOR_SERVICE);
        initSensorsToListenIds();
        initSensorsToListen();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long time = System.currentTimeMillis();
        if(time - lastSensorsTime.get(sensorType) > listenFrequencies.get(sensorType,baseListenFrequency)){
            //Log.d("Start", "onSensorChanged: "+sensorEvent.sensor.getType());
            lastSensorsTime.put(sensorType,time);
            SensorDataPackage dataPackage = new SensorDataPackage();
            AndroidSensorResolver.resolveSensor(sensorEvent,dataPackage);
            sensorService.broadcastUpdate(SensorService.ACTION_DATA_AVAILABLE,dataPackage);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void startListening(){
        initListeners();
    }

    public void stopListening(){
        endListeners();
    }

    private void initListeners(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<toListen.size();i++){
                    mSensorManager.registerListener(AndroidSensorManager.this,toListen.get(i),SensorManager.SENSOR_DELAY_UI);
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
        for (int i = 0; i<sensorsToListen.size();i++) {
            //toListen.add(mSensorManager.getDefaultSensor(sensorsToListen.get(i)));
            listenFrequencies.put(sensorsToListen.get(i),listeningFrequencies.get(i));
        }
    }

    private void initOrientationListener(){
        orientationEventListener = new OrientationEventListener(sensorService) {
            @Override
            public void onOrientationChanged(int i) {
                //TODO resolve orientation
            }
        };
    }

    private void initSensorsToListenIds(){
        toListenIds = new ArrayList<>();
        toListenIds.add(Sensor.TYPE_ACCELEROMETER);
        toListenIds.add(Sensor.TYPE_GYROSCOPE);
        toListenIds.add(Sensor.TYPE_MAGNETIC_FIELD);
        toListenIds.add(Sensor.TYPE_LIGHT);
        //toListenIds.add(Sensor.TYPE_PROXIMITY);
        toListenIds.add(Sensor.TYPE_GRAVITY);
    }

    private void initSensorsToListen(){
        List<Sensor>  sensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s: sensors){
            if(toListenIds.contains(s.getType())){
                toListen.add(s);
            }
        }
    }

    public void giveMeYourSensorTypes(List<Integer> sensorTypes){
        sensorTypes.addAll(toListenIds);
    }
}
