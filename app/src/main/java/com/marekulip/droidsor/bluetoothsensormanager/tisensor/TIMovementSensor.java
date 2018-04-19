package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

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
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType, convertAcc(data.getValue()),SensorData.getTime()));
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType, convertGyr(data.getValue()),SensorData.getTime()));
            sensorDataList.add(new SensorData(SensorsEnum.EXT_MOV_MAGNETIC.sensorType, convertMag(data.getValue()),SensorData.getTime()));
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

    @Override
    protected Point3D convert(byte[] value) {
        return null;
    }
    private Point3D convertAcc(byte[] value) {
        final float SCALE = (float) 4096.0;

        int x = (value[7]<<8) + value[6];
        int y = (value[9]<<8) + value[8];
        int z = (value[11]<<8) + value[10];
        return new Point3D(((x / SCALE) * -1), y / SCALE, ((z / SCALE)*-1));
    }
    private Point3D convertMag(byte[] value) {
        final float SCALE = (float) (32768 / 4912);
        if (value.length >= 18) {
            int x = (value[13]<<8) + value[12];
            int y = (value[15]<<8) + value[14];
            int z = (value[17]<<8) + value[16];
            return new Point3D(x / SCALE, y / SCALE, z / SCALE);
        }
        else return new Point3D(0,0,0);
    }
    private Point3D convertGyr(byte[] value) {
        final float SCALE = (float) 128.0;

        int x = (value[1]<<8) + value[0];
        int y = (value[3]<<8) + value[2];
        int z = (value[5]<<8) + value[4];
        return new Point3D(x / SCALE, y / SCALE, z / SCALE);
    }
}
