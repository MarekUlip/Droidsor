package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;
import java.util.UUID;

/**
 * Created by Fredred on 21.10.2017.
 */

public abstract class GeneralTISensor {
    protected BluetoothGattCharacteristic configurationCharacteristic;
    //Also contains descriptor
    protected BluetoothGattCharacteristic dataCharacteristic;
    protected BluetoothGattCharacteristic periodCharacteristic;
    protected BluetoothGattService mGattService;
    protected BluetoothGatt mBluetoothGatt;
    protected UUID confUUID;
    protected UUID dataUUID;
    protected UUID periodUUID;
    protected UUID serviceUUID;
    protected int sensorType;
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    /*public GeneralTISensor(BluetoothGatt bluetoothGatt){
        this(bluetoothGatt,null,null,null);
    }*/

    protected GeneralTISensor(BluetoothGatt bluetoothGatt, UUID confUUID, UUID dataUUID, UUID periodUUID, UUID serviceUUID){
        mBluetoothGatt = bluetoothGatt;
        this.confUUID = confUUID;
        this.dataUUID = dataUUID;
        this.periodUUID = periodUUID;
        this.serviceUUID = serviceUUID;
        sensorType = SensorsEnum.resolveSensor(dataUUID);
    }

    public boolean resolveService(BluetoothGattService service){
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

    public void configureSensor(boolean enable){
        configurationCharacteristic.setValue(enable?new byte[]{0x01}:new byte[]{0x00});
        mBluetoothGatt.writeCharacteristic(configurationCharacteristic);
    }

    public void configureNotifications(boolean enable){
        BluetoothGattDescriptor desc = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        mBluetoothGatt.setCharacteristicNotification(desc.getCharacteristic(),enable);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(desc);
    }

    public abstract boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList);

    public void getSensorTypes(List<Integer> sensorTypes){
        sensorTypes.add(sensorType);
    }

    public int getSensorType(){
        return sensorType;
    }
}
