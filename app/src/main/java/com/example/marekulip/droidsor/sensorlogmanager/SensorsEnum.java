package com.example.marekulip.droidsor.sensorlogmanager;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.SparseArray;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt;
import com.example.marekulip.droidsor.database.SensorDataTable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Created by Fredred on 13.11.2017.
 */

public enum SensorsEnum {

    INTERNAL_ACCELEROMETER(Sensor.TYPE_ACCELEROMETER,R.string.accelerometer,R.string.meter_per_sec_square_unit,R.string.internal,R.string.accelerometer_xml,R.string.internal_xml,3),
    INTERNAL_MAGNETOMETER(Sensor.TYPE_MAGNETIC_FIELD,R.string.magnetometer,R.string.magnetometer_unit,R.string.internal,R.string.magnetometer_xml,R.string.internal_xml,3),
    INTERNAL_GYROSCOPE(Sensor.TYPE_GYROSCOPE,R.string.gyroscope,R.string.gyroscope_unit,R.string.internal,R.string.gyroscope_xml,R.string.internal_xml,3),
    INTERNAL_LIGHT(Sensor.TYPE_LIGHT,R.string.optical,R.string.optical_unit,R.string.internal,R.string.optical_xml,R.string.internal_xml,1),
    INTERNAL_BAROMETER(Sensor.TYPE_PRESSURE,R.string.barometer,R.string.barometer_unit,R.string.internal,R.string.barometer_xml,R.string.internal_xml,1),
    INTERNAL_GRAVITY(Sensor.TYPE_GRAVITY,R.string.gravity,R.string.meter_per_sec_square_unit,R.string.internal,R.string.gravity_xml,R.string.internal_xml,3),
    INTERNAL_TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE,R.string.thermometer,R.string.celsius_degree_unit,R.string.internal,R.string.thermometer_xml,R.string.internal_xml,1),
    INTERNAL_HUMIDITY(Sensor.TYPE_RELATIVE_HUMIDITY,R.string.humidity,R.string.humidity_unit,R.string.internal,R.string.humidity_xml,R.string.internal_xml,1),
    INTERNAL_ORIENTATION(98,R.string.orientation,R.string.radian,R.string.internal,R.string.orientation_xml,R.string.internal_xml,3){
        //98 is used because Sensor.TYPE_ORIENTATION is deprecated so i wanted to avoid wrong type
        @Override
        public void resolveSensor(List<SensorData> sensorDataList, float[] data){
            sensorDataList.add(new SensorData(sensorType,new Point3D(data[0],data[1],data[2]),SensorData.getTime()));
        }
    },
    EXT_MOV_ACCELEROMETER(100,R.string.accelerometer,R.string.meter_per_sec_square_unit,R.string.external,R.string.accelerometer_xml,R.string.external_xml, 3),
    EXT_MOV_GYROSCOPE(101,R.string.gyroscope,R.string.degrees_per_second,R.string.external,R.string.gyroscope_xml,R.string.external_xml,3),
    EXT_MOV_MAGNETIC(102,R.string.magnetometer,R.string.magnetometer_unit,R.string.external,R.string.magnetometer_xml,R.string.external_xml,3),
    EXT_HUMIDITY(103,R.string.humidity,R.string.humidity_unit,R.string.external,R.string.humidity_xml,R.string.external_xml,1),
    EXT_TEMPERATURE(104,R.string.thermometer,R.string.celsius_degree_unit,R.string.external,R.string.thermometer_xml,R.string.external_xml,3){
        @Override
        protected String getSensorUnitName(Context context, int position) {
            if(position == 2) return context.getString(R.string.fahrenheit_degree_unit);
            return super.getSensorUnitName(context);
        }
    },
    EXT_OPTICAL(105,R.string.optical,R.string.optical_unit,R.string.external,R.string.optical_xml,R.string.external_xml,1),
    EXT_BAROMETER(106,R.string.barometer,R.string.barometer_unit,R.string.external,R.string.barometer_xml,R.string.external_xml,1),
    EXT_MOVEMENT(107,R.string.movement,R.string.meter_per_sec_square_unit,R.string.external,R.string.movement_xml,R.string.external_xml,1);


    public int sensorType;
    public int itemCount;
    private String sensorName = null;
    private String[] sensorNameXmlFriendly = null;
    private int sensorNameRes;
    private String sensorUnitName = null;
    private int sensorNameResXml;
    private int sensorUnitNameRes;
    private String sPositionString = null;
    private int sPositionStringRes;
    private int sPositionStringResXml;
    private DecimalFormat decimalFormat;


    private static final SparseArray<SensorsEnum> map = new SparseArray<>(SensorsEnum.values().length);

    static {
        for(SensorsEnum item: values()) map.put(item.sensorType,item);
    }

    SensorsEnum(int sensorType, int sensorNameRes, int sensorUnitNameRes, int positionRes, int sensorNameResXml, int sPositionStringResXml, int itemCount){
        this.sensorType = sensorType;
        this.sensorNameRes = sensorNameRes;
        this.sensorUnitNameRes = sensorUnitNameRes;
        this.sPositionStringRes = positionRes;
        this.itemCount = itemCount;
        this.sensorNameResXml = sensorNameResXml;
        this.sPositionStringResXml = sPositionStringResXml;
        decimalFormat =  new DecimalFormat("##.##");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
    }

    public static SensorsEnum resolveEnum(int sensorType){
        return map.get(sensorType,null);
    }

    public String getSensorName(Context context){
        if(sensorName == null && context!=null){
            sensorName = context.getString(sensorNameRes)+ " " + context.getString(sPositionStringRes);
        }
        return sensorName;
    }

    public String[] getSensorNamesXmlFriendly(Context context){
        if(sensorNameXmlFriendly == null && context!=null){
            sensorNameXmlFriendly = new String[2];
            sensorNameXmlFriendly[0] = context.getString(sensorNameResXml)+ " data_source=\"" + context.getString(sPositionStringResXml) +"\"";
            sensorNameXmlFriendly[1] = context.getString(sensorNameResXml);
        }
        return sensorNameXmlFriendly;
    }

    public String getSensorUnitName(Context context){
        if(sensorUnitName == null && context!=null){
            sensorUnitName = context.getString(sensorUnitNameRes);
        }
        return sensorUnitName;
    }

    protected String getSensorUnitName(Context context,int position){
        return getSensorUnitName(context);
    }



    private StringBuilder allValuesString(Point3D data, Context c, StringBuilder sb){
        return twoValuesString(data,c,sb).append("Z: ").append(decimalFormat.format(data.z)).append(getSensorUnitName(c,3)).append(System.lineSeparator());
    }

    private StringBuilder twoValuesString(Point3D data, Context c, StringBuilder sb){
        return oneValueString(data,c,sb).append("Y: ").append(decimalFormat.format(data.y)).append(getSensorUnitName(c,2)).append(System.lineSeparator());
    }

    private StringBuilder oneValueString(Point3D data, Context c, StringBuilder sb){
       return sb.append("X: ").append(decimalFormat.format(data.x)).append(getSensorUnitName(c,1)).append(System.lineSeparator());
    }

    public String getStringData(Context c, Point3D data){
        switch (itemCount){
            case 1:
                return oneValueString(data,c,new StringBuilder()).toString();
            case 2:
                return twoValuesString(data,c,new StringBuilder()).toString();
            case 3:
                return allValuesString(data,c,new StringBuilder()).toString();
            default:
                return "Unsupported";
        }
    }

    public Point3D getPointFromRow(Cursor c){
        switch (itemCount){
            case 1: return new Point3D(c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                    0.0,0.0);
            case 2: return new Point3D(c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                    0.0);
            case 3: return new Point3D(c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z)));
            default: return new Point3D(0.0,0.0,0.0);
        }
    }

    public static int resolveSensor(UUID sensorId){
        if(sensorId.equals(SensorTagGatt.UUID_MOV_DATA))return EXT_MOVEMENT.sensorType;
        if(sensorId.equals(SensorTagGatt.UUID_HUM_DATA))return EXT_HUMIDITY.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_IRT_DATA))return EXT_TEMPERATURE.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_OPT_DATA))return EXT_OPTICAL.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_BAR_DATA))return EXT_BAROMETER.sensorType;
        else return -1;
    }

    public static Point3D resolveSensor(SensorEvent event){
        SensorsEnum sensor = resolveEnum(event.sensor.getType());
        if(sensor == null)return  new Point3D(0.0,0.0,0.0);
        switch (sensor.itemCount){
            case 1: return new Point3D(event.values[0],
                    0.0,0.0);
            case 2: return new Point3D(event.values[0],
                    event.values[1],
                    0.0);
            case 3: return new Point3D(event.values[0],
                    event.values[1],
                    event.values[2]);
            default: return new Point3D(0.0,0.0,0.0);
        }
    }

    public static void resolveSensor(SensorEvent event, List<SensorData> sensorDataList){
        sensorDataList.add(new SensorData(event.sensor.getType(),resolveSensor(event),SensorData.getTime()));
    }

    public void resolveSensor(List<SensorData> sensorDataList, float[] data){
        throw new UnsupportedOperationException("Error: this method should be overriden by enums which requires this method.");
    }
}
