package com.marekulip.droidsor.droidsorservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.marekulip.droidsor.DroidsorSettingsFramgent;
import com.marekulip.droidsor.R;
import com.marekulip.droidsor.androidsensormanager.AndroidSensorManager;
import com.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;
import com.marekulip.droidsor.nosensormanager.NoSensorManager;
import com.marekulip.droidsor.positionmanager.PositionManager;
import com.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorLogManager;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service used to handle all sensor data. It sends data to activities that are listening. I also takes care
 * of logging feature - it sens data to SensorLogManager.
 */
public class DroidsorService extends Service implements PositionManager.OnRecievedPositionListener{

    /**
     * Action indicating that service has new data and if some activity wants them then it can get them.
     */
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    /**
     * Action indicating that service is about to be stopped. After sending this action service is stopped.
     */
    public final static String SERVICE_IS_TURNING_OFF  = "SERVICE_IS_TURNING_OFF";
    /**
     * Action indicating that log has exceeded maximum log length set in settings and it has been stopped.
     */
    public final static String SCHEDULED_LOG_STOP = "SCHEDULED_LOG_STOP";


    /**
     * Mode where no sensors are viewed.
     */
    public final static int NO_SENSORS_MODE = -1;
    /**
     * Mode where all available sensors are displayed at once
     */
    public final static int ALL_SENSORS_MODE = 0;
    /**
     * Mode where only smartphone sensors are displayed
     */
    public final static int MOBILE_SENSORS_MODE = 1;
    /**
     * Mode where only BT sensors are displayed
     */
    public final static int BLUETOOTH_SENSORS_MODE = 2;
    /**
     * Indicates whether service was stopped or is still contactable. It was created because methods
     * which were checking if this service is running were not 100% reliable. They are still used thought.
     */
    private static boolean isServiceOff = true;

    /**
     * Id of notification this service displays.
     */
    private final static int NOTIFICATION_ID = 101;
    /**
     * Channel of this notification. Without channel no notification would be displayed
     * on Android 8.0 and higher
     */
    private final static String NOTIFICATION_CHANNEL_ID = "droidsor_service_channel";
    /**
     * Minimum interval at which this service should notify its listeners that new data are available.
     */
    private int minSendInterval = 200;
    /**
     * Last time in milliseconds that new data from sensors were detected.
     */
    private long lastTime;

    /**
     * Manager used for starting and stopping logs
     */
    private SensorLogManager sensorLogManager;
    /**
     * Manager used for BT device manipulation
     */
    private BluetoothSensorManager bluetoothSensorManager;
    /**
     * Manager used for Android sensor manipulation
     */
    private AndroidSensorManager androidSensorManager;
    /**
     * Manager used for Android 'things' that are not sensors such as battery or microphone
     */
    private NoSensorManager noSensorManager;
    /**
     * Manager used for getting GPS position.
     */
    private PositionManager positionManager;
    /**
     * Binder used to enable Activities to connect to this service.
     */
    private final IBinder mBinder = new LocalBinder();
    /**
     * Indicates actual display mode.
     */
    private int displayMode = ALL_SENSORS_MODE;

    /**
     * Indicator used to stop service from notification.
     */
    private boolean isStopIntended = false;
    /**
     * Indicator used to determine whether this service is listening to any sensors
     */
    private boolean isListening = false;

    /**
     * SparseArray containing data from sensors
     */
    private final SparseArray<SensorData> sensorDataSparseArray = new SparseArray<>();
    /**
     * Array of sensor types that occurred since last {@link #ACTION_DATA_AVAILABLE} broadcast
     */
    private final List<Integer> sensorTypesOccured = new ArrayList<>();

