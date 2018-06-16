package com.marekulip.droidsor.nosensormanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.marekulip.droidsor.droidsorservice.DroidsorSensorManager;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that monitors everything monitorable that is not sensor such as battery and sound intensity.
 */
public class NoSensorManager extends DroidsorSensorManager {
    private final DroidsorService droidsorService;
    private MediaRecorder mediaRecorder = null;
    private Thread listeningThread;
    private SparseLongArray lastTimeSensorFrequencies = new SparseLongArray();
    /**
     * Each initialized listening thread has an item in this array which indicates whether the thread
     * is allow to run or not. It is made this way so that new threads can be started immediately without
     * waiting for the old thread to be stopped.
     */
    private SparseBooleanArray listeningThreadIndicators = new SparseBooleanArray();
    private BatteryListener batteryListener;
    /**
     * Id that will be assigned to a created thread so it can be added to {@link #listeningThreadIndicators}
     * array.
     */
    private int listeningThreadId = 0;

    public NoSensorManager(DroidsorService droidsorService){
        this.droidsorService = droidsorService;
        batteryListener = new BatteryListener(droidsorService);
        initListenedSensors();
    }

    @Override
    public void setSensorsToListen(SparseIntArray sensors) {
        listenedSensors = sensors;
    }

    @Override
    public void getListenedSensorTypes(List<Integer> sensors) {
        getAllAvailableSensorTypes(sensors);
    }
    @Override
    public void startListening(){
        if(listenedSensors.size() == 0)return;
        initSensors();
        int smallestFrequency = Integer.MAX_VALUE;
        for (int i = 0; i< listenedSensors.size(); i++){
            if(listenedSensors.keyAt(i)<smallestFrequency){
                smallestFrequency = listenedSensors.keyAt(i);
            }
        }
        listeningThreadIndicators.put(listeningThreadId,true);
        initListeningThread(smallestFrequency,listeningThreadId);
    }
    @Override
    public void stopListening(){
        //Stops current running thread
        listeningThreadIndicators.delete(listeningThreadId++);
        stopSensors();
    }

    @Override
    public void getAllAvailableSensorTypes(List<Integer> sensors) {
        for(int i = 0; i<listenedSensors.size();i++){
            sensors.add(listenedSensors.keyAt(i));
        }
    }

    public void resetManager(){
        initListenedSensors();
    }

    /**
     * Sets ids for supported no sensors to be listened
     */
    private void initListenedSensors(){
        listenedSensors.clear();
        listenedSensors.put(SensorsEnum.INTERNAL_MICROPHONE.sensorType, DroidsorSensorManager.defaultSensorFrequency);
        listenedSensors.put(SensorsEnum.INTERNAL_BATTERY.sensorType,DroidsorSensorManager.defaultSensorFrequency);
    }

    /*private void initListeningThread(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                //Log.d("dd", "run: ");
                if(!isListening)return;
                long time = System.currentTimeMillis();
                List<SensorData> sensorDatas = null;
                SensorData sensorData;
                for(int i =0,key; i< listenedSensors.size();i++){
                    key = listenedSensors.keyAt(i);
                    if(time - lastTimeSensorFrequencies.get(key,0) > listenedSensors.valueAt(i)) {
                        //Log.d("tst", "run: "+key);
                        lastTimeSensorFrequencies.put(key,time);
                        if(sensorDatas== null)sensorDatas = new ArrayList<>();
                        if(key == SensorsEnum.INTERNAL_MICROPHONE.sensorType) {
                            sensorData = new SensorData(SensorsEnum.INTERNAL_MICROPHONE.sensorType, new Point3D(getSoundIntensity(), 0.0, 0.0), SensorData.getTime());
                            sensorDatas.add(sensorData);
                        }
                        else if(key == SensorsEnum.INTERNAL_BATTERY.sensorType){
                            sensorData = new SensorData(SensorsEnum.INTERNAL_BATTERY.sensorType, new Point3D(batteryListener.getTemp(),batteryListener.getLevel(),0.0),SensorData.getTime());
                            sensorDatas.add(sensorData);
                        }
                    }
                }
                if(sensorDatas != null) droidsorService.broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE,sensorDatas);
            }
        };
    }*/

    private void initListeningThread(final long frequency, final int id){
        listeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(listeningThreadIndicators.get(id,false)) {
                    long time = System.currentTimeMillis();
                    List<SensorData> sensorDatas = null;
                    SensorData sensorData;
                    for (int i = 0, key; i < listenedSensors.size(); i++) {
                        key = listenedSensors.keyAt(i);
                        if (time - lastTimeSensorFrequencies.get(key, 0) > listenedSensors.valueAt(i)) {
                            //Log.d("tst", "run: "+key);
                            lastTimeSensorFrequencies.put(key, time);
                            if (sensorDatas == null) sensorDatas = new ArrayList<>();
                            if (key == SensorsEnum.INTERNAL_MICROPHONE.sensorType) {
                                sensorData = new SensorData(SensorsEnum.INTERNAL_MICROPHONE.sensorType, new Point3D(getSoundIntensity(), 0.0, 0.0), SensorData.getTime());
                                sensorDatas.add(sensorData);
                            } else if (key == SensorsEnum.INTERNAL_BATTERY.sensorType) {
                                sensorData = new SensorData(SensorsEnum.INTERNAL_BATTERY.sensorType, new Point3D(batteryListener.getTemp(), batteryListener.getLevel(), 0.0), SensorData.getTime());
                                sensorDatas.add(sensorData);
                            }
                        }
                    }
                    if (sensorDatas != null)
                        droidsorService.broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE, sensorDatas);
                    try {
                        Thread.sleep(frequency);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        listeningThread.start();
    }

    private void initSensors(){
        if(containsSensor(SensorsEnum.INTERNAL_MICROPHONE.sensorType,listenedSensors))initMediaRecorder();
        if(containsSensor(SensorsEnum.INTERNAL_BATTERY.sensorType,listenedSensors))batteryListener.startListening();
    }

    private void stopSensors(){
        if(containsSensor(SensorsEnum.INTERNAL_MICROPHONE.sensorType,listenedSensors))stopMediaRecorder();
        if(containsSensor(SensorsEnum.INTERNAL_BATTERY.sensorType,listenedSensors))batteryListener.stopListening();//TODO dont stop unregistered
    }

    /**
     * Sets up media recorder so it can then provide sound intensity
     */
    private void initMediaRecorder(){
        if (mediaRecorder == null) {
            try {
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile("/dev/null");
                mediaRecorder.prepare();
                mediaRecorder.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops media recorder
     */
    private void stopMediaRecorder(){
        if(mediaRecorder != null){
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    /**
     * Returns highest sound amplitude since last call
     * @return sound intensity in decibels
     */
    private int getSoundIntensity(){
        if(mediaRecorder != null){
            return (int)(20 * Math.log(mediaRecorder.getMaxAmplitude()) / 2.302585092994046);
        }
        else return 0;
    }

    private class BatteryListener extends BroadcastReceiver{
        private float temp = 0;
        private int level = 0;
        private Context context;
        private boolean isReceiverRegistered = false;

        BatteryListener(Context context){
            this.context = context;
        }

        public void startListening(){
            context.registerReceiver(this,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isReceiverRegistered = true;
        }

        public void stopListening(){
            if(isReceiverRegistered) {
                context.unregisterReceiver(this);
                isReceiverRegistered = false;
            }
        }

        float getTemp(){
            return temp / 10;
        }

        float getLevel(){
            return level;
        }

        @Override
        public void onReceive(Context arg0, Intent intent) {
            temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
        }
    }
}
