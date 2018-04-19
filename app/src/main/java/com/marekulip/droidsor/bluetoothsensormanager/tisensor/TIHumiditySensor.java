package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Class representing Humidity sensor from SensorTag CC2650
 * Created by Marek Ulip on 21.10.2017.
 */

public class TIHumiditySensor extends GeneralTISensor{

    public TIHumiditySensor(BluetoothGatt bluetoothGatt){
        super(bluetoothGatt, SensorTagGatt.UUID_HUM_CONF,SensorTagGatt.UUID_HUM_DATA,SensorTagGatt.UUID_HUM_PERI,SensorTagGatt.UUID_HUM_SERV);
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
        int a = ByteShifter.shortUnsignedAtOffset(value, 2);
        a &=  ~0x0003; // remove status bits

        return new Point3D(((double)a / 65536)*100,0,0);//((double)a / 65536f)*100, 0, 0);
    }
}
