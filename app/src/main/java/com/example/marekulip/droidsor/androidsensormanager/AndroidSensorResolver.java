package com.example.marekulip.droidsor.androidsensormanager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;

/**
 * Created by Fredred on 22.10.2017.
 */

public class AndroidSensorResolver {

    public static Point3D resolveSensor(SensorEvent event){
        int sensorType = event.sensor.getType();
        switch (sensorType){
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_PROXIMITY:
                return new Point3D(event.values[0],0,0);
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_GYROSCOPE:
                return new Point3D(event.values[0],event.values[1],event.values[2]);
            default: return new Point3D(0,0,0);
        }

    }

    public static void resolveSensor(SensorEvent event, SensorDataPackage dataPackage){
        dataPackage.getDatas().add(new SensorData(resolveSensor(event),SensorData.getTime()));
        dataPackage.getSensorTypes().add(event.sensor.getType());
    }
}
