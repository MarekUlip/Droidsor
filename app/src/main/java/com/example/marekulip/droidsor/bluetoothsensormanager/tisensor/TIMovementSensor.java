package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;
import com.example.marekulip.droidsor.sensorlogmanager.SensorTypeResolver;

/**
 * Created by Fredred on 21.10.2017.
 */

public class TIMovementSensor extends GeneralTISensor{

    public TIMovementSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_MOV_CONF,SensorTagGatt.UUID_MOV_DATA,SensorTagGatt.UUID_MOV_PERI,SensorTagGatt.UUID_MOV_SERV);
    }

    @Override
    public void configureSensor(boolean enable) {
        configurationCharacteristic.setValue(enable?new byte[]{0x7F,0x02}:new byte[]{0x00,0x00});
        mBluetoothGatt.writeCharacteristic(configurationCharacteristic);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, SensorDataPackage dataPackage) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            dataPackage.getDatas().add(new SensorData(TISensor.MOVEMENT_ACC.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorTypeResolver.EXT_MOV_ACC);
            dataPackage.getDatas().add(new SensorData(TISensor.MOVEMENT_GYRO.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorTypeResolver.EXT_MOV_GYRO);
            dataPackage.getDatas().add(new SensorData(TISensor.MOVEMENT_MAG.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorTypeResolver.EXT_MOV_MAG);
            return true;
        }
        return false;
    }
}
