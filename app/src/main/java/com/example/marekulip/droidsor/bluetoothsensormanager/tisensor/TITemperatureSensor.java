package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

/**
 * Created by Fredred on 21.10.2017.
 */

public class TITemperatureSensor extends GeneralTISensor{

    public TITemperatureSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_IRT_CONF,SensorTagGatt.UUID_IRT_DATA,SensorTagGatt.UUID_IRT_PERI,SensorTagGatt.UUID_IRT_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, SensorDataPackage dataPackage) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            dataPackage.getDatas().add(new SensorData(TISensor.IR_TEMPERATURE.convert(data.getValue()),SensorData.getTime()));
            dataPackage.getSensorTypes().add(SensorsEnum.resolveSensor(data.getUuid()));
            return true;
        }
        return false;
    }
}
