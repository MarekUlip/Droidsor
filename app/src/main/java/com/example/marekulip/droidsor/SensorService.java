package com.example.marekulip.droidsor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.marekulip.droidsor.androidsensormanager.AndroidSensorManager;
import com.example.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;
import com.example.marekulip.droidsor.positionmanager.PositionManager;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorLogManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SensorService extends Service {

    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";

    public final static int NO_SENSORS_MODE = -1;
    public final static int ALL_SENSORS_MODE = 0;
    public final static int MOBILE_SENSORS_MODE = 1;
    public final static int BLUETOOTH_SENSORS_MODE = 2;

    private final static int NOTIFICATION_ID = 100;
    private final static String NOTIFICATION_CHANNEL = "sensor_service_channel";
    private final int minSendInterval = 200;
    private long lastTime;

    private SensorLogManager sensorLogManager;
    private BluetoothSensorManager bluetoothSensorManager;
    private AndroidSensorManager androidSensorManager;
    private PositionManager positionManager;//TODO Manage intervals possibly add them when setting up profile
    private final IBinder mBinder = new LocalBinder();
    private int displayMode = ALL_SENSORS_MODE;

    private boolean isStopIntended = false;
    private boolean isListening = false;

    private ArrayDeque<SensorData> sensorDataQueue = new ArrayDeque<>();
    public SensorService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorLogManager= new SensorLogManager(this);
        bluetoothSensorManager = new BluetoothSensorManager(this);
        androidSensorManager = new AndroidSensorManager(this);
        positionManager = new PositionManager(this);
        positionManager.tryInitPosManager();
        createOrUpdateServiceNotification("","");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void broadcastUpdate(final String action, List<SensorData> sensorData){
        if(action.equals(ACTION_DATA_AVAILABLE)) {
            if (sensorLogManager.isLogging()) {
                Location location = positionManager.getLocation();
                for (int i = 0; i < sensorData.size(); i++) {
                    //dataPackage.getDatas().get(i).setLocationData(location.getLongitude(), location.getLatitude(), location.getAltitude());
                    if(location!= null)sensorData.get(i).setLocationData(location.getLongitude(),location.getLatitude(),location.getAltitude()); //TODO fix altitude
                    else sensorData.get(i).setLocationData(0,0,0);
                    sensorLogManager.postNewData(sensorData.get(i));
                }
            }
            if (displayMode == ALL_SENSORS_MODE || isSendable(sensorData)) {
                if(sensorData.size()==1)sensorDataQueue.push(sensorData.get(0));
                else{
                    for(int i =0;i<sensorData.size();i++) {
                        sensorDataQueue.push(sensorData.get(i));
                    }
                }
                if(System.currentTimeMillis()-lastTime > minSendInterval) {
                    lastTime = System.currentTimeMillis();
                    Intent intent = new Intent(action);
                    sendBroadcast(intent);
                }
            }
        }
    }

    public void initialize(){

    }

    public void startListeningSensors(){
        if(!isListening){
            androidSensorManager.startListening();
            bluetoothSensorManager.tryToReconnect();
            isListening = true;
        }
    }

    public void stopListeningSensors(){
        if(isListening) {
            androidSensorManager.stopListening();
            bluetoothSensorManager.disconnect();
            isListening = false;
        }
    }

    /**
     * Used when broadcasting when thing that are not supposed to be shown are not broadcasted and discarded instead.
     * I could just turn the sensors off but at the display mode they can come on any second as user filters the sensors
     * so its better to just ignore broadcasts instead of turning listeners down.
     * @param mode
     */
    public void setMode(int mode){
        displayMode = mode;
        /*switch (mode){
            case MOBILE_SENSORS_MODE:

        }*/
    }

    public boolean isBluetoothDeviceOn(){
        return bluetoothSensorManager.isBluetoothDeviceOn();
    }

    public void connectToBluetoothDevice(final String address){
        bluetoothSensorManager.connect(address);
    }

    public void disconnectFromBluetoothDevice(){
        bluetoothSensorManager.disconnect();
    }

    public void startLogging(LogProfile profile){
        if(profile.isSaveGPS()) {
            Log.d("GPS wanted", "startLogging: Setting GPS");
            positionManager.setIntervals(profile.getGPSFrequency());
            positionManager.startUpdates();//TODO resolve first missing locations
        }
        List<Integer> androidSensorTypes = new ArrayList<>();
        List<Integer> androidSensorFrequencies = new ArrayList<>();
        List<Integer> bluetoothSensorTypes = new ArrayList<>();
        List<Integer> bluetoothSensorFrequencies = new ArrayList<>();
        androidSensorManager.stopListening();
        //androidSensorManager.setSensorsToListen();
        //bluetoothSensorManager.
        for(LogProfileItem item : profile.getLogItems()){
            if(item.getSensorType()<100){
                androidSensorTypes.add(item.getSensorType());
                androidSensorFrequencies.add(item.getScanFrequency());
            }else {
                bluetoothSensorTypes.add(item.getSensorType());
                bluetoothSensorFrequencies.add(item.getScanFrequency());
            }
        }
        androidSensorManager.setSensorsToListen(androidSensorTypes,androidSensorFrequencies);
        androidSensorManager.startListening();//TODO Optimize
        //bluetoothSensorManager.

        androidSensorTypes.addAll(bluetoothSensorTypes);
        sensorLogManager.startLog(profile.getProfileName(),androidSensorTypes);
    }

    public void stopLogging(){
        positionManager.stopUpdates();
        sensorLogManager.endLog();
        androidSensorManager.stopListening();
        androidSensorManager.resetManager();
        androidSensorManager.startListening();
    }

    public boolean isLogging(){
        return sensorLogManager.isLogging();
    }

    public ArrayDeque<SensorData> getSensorDataQueue(){
        return sensorDataQueue;
    }

    public List<Integer> getMonitoredSensorsTypes(boolean ignoreMode){
        List<Integer> sensorTypes = new ArrayList<>();
        if(ignoreMode || displayMode == ALL_SENSORS_MODE){
            androidSensorManager.giveMeYourSensorTypes(sensorTypes);
            if(bluetoothSensorManager.isBluetoothDeviceOn())
            bluetoothSensorManager.giveMeYourSensorTypes(sensorTypes);
        }
        else {
            if(displayMode == BLUETOOTH_SENSORS_MODE){
                //if(bluetoothSensorManager.isBluetoothDeviceOn())
                bluetoothSensorManager.giveMeYourSensorTypes(sensorTypes);
            }
            else if(displayMode == MOBILE_SENSORS_MODE){
                androidSensorManager.giveMeYourSensorTypes(sensorTypes);
            }
        }
        return sensorTypes;
    }

    private boolean isSendable(List<SensorData> sensorDataList){
        if(displayMode == BLUETOOTH_SENSORS_MODE && sensorDataList.get(0).sensorType<100)return false;//It is enough to check only first sensor from list because ussualy only one value is present at list. When there are multiple values in the list they are from the same source
        if(displayMode == MOBILE_SENSORS_MODE && sensorDataList.get(0).sensorType>100)return false;
        return true;
    }

    private void createOrUpdateServiceNotification(String name, String contextText){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("Droidsor Service")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.notification_icon)
                .setOngoing(true);
        Intent stopServiceIntent = new Intent(this, ServiceStopperService.class);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this,0,stopServiceIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mNotifyBuilder.setContentIntent(stopServicePendingIntent);
        //PendingIntent.get
        mNotificationManager.notify(
                NOTIFICATION_ID,
                mNotifyBuilder.build());
    }

    private void destroyServiceNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public void stop(boolean hardStop){
        if(isStopIntended || hardStop) {
            if (isLogging()) sensorLogManager.endLog();
            stopListeningSensors();
            destroyServiceNotification();
            stopSelf();
        }else{
            isStopIntended = true;
            Toast.makeText(getApplicationContext(),"Click again to end Droidsor service and any running logs",Toast.LENGTH_LONG).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    isStopIntended = false;
                }

            }, 10000);
        }
    }



    public class LocalBinder extends Binder {
        SensorService getService(){
            return SensorService.this;
        }
    }
}
