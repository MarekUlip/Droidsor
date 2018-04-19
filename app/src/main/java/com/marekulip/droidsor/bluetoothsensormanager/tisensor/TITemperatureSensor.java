package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

import static java.lang.Math.pow;

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
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()),convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }

    @Override
    protected Point3D convert(byte[] value) {
        /*
         * The IR Temperature sensor produces two measurements; Object ( AKA target or IR) Temperature, and Ambient ( AKA die ) temperature.
         * Both need some conversion, and Object temperature is dependent on Ambient temperature.
         * They are stored as [ObjLSB, ObjMSB, AmbLSB, AmbMSB] (4 bytes) Which means we need to shift the bytes around to get the correct values.
         */

        double ambient = extractAmbientTemperature(value);
        double targetNewSensor = extractTargetTemperatureTMP007(value);
        return new Point3D(ambient, targetNewSensor,0);
    }

    private double extractAmbientTemperature(byte [] v) {
        return ByteShifter.shortUnsignedAtOffset(v, 2) / 128.0;
    }
    private double extractTargetTemperatureTMP007(byte [] v) {
        return ByteShifter.shortUnsignedAtOffset(v, 0) / 128.0;
    }
}
