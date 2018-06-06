package com.marekulip.droidsor.bluetoothsensormanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.util.SparseIntArray;

import com.marekulip.droidsor.droidsorservice.DroidsorSensorManager;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.GeneralTISensor;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.TIBarometricSensor;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.TIHumiditySensor;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.TIMovementSensor;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.TIOpticalSensor;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.TITemperatureSensor;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Class used for communication with BLE device. Ensures proper connection, disconnection and data transfer between devices.
 * Created by Marek Ulip on 21.10.2017.
 */

public class BluetoothSensorManager extends DroidsorSensorManager {
    private static final String TAG = BluetoothSensorManager.class.toString();


    /**
     * Message to be send as broadcast when smartphone has connected with BLE device
     */
    public final static String ACTION_GATT_CONNECTED =
            "ACTION_GATT_CONNECTED";
    /**
     * Message to be send as broadcast when smartphone has disconnected with BLE device
     */
    public final static String ACTION_GATT_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    /**
     * Service which should process broadcasts of this class
     */
    private final DroidsorService droidsorService;

    /**
     * BluetoothManager from Android system
     */
    private BluetoothManager mBluetoothManager;
    /**
     * BluetoothAdapter from Android system
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Address of a connected device or address of a device to be connected with.
     */
    private String mBluetoothDeviceAddress;
    /**
     * BluetoothGatt from Android system.
     */
    private BluetoothGatt mBluetoothGatt;

    /**
     * State of connection between smartphone and BLE device. Can be {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING} or {@link #STATE_CONNECTED}
     * Default value is {@link #STATE_DISCONNECTED}
     */
    private int mConnectionState = STATE_DISCONNECTED;
    /**
     * Indicates whether {@link #mBluetoothDeviceAddress} has been set correctly
     */
    private boolean isAddressSet = false;

    private boolean isInitializing = false;
    private boolean isInitializingImportant = false;
    private boolean shouldStopInitializing = false;
    private boolean shouldStopInitializingImportant = false;

    /**
     * List of all supported sensors from BLE device
     */
    private List<GeneralTISensor> sensors = getBasicSetOfSensors();
    /**
     * List of active sensors from BLE device. Active = sensors are sending sensor data.
     */
    private final List<GeneralTISensor> activeSensors = getBasicSetOfSensors();
    /**
     * Semaphore used for communication with BLE device where it is required to wait for answer.
     * So when device sends answer it also releases semaphore.
     */
    private final Semaphore communicationSemaphore = new Semaphore(0,true);
    private final Semaphore initializationSemaphore = new Semaphore(0,true);

    /**
     * Initializes manager and connects to provided service
     * @param service Service which should be able to process broadcast of this class
     */
    public BluetoothSensorManager(DroidsorService service){
        droidsorService = service;
        initialize();
    }

    /**
     * Send broadcast to service with provided action
     * @param action Action to be broadcasted
     */
    private void broadcastUpdate(final String action){
        if(action.equals(ACTION_GATT_CONNECTED)){
            mConnectionState = STATE_CONNECTED;
        }
        droidsorService.broadcastUpdate(action);
    }

    /**
     * Sends broadcast with data to service.
     * @param action Action to be broadcasted. Use with ACTION_DATA_AVAILABLE from the Service so that data get processed.
     * @param sensorDataList data to be send
     */
    private void broadcastUpdate(final String action, List<SensorData> sensorDataList){
        droidsorService.broadcastUpdate(action,sensorDataList);
    }

    /**
     * Initilizes this class
     * @return true if initialization was successful false if there were BT compatability errors.
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) droidsorService.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothSensorManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to a device with specified address
     * @param address Address of a device to connect to
     * @return true if connecting is started
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }
        //If device was connected before just reconnect
        if(mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt !=null){
            if(mBluetoothGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device == null){
            return false;
        }
        // No previous connection establish it now
        mBluetoothGatt = device.connectGatt(droidsorService,false,myBluetoothGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        isAddressSet = true;
        return true;
    }

    /**
     * Attempts to reconnect to disconnected device.
     */
    public void tryToReconnect(){
        if(mConnectionState==STATE_CONNECTED){
            defaultListeningMode();
            return;
        }
        if(isAddressSet)connect(mBluetoothDeviceAddress);//&&mConnectionState!=STATE_CONNECTED)
    }

