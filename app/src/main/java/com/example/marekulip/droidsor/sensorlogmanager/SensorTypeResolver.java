package com.example.marekulip.droidsor.sensorlogmanager;

import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt;

import java.util.UUID;

/**
 * Created by Fredred on 25.08.2017.
 */

public class SensorTypeResolver {
    public static final int EXT_MOV_ACC = 100;
    public static final int EXT_MOV_GYRO = 101;
    public static final int EXT_MOV_MAG = 102;
    private static final int EXT_HUM = 103;
    private static final int EXT_TMP = 104;
    private static final int EXT_OPT = 105;
    private static final int EXT_BAR = 106;
    private static final int EXT_MOV = 107;

    public static int resolveSensor(UUID sensorId){
        if(sensorId.equals(SensorTagGatt.UUID_MOV_DATA))return EXT_MOV;
        if(sensorId.equals(SensorTagGatt.UUID_HUM_DATA))return EXT_HUM;
        else if(sensorId.equals(SensorTagGatt.UUID_IRT_DATA))return EXT_TMP;
        else if(sensorId.equals(SensorTagGatt.UUID_OPT_DATA))return EXT_OPT;
        else if(sensorId.equals(SensorTagGatt.UUID_BAR_DATA))return EXT_BAR;
        else return -1;
    }





}
