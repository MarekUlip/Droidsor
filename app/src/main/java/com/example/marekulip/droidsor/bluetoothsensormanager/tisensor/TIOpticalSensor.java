package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Class represents Optical sensor from SensorTag CC2650
 * Created by Marek Ulip on 21.10.2017.
 */

public class TIOpticalSensor extends GeneralTISensor{
    public TIOpticalSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_OPT_CONF,SensorTagGatt.UUID_OPT_DATA,SensorTagGatt.UUID_OPT_PERI,SensorTagGatt.UUID_OPT_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()), TISensor.LUXOMETER.convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }
}
