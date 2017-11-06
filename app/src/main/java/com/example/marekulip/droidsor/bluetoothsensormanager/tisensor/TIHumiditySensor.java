package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;
import com.example.marekulip.droidsor.sensorlogmanager.SensorTypeResolver;

/**
 * Created by Fredred on 21.10.2017.
 */

public class TIHumiditySensor extends GeneralTISensor{

    public TIHumiditySensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_HUM_CONF,SensorTagGatt.UUID_HUM_DATA,SensorTagGatt.UUID_HUM_PERI,SensorTagGatt.UUID_HUM_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, SensorDataPackage dataPackage) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            dataPackage.getDatas().add(new SensorData(TISensor.HUMIDITY.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorTypeResolver.resolveSensor(data.getUuid()));
            return true;
        }
        return false;
    }
}
