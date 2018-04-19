package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

import static java.lang.Math.pow;

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
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()),convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }

    @Override
    protected Point3D convert(byte[] value) {
        if (value.length > 4) {
            Integer val = ByteShifter.twentyFourBitUnsignedAtOffset(value, 3);
            //Integer val = ((value[3] & 0xff) + ((value[4]<<8) & 0xff00) + ((value[5]<<16) & 0xff0000));
            return new Point3D((double) val / 100.0, 0, 0);
        }
        else {
            int mantissa;
            int exponent;
            Integer sfloat = ByteShifter.shortUnsignedAtOffset(value, 2);

            mantissa = sfloat & 0x0FFF;
            exponent = (sfloat >> 12) & 0xFF;

            double output;
            double magnitude = pow(2.0f, exponent);
            output = (mantissa * magnitude);
            return new Point3D(output / 100.0f, 0, 0);
        }
    }
}