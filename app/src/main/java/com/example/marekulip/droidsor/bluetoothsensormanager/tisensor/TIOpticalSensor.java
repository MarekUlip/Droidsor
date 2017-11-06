package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;
import com.example.marekulip.droidsor.sensorlogmanager.SensorTypeResolver;

/**
 * Created by Fredred on 21.10.2017.
 */

public class TIOpticalSensor extends GeneralTISensor{
    public TIOpticalSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_OPT_CONF,SensorTagGatt.UUID_OPT_DATA,SensorTagGatt.UUID_OPT_PERI,SensorTagGatt.UUID_OPT_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, SensorDataPackage dataPackage) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            dataPackage.getDatas().add(new SensorData(TISensor.LUXOMETER.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorTypeResolver.resolveSensor(data.getUuid()));
            return true;
        }
        return false;
    }
}
