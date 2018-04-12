package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Class represents temperature sensor from SensorTag CC2650
 * Created by Marek Ulip on 21.10.2017.
 */

public class TITemperatureSensor extends GeneralTISensor{

    public TITemperatureSensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_IRT_CONF,SensorTagGatt.UUID_IRT_DATA,SensorTagGatt.UUID_IRT_PERI,SensorTagGatt.UUID_IRT_SERV);
    }

    @Override
    public boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList) {
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()),TISensor.IR_TEMPERATURE.convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }
}
