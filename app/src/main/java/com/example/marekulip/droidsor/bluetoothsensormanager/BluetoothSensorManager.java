package com.example.marekulip.droidsor.bluetoothsensormanager;

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

import com.example.marekulip.droidsor.DroidsorService;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.GeneralTISensor;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.TIBarometricSensor;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.TIHumiditySensor;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.TIMovementSensor;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.TIOpticalSensor;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.TITemperatureSensor;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by Fredred on 21.10.2017.
 */

public class BluetoothSensorManager {
    private static final String TAG = BluetoothSensorManager.class.toString();


    public final static String ACTION_GATT_CONNECTED =
            "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private final DroidsorService droidsorService;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private boolean isAddressSet = false;
    private boolean areActiveSenorsSet = false;
    private boolean firstTime = true;

   /* private ArrayDeque<GeneralTISensor> descriptors = new ArrayDeque<>();
    private ArrayDeque<GeneralTISensor> characteristics = new ArrayDeque<>();
    private ArrayDeque<GeneralTISensor> frequencies = new ArrayDeque<>();*/
    private List<GeneralTISensor> sensors = getBasicSetOfSensors();
    private final List<GeneralTISensor> activeSensors = new ArrayList<>();
    private final SparseIntArray listenFrequencies = new SparseIntArray();
    private Semaphore communicationSemaphore = new Semaphore(0,true);

    public BluetoothSensorManager(DroidsorService service){
        droidsorService = service;
        initialize();
    }

    public void broadcastUpdate(final String action){
        if(action.equals(ACTION_GATT_CONNECTED)){
            mConnectionState = STATE_CONNECTED;
        }
        droidsorService.broadcastUpdate(action);
    }

    public void broadcastUpdate(final String action, List<SensorData> sensorDataList){
        droidsorService.broadcastUpdate(action,sensorDataList);
    }

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

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if(mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt !=null){
            if(mBluetoothGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        Log.d(TAG, "connect: NULL");
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device == null){
            return false;
        }

        mBluetoothGatt = device.connectGatt(droidsorService,false,myBluetoothGattCallback);
        Log.d(TAG, "connect: GattAddedd");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        isAddressSet = true;
        return true;
    }

    public void tryToReconnect(){
        if(mConnectionState==STATE_CONNECTED){
            defaultListeningMode();
            return;
        }
        if(isAddressSet)connect(mBluetoothDeviceAddress);//&&mConnectionState!=STATE_CONNECTED)
    }

    public void disconnect(){
        if(mBluetoothAdapter == null || mBluetoothGatt == null){
            return;
        }
        Log.d(TAG, "disconnect: ");
        mBluetoothGatt.disconnect();
        mConnectionState = STATE_DISCONNECTED;
    }