    /**
     * disconnects from connected device.
     */
    public void disconnect(){
        if(mBluetoothAdapter == null || mBluetoothGatt == null){
            return;
        }
        mBluetoothGatt.disconnect();
        mConnectionState = STATE_DISCONNECTED;
    }

    /**
     * Closes GATT client.
     */
    public void close(){
        if(mBluetoothAdapter == null){
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Determines wheter the BLE device is connected
     * @return true if connected otherwise false
     */
    public boolean isBluetoothDeviceOn(){
        return mConnectionState == STATE_CONNECTED;
    }

    /**
     * Callback to process communication from BLE device.
     */
    private final BluetoothGattCallback myBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                sensors = getBasicSetOfSensors();
                mConnectionState = STATE_CONNECTED;
                mBluetoothGatt.discoverServices();
                broadcastUpdate(BluetoothSensorManager.ACTION_GATT_CONNECTED);
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(BluetoothSensorManager.ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                clearArrays();
                for (BluetoothGattService s : mBluetoothGatt.getServices()){
                    for(GeneralTISensor sensor: sensors){
                        if(sensor.resolveService(s)){
                            activeSensors.add(sensor);
                            break;
                        }
                    }
                }
                initializeSensors(true);

            } else {
                Log.w(TAG, "onServicesDiscovered: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // New data available process and send them
            List<SensorData> dataPackage = new ArrayList<>();
            for (GeneralTISensor sensor: activeSensors) {
                if(sensor.processNewData(characteristic,dataPackage)) break;
            }
            broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE,dataPackage);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // Release semaphore lock so another message can be send.
            communicationSemaphore.release();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            communicationSemaphore.release();
        }
    };

    /**
     * Initializes all desired sensors which were set via {@link #setSensorsToListen(SparseIntArray)} or {@link #defaultListeningMode()} methods.\
     * In other word all sensors which are in {@link #activeSensors} will be activated.
     * @param hasPriority Indicates whether this initialization should stop another going.
     */
    private void initializeSensors(final boolean hasPriority){
        // New thread for semaphore releases
        if(isInitializing && !hasPriority){
            if(isInitializingImportant)return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: " + hasPriority +" "+initializationSemaphore.availablePermits());
                    //Check if another initialization is in progress
                    if(isInitializing){
                        // if yes stop other initialization and after it stops start this one
                        shouldStopInitializing = true;
                        initializationSemaphore.acquire();
                    }
                    isInitializing = true;
                    isInitializingImportant = hasPriority;
                    for (GeneralTISensor sensor : sensors) {
                        // If more important initialization is about to begin start it and stop this.
                        if(shouldStopInitializing){
                            return;
                        }
                        if (activeSensors.contains(sensor)) {
                            configureSensor(sensor, true);
                        } else {
                            configureSensor(sensor, false);
                        }
                    }

                } catch (InterruptedException | NullPointerException e) {
                    // Catching null pointer because if user exits before device was configured
                    // then this exception rises. It does not have any impact on performance because
                    // when null occurs this method would quit anyway.
                    e.printStackTrace();
                } finally {
                    isInitializing = false;
                    isInitializingImportant = false;
                    // Check for the last time of no one is waiting so no locked threads remain.
                    if(shouldStopInitializing){
                        initializationSemaphore.release();
                    }
                    shouldStopInitializing = false;
                }
            }
        }).start();

    }

    /**
     * Enable or disable provided sensor
     * @param sensor Sensor to be enabled or disabled
     * @param enable true to enable false to disable
     * @throws InterruptedException Exception which may be thrown from semaphore errors.
     */
    private void configureSensor(GeneralTISensor sensor, boolean enable) throws InterruptedException{
        // First enable notifications to all Services... then enable sensors.
        sensor.configureNotifications(enable);
        communicationSemaphore.acquire();
        sensor.configureSensor(enable);
        communicationSemaphore.acquire();
        //No need to set frequency when sensor will not be enabled
        if (enable){
            sensor.configureSensorFrequency(listenedSensors.get(sensor.getSensorType(), 1000));
            communicationSemaphore.acquire();
        }
    }

    /**
     * Returns list of all supported sensors provided by BLE device
     * @return list of all supported sensors
     */
    private List<GeneralTISensor> getBasicSetOfSensors(){
        List<GeneralTISensor> sensors = new ArrayList<>();
        sensors.add(new TIHumiditySensor(mBluetoothGatt));
        sensors.add(new TIBarometricSensor(mBluetoothGatt));
        sensors.add(new TIMovementSensor(mBluetoothGatt));
        sensors.add(new TIOpticalSensor(mBluetoothGatt));
        sensors.add(new TITemperatureSensor(mBluetoothGatt));
        return sensors;
    }



    /**
     * Returns all sensors which are actually listened
     * @param sensorTypes list of all listened sensors
     */
    public void getListenedSensorTypes(List<Integer> sensorTypes){
        for(GeneralTISensor sensor: activeSensors){
            if(sensor.getSensorType()== SensorsEnum.EXT_MOVEMENT.sensorType){
                sensorTypes.add(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType);
                sensorTypes.add(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType);
                sensorTypes.add(SensorsEnum.EXT_MOV_MAGNETIC.sensorType);
                continue;
            }
            sensorTypes.add(sensor.getSensorType());
        }
    }

    /**
     * Returns ids of all supported BLE sensors
     * @param sensorTypes list of supported sensors ids
     */
    public void giveMeYourSensorTypesForProfile(List<Integer> sensorTypes){
        List<GeneralTISensor> toIterate = getBasicSetOfSensors();
        for(GeneralTISensor sensor: toIterate){
            sensorTypes.add(sensor.getSensorType());
        }
    }


    @Override
    public void setSensorsToListen(SparseIntArray sensorTypes) {
        clearArrays();
        for(GeneralTISensor sensor: sensors){
            if(containsSensor(sensor.getSensorType(),listenedSensors)){
                activeSensors.add(sensor);
            }
        }
        // If there is some part of movement sensor activate movement sensor
        if(containsSensor(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType,listenedSensors)||containsSensor(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType,listenedSensors)||containsSensor(SensorsEnum.EXT_MOV_MAGNETIC.sensorType,listenedSensors)){
            for(GeneralTISensor sensor: sensors){
                if(sensor.getSensorType()==SensorsEnum.EXT_MOVEMENT.sensorType){
                    activeSensors.add(sensor);
                    break;
                }
            }
        }
        for(int i = 0; i<sensorTypes.size();i++){
            if(sensorTypes.keyAt(i)>=SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType&&sensorTypes.keyAt(i)<=SensorsEnum.EXT_MOV_MAGNETIC.sensorType){
                listenedSensors.put(SensorsEnum.EXT_MOVEMENT.sensorType,sensorTypes.valueAt(i));
                continue;
            }
            listenedSensors.put(sensorTypes.keyAt(i),sensorTypes.valueAt(i));
        }
    }

    /**
     * Starts listening to sensors which were set in setSensorsToListen. Those which are not set will be turned off.
     */
    public void startListening(){
        initializeSensors(true);
    }

    /**
     * Stops listening to all sensors.
     */
    public void stopListening(){
        // Setting empty arrays will cause that all sensors will be turned off.
        setSensorsToListen(new SparseIntArray());
        initializeSensors(false);
    }

    @Override
    public void getAllAvailableSensorTypes(List<Integer> sensors) {

    }

    /**
     * Resets listened sensors so that all sensors are again listened with frequency 1s
     */
    public void defaultListeningMode(){
        // If important profile is initing we don't want to purge sensors that are supposed to log.
        // Although this situation is rare it happens
        if(isInitializingImportant) return;
        clearArrays();
        activeSensors.addAll(sensors);
        initializeSensors(false);
    }

    /**
     * Clears active sensors and listenedSensors arrays
     */
    private void clearArrays(){
        activeSensors.clear();
        listenedSensors.clear();
    }
}
