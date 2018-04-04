package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Class representing Movement sensor from SensorTag CC2650
 * Created by Marek Ulip on 21.10.2017.
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
    public boolean processNewData(BluetoothGattCharacteristic data, List<SensorData> sensorDataList) {
        //This sensor has three sub sensors so different behaviour has to be applied
        if(data.getUuid().equals(dataCharacteristic.getUuid())){
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType, TISensor.MOVEMENT_ACC.convert(data.getValue()),SensorData.getTime()));
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType, TISensor.MOVEMENT_GYRO.convert(data.getValue()),SensorData.getTime()));
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_MAGNETIC.sensorType, TISensor.MOVEMENT_MAG.convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }

    @Override
    public void getSensorTypes(List<Integer> sensorTypes) {
        sensorTypes.add(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType);
        sensorTypes.add(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType);
        sensorTypes.add(SensorsEnum.EXT_MOV_MAGNETIC.sensorType);
    }
}