    public void close(){
        if(mBluetoothAdapter == null){
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public boolean isBluetoothDeviceOn(){
        return mConnectionState == STATE_CONNECTED;
    }

    private final BluetoothGattCallback myBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                sensors = getBasicSetOfSensors();
                mConnectionState = STATE_CONNECTED;
                mBluetoothGatt.discoverServices();
                broadcastUpdate(BluetoothSensorManager.ACTION_GATT_CONNECTED);
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d(TAG, "onConnectionStateChange: Disconnected");
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(BluetoothSensorManager.ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                clearQues();
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                for (BluetoothGattService s : mBluetoothGatt.getServices()){
                    //Log.d(TAG, "onServicesDiscovered: Iterating services"+s.getUuid()+" their chara count is"+s.getCharacteristics().size());
                    for(GeneralTISensor sensor: sensors){
                        if(sensor.resolveService(s)){
                            /*descriptors.add(sensor);
                            characteristics.add(sensor);
                            frequencies.add(sensor);*/
                            activeSensors.add(sensor);
                            areActiveSenorsSet = true;
                            break;
                        }
                    }
                }
                //getNextSensorNotificationGoing();
                initializeSensors();

            } else {
                Log.w(TAG, "onServicesDiscovered: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            List<SensorData> dataPackage = new ArrayList<>();
            for (GeneralTISensor sensor: activeSensors) {
                if(sensor.processNewData(characteristic,dataPackage)) break;
            }
            broadcastUpdate(DroidsorService.ACTION_DATA_AVAILABLE,dataPackage);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: "+(descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)+" status = "+status);
            communicationSemaphore.release(); //First enable notifications to all Services... then enable sensors. It is made this way so this function doesnt have to search for settings characteriscs.
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: "+characteristic.getUuid());
            communicationSemaphore.release();
        }
    };

    private void initializeSensors(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(GeneralTISensor sensor: sensors){
                    if(activeSensors.contains(sensor)){
                        configureSensor(sensor,true);
                    }else{
                        configureSensor(sensor,false);
                    }
                }
            }
        }).start();

    }
    private void configureSensor(GeneralTISensor sensor, boolean enable){
        try {
            /*if(firstTime){
                communicationSemaphore.acquire();
                firstTime = false;
            }*/
            sensor.configureNotifications(enable);
            communicationSemaphore.acquire();
            sensor.configureSensor(enable);
            communicationSemaphore.acquire();
            if (enable){
                sensor.configureSensorFrequency(listenFrequencies.get(sensor.getSensorType(), 1000));
                communicationSemaphore.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /*private void getNextSensorNotificationGoing(){
        if(descriptors.isEmpty()){
            Log.d(TAG, "getNextSensorGoing: All sensor notifications enabled. Enabling sensors themselves");
            getNextSensorGoing();
            return;
        }
        Log.d(TAG, "getNextSensorGoing: Activating next sensor. Total size is: "+descriptors.size());
        if(activeSensors.contains(descriptors.peek())){
            descriptors.pop().configureNotifications(true);
        }else {
            descriptors.pop().configureNotifications(false);
        }
    }



    private void getNextSensorGoing(){
        if(characteristics.isEmpty()){
            Log.d(TAG, "getNextSensorGoing: Sensors are up and running");
            setNextSensorFrequency();
            return;
        }
        if(activeSensors.contains(characteristics.peek())){
            characteristics.pop().configureSensor(true);
        }else {
            characteristics.pop().configureSensor(false);
        }
    }

    private void setNextSensorFrequency(){
        if(frequencies.isEmpty()){
            Log.d(TAG, "setNextSensorFrequency: All required frequencies has been set");
            return;
        }
        GeneralTISensor sensor = frequencies.pop();
        if(activeSensors.contains(sensor)){
            sensor.configureSensorFrequency(listenFrequencies.get(sensor.getSensorType(),1000));
        }

    }*/

    private List<GeneralTISensor> getBasicSetOfSensors(){
        List<GeneralTISensor> sensors = new ArrayList<>();
        sensors.add(new TIHumiditySensor(mBluetoothGatt));
        sensors.add(new TIBarometricSensor(mBluetoothGatt));
        sensors.add(new TIMovementSensor(mBluetoothGatt));
        sensors.add(new TIOpticalSensor(mBluetoothGatt));
        sensors.add(new TITemperatureSensor(mBluetoothGatt));
        return sensors;
    }

    public void giveMeYourSensorTypes(List<Integer> sensorTypes){
        //Log.d(TAG, "getListenedSensorTypes: "+activeSensors.size());
        List<GeneralTISensor> toIterate;
        if(!areActiveSenorsSet) toIterate = getBasicSetOfSensors();
        else toIterate = activeSensors;
        for(GeneralTISensor sensor: toIterate){
            if(sensor.getSensorType()== SensorsEnum.EXT_MOVEMENT.sensorType){
                sensorTypes.add(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType);
                sensorTypes.add(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType);
                sensorTypes.add(SensorsEnum.EXT_MOV_MAGNETIC.sensorType);
                continue;
            }
            sensorTypes.add(sensor.getSensorType());
        }
    }

    public void giveMeYourSensorTypesForProfile(List<Integer> sensorTypes){
        List<GeneralTISensor> toIterate = getBasicSetOfSensors();
        for(GeneralTISensor sensor: toIterate){
            sensorTypes.add(sensor.getSensorType());
        }
    }

    public void setSensorsToListen(List<Integer> sensorsTypes,List<Integer> sensorFrequencies){
        clearQues();
        for(GeneralTISensor sensor: sensors){
            if(sensorsTypes.contains(sensor.getSensorType())){
                activeSensors.add(sensor);
                //frequencies.add(sensor);
            }
            /*descriptors.add(sensor);
            characteristics.add(sensor);*/
        }
        if(sensorsTypes.contains(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType)||sensorsTypes.contains(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType)||sensorsTypes.contains(SensorsEnum.EXT_MOV_MAGNETIC.sensorType)){
            for(GeneralTISensor sensor: sensors){
                if(sensor.getSensorType()==SensorsEnum.EXT_MOVEMENT.sensorType){
                    activeSensors.add(sensor);
                    /*frequencies.add(sensor);
                    descriptors.add(sensor);
                    characteristics.add(sensor);*/
                    break;
                }
            }
        }
        for(int i = 0; i<sensorsTypes.size();i++){
            if(sensorsTypes.get(i)>=SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType&&sensorsTypes.get(i)<=SensorsEnum.EXT_MOV_MAGNETIC.sensorType){
                listenFrequencies.put(SensorsEnum.EXT_MOVEMENT.sensorType,sensorFrequencies.get(i));
                continue;
            }
            listenFrequencies.put(sensorsTypes.get(i),sensorFrequencies.get(i));
        }
    }

    /**
     * Starts listening to sensors which were set in setSensorsToListen. Those which are not set will be turned off.
     */
    public void startListening(){
        initializeSensors();//getNextSensorNotificationGoing();
    }

    public void defaultListeningMode(){
        clearQues();
        for(GeneralTISensor sensor: sensors){
            Log.d(TAG, "defaultListeningMode: "+sensor.getSensorType());
            /*descriptors.add(sensor);
            characteristics.add(sensor);
            frequencies.add(sensor);*/
            activeSensors.add(sensor);
        }
        initializeSensors();//getNextSensorNotificationGoing();
    }

    private void clearQues(){
        activeSensors.clear();
        /*descriptors.clear();
        characteristics.clear();
        frequencies.clear();*/
        listenFrequencies.clear();
    }
}
