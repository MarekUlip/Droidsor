package com.marekulip.droidsor.nosensormanager;

import android.media.MediaRecorder;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import com.marekulip.droidsor.droidsorservice.DroidsorSensorManagerIface;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that monitors everything monitorable that is not sensor such as battery and sound intensity.
 */
public class NoSensorManager implements DroidsorSensorManagerIface {
    private final DroidsorService droidsorService;
    private MediaRecorder mediaRecorder = null;
    private Timer timer;
    private TimerTask timerTask;
    private SparseIntArray listenedSensors = new SparseIntArray();
    private SparseLongArray lastTimeSensorFrequencies = new SparseLongArray();

    public NoSensorManager(DroidsorService droidsorService){
        this.droidsorService = droidsorService;
        initTimer();
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
        initSensors();
        initTimer();
        int defaultFrequency = 200;
        timer.schedule(timerTask, defaultFrequency, defaultFrequency);
    }
    @Override
    public void stopListening(){
        stopSensors();
        if(timer != null) {
            timer.cancel();
            timer.purge();
            timerTask = null;
        }
    }

    @Override
    public void getAllAvailableSensorTypes(List<Integer> sensors) {
        for(int i = 0; i<listenedSensors.size();i++){
            sensors.add(listenedSensors.keyAt(i));
        }
    }

    /**
     * Sets ids for supported no sensors to be listened
     */
    private void initListenedSensors(){
        listenedSensors.clear();
        listenedSensors.put(SensorsEnum.INTERNAL_MICROPHONE.sensorType,DroidsorSensorManagerIface.defaultSensorFrequency);
    }

    private void initTimer(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                //Log.d("dd", "run: ");
                long time = System.currentTimeMillis();
                List<SensorData> sensorDatas = null;
                SensorData sensorData;
                for(int i =0,key; i< listenedSensors.size();i++){
                    key = listenedSensors.keyAt(i);
                    if(time - lastTimeSensorFrequencies.get(key,0) > listenedSensors.valueAt(i)) {
                        lastTimeSensorFrequencies.put(key,time);
                        if(sensorDatas== null)sensorDatas = new ArrayList<>();
                        sensorData = new SensorData(SensorsEnum.INTERNAL_MICROPHONE.sensorType, new Point3D(getSoundIntensity(), 0.0, 0.0), SensorData.getTime());
                        sensorDatas.add(sensorData);
                    }
                }
                if(sensorDatas != null) droidsorService.broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE,sensorDatas);
            }
        };
    }

    public void getMonitoredSensors(List<Integer> sensors){
        for(int i =0; i< listenedSensors.size();i++){
            sensors.add(listenedSensors.keyAt(i));
        }
    }

    /*public void setSensorsToListen(List<Integer> sensors, List<Integer> frequencies){
        listenedSensors.clear();
        for(int i = 0; i<sensors.)
        listenedSensors.addAll(sensors);
        for(int i : frequencies){
            sensorFrequencies.put(i,);
        }
    }*/

    private void initSensors(){
        initMediaRecorder();
    }

    private void stopSensors(){
        stopMediaRecorder();
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
}
