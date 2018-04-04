package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Class representing Barometric sensor from SensorTag CC2650
 * Created by Marek Ulip on 21.10.2017.
 */

public class TIBarometricSensor extends GeneralTISensor{

    public TIBarometricSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_BAR_CONF,SensorTagGatt.UUID_BAR_DATA,SensorTagGatt.UUID_BAR_PERI,SensorTagGatt.UUID_BAR_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()),TISensor.BAROMETER.convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }
}