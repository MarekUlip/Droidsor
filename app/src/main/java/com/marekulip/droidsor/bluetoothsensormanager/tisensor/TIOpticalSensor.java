package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

import static java.lang.Math.pow;

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
            sensorDataList.add(new SensorData(SensorsEnum.resolveSensor(data.getUuid()), convert(data.getValue()),SensorData.getTime()));
            return true;
        }
        return false;
    }

    @Override
    protected Point3D convert(byte[] value) {
        Integer sfloat= ByteShifter.shortUnsignedAtOffset(value, 0);

        int mantissa = sfloat & 0x0FFF;
        int exponent = (sfloat & 0xF000) >> 12;
        exponent = (exponent == 0) ? 1 : 2 << (exponent - 1);
        return new Point3D(mantissa * (0.01 * exponent),0,0);
    }
}