    /**
     * Timer used to stop logs after time specified in settings
     */
    private Timer logTimer;
    public DroidsorService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorLogManager= new SensorLogManager(this);
        bluetoothSensorManager = new BluetoothSensorManager(this);
        androidSensorManager = new AndroidSensorManager(this);
        noSensorManager = new NoSensorManager(this);
        positionManager = new PositionManager(this);
        // Try to get position if all settings are correct
        positionManager.tryInitPosManager();
        positionManager.setOnRecievedPositionListener(this);
        //createOrUpdateServiceNotification("","");
        startForeground(NOTIFICATION_ID,createOrUpdateServiceNotification());
        isServiceOff = false;
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Indicates whether this service has been started or stopped
     * @return false - this service is running, true - this service is stopped
     */
    public static boolean isServiceOff(){
        return isServiceOff;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Sends broadcast with specified action
     * @param action action to be broadcasted
     */
    public void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Sends broadcast with specified action and data.
     * @param action action to be broadcasted
     * @param sensorData Sensordata to be broadcasted they are also added to log if logging is active
     */
    public void broadcastUpdate(final String action, List<SensorData> sensorData){
        if(action.equals(ACTION_DATA_AVAILABLE)) {
            if (isLogging()) {
                Location location = positionManager.getLocation();
                SensorData data;
                for (int i = 0; i < sensorData.size(); i++) {
                    data = sensorData.get(i);
                    if(location!= null){
                        data.setLocationData(location.getLongitude(),location.getLatitude());
                        if(location.hasAltitude()) data.altitude = location.getAltitude();//TODO fix altitude
                        if(location.hasAccuracy()) data.accuracy = location.getAccuracy();
                        if(location.hasSpeed()) data.speed = location.getSpeed();
                    }
                    sensorLogManager.postNewData(data);
                }
            }
            if (displayMode == ALL_SENSORS_MODE || isSendable(sensorData)||isLogging()) {
                SensorData data;
                if(sensorData.size()==1){
                    data = sensorData.get(0);
                    sensorDataSparseArray.put(data.sensorType,data);
                    if(!sensorTypesOccured.contains(data.sensorType))sensorTypesOccured.add(data.sensorType);
                }
                else{
                    for(int i =0;i<sensorData.size();i++) {
                        data = sensorData.get(i);
                        sensorDataSparseArray.put(data.sensorType,data);
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

    /**
     * Starts listening to all specified sensors provided that its not already listening.
     */
    public void startListeningSensors(){
        if(!isListening){
            androidSensorManager.startListening();
            noSensorManager.startListening();
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SHOW_GPS_DATA,true)) {
                positionManager.startDefaultUpdates();
            }
            bluetoothSensorManager.tryToReconnect();
            isListening = true;
        }
    }

    /**
     * Stops listening to sensors
     */
    public void stopListeningSensors(){
        stopListeningSensors(false);
    }

    /**
     * Stops listening to sensors
     * @param fullStop If set to true it will also disconnect from BT device if connected.
     */
    private void stopListeningSensors(boolean fullStop){
        if(isListening) {
            androidSensorManager.stopListening();
            noSensorManager.stopListening();
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SHOW_GPS_DATA,true)){
                positionManager.stopUpdates();
            }
            if(bluetoothSensorManager.isBluetoothDeviceOn()) {
                if (fullStop || PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.DISCONNECT_FROM_BT_PREF, false)){
                    bluetoothSensorManager.disconnect();
                } else{
                    bluetoothSensorManager.stopListening();
                }
            }
            isListening = false;
            return;
        }
        if(fullStop&&bluetoothSensorManager.isBluetoothDeviceOn())bluetoothSensorManager.disconnect();
    }

    /**
     * Used for broadcasting when sensors that are not supposed to be shown are not broadcasted and discarded instead.
     * Its made this way to ensure that no phantom sensors will during mode switch
     * @param mode Mode to set. Can be  {@link #ALL_SENSORS_MODE}, {@link #MOBILE_SENSORS_MODE} or {@link #BLUETOOTH_SENSORS_MODE}. Any other mode will cause no data sending.
     */
    public void setMode(int mode){
        displayMode = mode;
    }

    /**
     * Returns actual display mode
     * @return actula display mode
     */
    public int getMode(){
        return displayMode;
    }

    /**
     * Indicates whether BT device is connected
     * @return true if connected otherwise false
     */
    public boolean isBluetoothDeviceOn(){
        return bluetoothSensorManager.isBluetoothDeviceOn();
    }

    /**
     * Connects to BT device with specified address.
     * @param address Address of the device to be connected with.
     */
    public void connectToBluetoothDevice(final String address){
        bluetoothSensorManager.connect(address);
    }

    /**
     * Disconnects from connected device.
     */
    public void disconnectFromBluetoothDevice(){
        bluetoothSensorManager.disconnect();
    }

    /**
     * Starts logging based on provided profile.
     * @param profile Profile containing sensors and frequencies to be used in the log
     */
    public void startLogging(LogProfile profile){
        if(profile.isSaveGPS()) {
            positionManager.setIntervals(profile.getGPSFrequency());
            positionManager.startUpdates();
        }
        SparseIntArray androidSensors = new SparseIntArray();
        SparseIntArray noSensors = new SparseIntArray();
        List<Integer> bluetoothSensorTypes = new ArrayList<>();
        List<Integer> bluetoothSensorFrequencies = new ArrayList<>();
        // Stop listening so new sensors can be safely set
        androidSensorManager.stopListening();
        noSensorManager.stopListening();
        List<Integer> sensorsToLog = new ArrayList<>();
        for(LogProfileItem item : profile.getLogItems()){
            if(item.getSensorType()<100) {
                androidSensors.put(item.getSensorType(), item.getScanFrequency());
            }
            else if(item.getSensorType()<200){
                bluetoothSensorTypes.add(item.getSensorType());
                bluetoothSensorFrequencies.add(item.getScanFrequency());
            }else {
                noSensors.put(item.getSensorType(),item.getScanFrequency());
            }
            // sensorsToLog.add(item.getSensorType());
        }
        androidSensorManager.setSensorsToListen(androidSensors);
        androidSensorManager.startListening();
        noSensorManager.setSensorsToListen(noSensors);
        noSensorManager.startListening();
        if(bluetoothSensorManager.isBluetoothDeviceOn()){
            bluetoothSensorManager.setSensorsToListen(bluetoothSensorTypes,bluetoothSensorFrequencies);
            bluetoothSensorManager.startListening();
        }
        androidSensorManager.getListenedSensorTypes(sensorsToLog);
        noSensorManager.getListenedSensorTypes(sensorsToLog);
        sensorsToLog.addAll(bluetoothSensorTypes);
        sensorLogManager.startLog(profile.getProfileName(),sensorsToLog);
        // Create log stopper if set in settings
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SCHEDULED_LOG_END,false)) {
            int time = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(DroidsorSettingsFramgent.SCHEDULED_LOG_END_TIME,"60"));
            if(time > 0 )createLogStopper(time);
        }
    }

    /**
     * Sets timer to stop log after specified duration
     * @param duration Time in minutes
     */
    private void createLogStopper(int duration){
        logTimer= new Timer();
        logTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopLogging();
                sendBroadcast(new Intent(SCHEDULED_LOG_STOP));
            }
        },duration*60*1000);
    }

    /**
     * Stops log stopper before it is executed.
     */
    private void stopLogStopper(){
        if(logTimer!=null){
            logTimer.cancel();
            logTimer.purge();
            logTimer = null;
        }
    }

    /**
     * Stops logging
     */
    public void stopLogging(){
        if(isLogging()) {
            stopLogStopper();
            sensorLogManager.endLog();
            positionManager.stopUpdates();
            androidSensorManager.stopListening();
            noSensorManager.stopListening();

            androidSensorManager.resetManager();
            noSensorManager.resetManager();

            androidSensorManager.startListening();
            noSensorManager.startListening();

            if (bluetoothSensorManager.isBluetoothDeviceOn()) {
                bluetoothSensorManager.defaultListeningMode();
            }
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SHOW_GPS_DATA,true)) {
                positionManager.startDefaultUpdates();
            }
        }
    }

    /**
     * Starts OpenGL mode causing the service to send {@link #ACTION_DATA_AVAILABLE} much faster in 20 ms intervals
     * it also initializes required smartphone sensors.
     */
    public void startOpenGLMode(){
        androidSensorManager.stopListening();
        minSendInterval = 20;
        SparseIntArray sensors = new SparseIntArray();
        sensors.put(SensorsEnum.INTERNAL_ACCELEROMETER.sensorType,minSendInterval);
        sensors.put(SensorsEnum.INTERNAL_GYROSCOPE.sensorType,minSendInterval);
        androidSensorManager.setSensorsToListenSafe(sensors);
        androidSensorManager.startListening();
    }

    /**
     * Stops OpenGL mode and sets sending intervals back to normal
     */
    public void stopOpenGLMode(){
        minSendInterval = 200;
        androidSensorManager.stopListening();
        androidSensorManager.resetManager();
    }

    /**
     * Indicates that it is completely safe to start new log.
     * @return true if it is safe otherwise false
     */
    public boolean isLoggingSafe(){
        return sensorLogManager.isLogging() || !sensorLogManager.isLogCompletelyStopped();
    }

    /**
     * Indicates whether this service is logging.
     * @return true if logging otherwise false
     */
    public boolean isLogging(){
        return sensorLogManager.isLogging();
    }

    /**
     * Returns array of all gathered sensor data
     * @return SparseArray of all gathered sensor data
     */
    public SparseArray<SensorData> getSensorDataSparseArray(){return sensorDataSparseArray;}

    /**
     * Returns list of all occurred sensor type ids after last {@link #ACTION_DATA_AVAILABLE} broadcast
     * @return list of occurred sensors or null if no new sensors occurred.
     */
    public List<Integer> getSensorTypesOccured(){
        if(sensorTypesOccured.isEmpty())return null;
        List<Integer> listToSend = new ArrayList<>(sensorTypesOccured);
        sensorTypesOccured.clear();
        return listToSend;
    }

    /**
     * Returns all currently monitored sensor types based on actual display mode
     * @param ignoreMode if set to true it will return all sensors not caring about display mode
     * @return list of monitored sensors
     */
    public List<Integer> getMonitoredSensorsTypes(boolean ignoreMode){
        List<Integer> sensorTypes = new ArrayList<>();
        if(ignoreMode || displayMode == ALL_SENSORS_MODE||isLogging()){
            androidSensorManager.getListenedSensorTypes(sensorTypes);
            noSensorManager.getListenedSensorTypes(sensorTypes);
            if(bluetoothSensorManager.isBluetoothDeviceOn())
            bluetoothSensorManager.getListenedSensorTypes(sensorTypes);
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SHOW_GPS_DATA,true)) {
                positionManager.getMonitoredSensorTypes(sensorTypes);
            }
        }
        else {
            if(displayMode == BLUETOOTH_SENSORS_MODE){
                bluetoothSensorManager.getListenedSensorTypes(sensorTypes);
            }
            else if(displayMode == MOBILE_SENSORS_MODE){
                androidSensorManager.getListenedSensorTypes(sensorTypes);
                noSensorManager.getListenedSensorTypes(sensorTypes);
                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.SHOW_GPS_DATA,true)) {
                    sensorTypes.add(SensorsEnum.GPS.sensorType);
                }
            }
        }
        return sensorTypes;
    }

    /**
     * Returns all sensor type ids safe for use in profile - no ids are missing due to active logging
     * if BT device is connected sensors from this device are included
     * @return list of all available sensor type ids
     */
    public List<Integer> getSensorTypesForProfile(){
        List<Integer> sensorTypes = new ArrayList<>();
        androidSensorManager.getAllAvailableSensorTypes(sensorTypes);
        noSensorManager.getAllAvailableSensorTypes(sensorTypes);
        if(bluetoothSensorManager.isBluetoothDeviceOn())bluetoothSensorManager.giveMeYourSensorTypesForProfile(sensorTypes);
        return sensorTypes;
    }

    /**
     * Determines whether this sensor data package can be sent to activity base on display mode
     * @param sensorDataList SensorData list to be judged
     * @return true if it is sendable otherwise false
     */
    private boolean isSendable(List<SensorData> sensorDataList) {
        if(sensorDataList.isEmpty())return false;
        //It is enough to check only first sensor from list because ussualy only one value is present at list. When there are multiple values in the list they are from the same source
        return !(displayMode == BLUETOOTH_SENSORS_MODE && sensorDataList.get(0).sensorType < 100) && !(displayMode == MOBILE_SENSORS_MODE && sensorDataList.get(0).sensorType > 100);
    }

    /**
     * Determines whether provided sensor type id is present on current device
     * @param type sensor type id of sensor to be determined
     * @return true if present otherwise false
     */
    public boolean isSensorPresent(int type){
        return androidSensorManager.isSensorPresent(type);
    }

    /**
     * Creates notification for this service capable of closing it on click or double click based on settings
     * @return displayable notification
     */
    private Notification createOrUpdateServiceNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //On Android Oreo there can be no notification without notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence channelName = getString(R.string.app_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            mNotificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.droidsor_service))
                .setContentText(getString(R.string.service_is_running))
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                .setOngoing(true);
        Intent stopServiceIntent = new Intent(this, ServiceStopperService.class);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this,0,stopServiceIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mNotifyBuilder.setContentIntent(stopServicePendingIntent);
        return mNotifyBuilder.build();
    }

    /**
     * Destroys notification issued by this service. Not used since foreground service destroys notification
     * automatically. But kept back if it will be ever needed.
     */
    private void destroyServiceNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Stops this service if all requirements are met.
     * @param hardStop if set to true it ignores all requirements and just stops the service
     */
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

    /**
     * Ends any currently running logs and stops the service.
     */
    private void stop(){
        if (isLogging()) sensorLogManager.endLog();
        sendBroadcast(new Intent(SERVICE_IS_TURNING_OFF));
        stopListeningSensors(true);
        stopForeground(true);
        //destroyServiceNotification();
        isServiceOff = true;
        stopSelf();
    }

    @Override
    public void positionRecieved() {
        SensorData sensorData= new SensorData(SensorsEnum.GPS.sensorType,
                new Point3D(positionManager.getLocation().getLatitude(),
                        positionManager.getLocation().getLongitude(),
                        positionManager.getLocation().getAltitude()
                ),SensorData.getTime());
        List<SensorData> data = new ArrayList<>();
        data.add(sensorData);
        broadcastUpdate(ACTION_DATA_AVAILABLE,data);
    }


    /**
     * Binder used to bind to this service.
     */
    public class LocalBinder extends Binder {
        public DroidsorService getService(){
            return DroidsorService.this;
        }
    }
}
