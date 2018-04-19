package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;
import java.util.UUID;

/**
 * Class that provides base functions for BLE sensor communication.
 * This class should be extended for every sensor.
 * Created by Marek Ulip on 21.10.2017.
 */

public abstract class GeneralTISensor {
    /**
     * Characteristic used for enabling sensor
     */
    protected BluetoothGattCharacteristic configurationCharacteristic;
    /**
     * Characteristic used for data processing. Also contains descriptor with which it is possible
     * to setup notifications.
     */
    protected BluetoothGattCharacteristic dataCharacteristic;
    /**
     * Characteristic used for frequency setting.
     */
    protected BluetoothGattCharacteristic periodCharacteristic;
    /**
     * GattService from Android
     */
    protected BluetoothGattService mGattService;
    /**
     * Bluetooth Gatt from Android
     */
    protected final BluetoothGatt mBluetoothGatt;
    /**
     * UUID of configuration characteristic
     */
    protected final UUID confUUID;
    /**
     * UUID of data characteristic
     */
    protected final UUID dataUUID;
    /**
     * UUID of period characteristic
     */
    protected final UUID periodUUID;
    /**
     * UUID of service characteristic
     */
    protected final UUID serviceUUID;
    /**
     * Sensor id of this sensor. Should match some enum from {@link SensorsEnum} enum.
     */
    protected final int sensorType;
    /**
     * UUID used to find descriptor to setup notifications
     */
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Constructor. Only assigns provided values
     * @param bluetoothGatt functional Gatt client
     * @param confUUID configuration characteristic UUID
     * @param dataUUID data characteristic UUID
     * @param periodUUID frequency characteristic UUID
     * @param serviceUUID service UUID
     */
    protected GeneralTISensor(BluetoothGatt bluetoothGatt, UUID confUUID, UUID dataUUID, UUID periodUUID, UUID serviceUUID){
        mBluetoothGatt = bluetoothGatt;
        this.confUUID = confUUID;
        this.dataUUID = dataUUID;
        this.periodUUID = periodUUID;
        this.serviceUUID = serviceUUID;
        sensorType = SensorsEnum.resolveSensor(dataUUID);
    }

    /**
     * Determines wheter provided service belongs to this sensor
     * @param service sensor to determine
     * @return true if belongs otherwise false
     */
    public boolean resolveService(BluetoothGattService service){
        //Yes it belongs. Process it and take all required characteristics
        if(service.getUuid().equals(serviceUUID)){
            mGattService = service;
            for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()){
                if(characteristic.getUuid().equals(confUUID)){
                    configurationCharacteristic = characteristic;
                    continue;
                }
                if(characteristic.getUuid().equals(dataUUID)){
                    dataCharacteristic = characteristic;
                    continue;
                }
                if(characteristic.getUuid().equals(periodUUID)){
                    periodCharacteristic = characteristic;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Configure sensor to either turn it on or off
     * @param enable true to turn sensor on false to turn sensor off
     */
    public void configureSensor(boolean enable){
        configurationCharacteristic.setValue(enable?new byte[]{0x01}:new byte[]{0x00});
        mBluetoothGatt.writeCharacteristic(configurationCharacteristic);
    }

    /**
     * Configure sensor notifications
     * @param enable true to enable them false to disable
     */
    public void configureNotifications(boolean enable){
        BluetoothGattDescriptor desc = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        mBluetoothGatt.setCharacteristicNotification(desc.getCharacteristic(),enable);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(desc);
    }

    /**
     * Configures sensor frequency.
     * @param frequency frequency to be set. Max value 2450 min value 100. If bounds are exceeded value is transformed to its relative max or min value.
     */
    public void configureSensorFrequency(int frequency){
        if (frequency > 2450) frequency = 2450;
        if (frequency < 100) frequency = 100;
        byte f = (byte)((frequency/10));//first frequency for movement must be 1000
        periodCharacteristic.setValue(new byte[]{f});
        mBluetoothGatt.writeCharacteristic(periodCharacteristic);
    }

    /**
     * Processed data from sensor
     * @param data Data to be processed
     * @param sensorDataList list to which processed data should be written
     * @return true if processing has been successful
     */
    public abstract boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList);

    /**
     * Adds all sensor ids contained in this sensor
     * @param sensorTypes list to which ids should be added
     */
    public void getSensorTypes(List<Integer> sensorTypes){
        sensorTypes.add(sensorType);
    }

    /**
     * Returns sensor type id of this sensor
     * @return sensor type id
     */
    public int getSensorType(){
        return sensorType;
    }

    /**
     * Converts received bytes into sensor data
     * @param value bytes to be converted
     * @return {@link Point3D} object with sensor data.
     */
    protected abstract Point3D convert(final byte[] value);
}
