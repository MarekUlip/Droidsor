package com.example.marekulip.droidsor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
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
    /**
     * Temporary profile used only for one log then it is deleted.
     */
    private LogProfile tempLogProfile = null;

    private boolean isStopIntended = false;
    private boolean isListening = false;

    private ArrayDeque<SensorData> sensorDataQueue = new ArrayDeque<>();
    private SparseArray<SensorData> sensorDataSparseArray = new SparseArray<>();
    private List<Integer> sensorTypesOccured = new ArrayList<>();
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
                SensorData data;
                for (int i = 0; i < sensorData.size(); i++) {
                    //dataPackage.getDatas().get(i).setLocationData(location.getLongitude(), location.getLatitude(), location.getAltitude());
                    data = sensorData.get(i);
                    if(location!= null){
                        data.setLocationData(location.getLongitude(),location.getLatitude());
                        if(location.hasAltitude()) data.altitude = location.getAltitude();//TODO fix altitude
                        if(location.hasAccuracy()) data.accuracy = location.getAccuracy();
                        if(location.hasSpeed()) data.speed = location.getSpeed();
                    }
                    //else sensorData.get(i).setLocationData(0,0);
                    //Log.d("test", "broadcastUpdate: "+data.sensorType);
                    sensorLogManager.postNewData(data);
                }
            }
            if (displayMode == ALL_SENSORS_MODE || isSendable(sensorData)||isLogging()) {
                SensorData data;
                if(sensorData.size()==1){
                    data = sensorData.get(0);
                    sensorDataSparseArray.put(data.sensorType,data);//sensorDataQueue.push(sensorData.get(0));
                    if(!sensorTypesOccured.contains(data.sensorType))sensorTypesOccured.add(data.sensorType);
                }
                else{
                    for(int i =0;i<sensorData.size();i++) {
                        data = sensorData.get(i);
                        sensorDataSparseArray.put(data.sensorType,data);//sensorDataQueue.push(sensorData.get(i));
                        if(!sensorTypesOccured.contains(data.sensorType))sensorTypesOccured.add(data.sensorType);
                    }
                }
                if(System.currentTimeMillis()-lastTime > minSendInterval) {
                    lastTime = System.currentTimeMillis();
                    Intent intent = new Intent(action);
                    sendBroadcast(intent);
                }
            }
        } else if(action.equals(BluetoothSensorManager.ACTION_GATT_CONNECTED)){
            Intent intent = new Intent(action);
            sendBroadcast(intent);
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
        stopListeningSensors(false);
    }

    private void stopListeningSensors(boolean fullStop){
        if(isListening) {
            androidSensorManager.stopListening();
            if(fullStop||PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.DISCONNECT_FROM_BT_PREF,false))bluetoothSensorManager.disconnect();
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

    public void setTempLogProfile(LogProfile profile){
        tempLogProfile = profile;
    }

    public LogProfile getTempLogProfile(){
        return tempLogProfile;
    }

    public void startTempProfileLogging(){
        startLogging(tempLogProfile);
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
        androidSensorManager.stopListening();//TODO turn off bluetooth when it is not required
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
        if(bluetoothSensorManager.isBluetoothDeviceOn()){
            bluetoothSensorManager.setSensorsToListen(bluetoothSensorTypes,bluetoothSensorFrequencies);
            bluetoothSensorManager.startListening();
        }
        //bluetoothSensorManager.
        List<Integer> sensorsToLog = new ArrayList<>();
        sensorsToLog.addAll(androidSensorTypes);
        sensorsToLog.addAll(bluetoothSensorTypes);
        sensorLogManager.startLog(profile.getProfileName(),sensorsToLog);
    }

    public void stopLogging(){
        sensorLogManager.endLog();
        positionManager.stopUpdates();
        androidSensorManager.stopListening();
        androidSensorManager.resetManager();
        androidSensorManager.startListening();
        if(bluetoothSensorManager.isBluetoothDeviceOn()){
            bluetoothSensorManager.defaultListeningMode();
        }
        if(tempLogProfile!=null) tempLogProfile = null;//TODO consider letting it be and use it till user sets favorite profile
    }

    public boolean isLogging(){
        return sensorLogManager.isLogging();
    }

    /*public ArrayDeque<SensorData> getSensorDataQueue(){
        return sensorDataQueue;
    }*/

    public SparseArray<SensorData> getSensorDataSparseArray(){return sensorDataSparseArray;}
    public List<Integer> getSensorTypesOccured(){
        if(sensorTypesOccured.isEmpty())return null;
        List<Integer> listToSend = new ArrayList<>();
        listToSend.addAll(sensorTypesOccured);
        sensorTypesOccured.clear();
        return listToSend;
    }

    public List<Integer> getMonitoredSensorsTypes(boolean ignoreMode){
        List<Integer> sensorTypes = new ArrayList<>();
        if(ignoreMode || displayMode == ALL_SENSORS_MODE||isLogging()){
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

    public List<Integer> getSensorTypesForProfile(){
        List<Integer> sensorTypes = new ArrayList<>();
        androidSensorManager.giveMeYourSensorTypes(sensorTypes);
        if(bluetoothSensorManager.isBluetoothDeviceOn())bluetoothSensorManager.giveMeYourSensorTypesForProfile(sensorTypes);
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
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
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
            stop();
        }else{
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.ONE_CLICK_NOTIFICATION_EXIT,false)){
                stop();
                return;
            }
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

    private void stop(){
        if (isLogging()) sensorLogManager.endLog();
        stopListeningSensors(true);
        destroyServiceNotification();
        stopSelf();
    }



    public class LocalBinder extends Binder {
        SensorService getService(){
            return SensorService.this;
        }
    }
}
