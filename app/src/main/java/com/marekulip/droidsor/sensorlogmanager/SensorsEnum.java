package com.marekulip.droidsor.sensorlogmanager;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.SparseArray;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt;
import com.marekulip.droidsor.database.SensorDataTable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Enum used to represent all supported sensors.
 * Created by Marek Ulip on 13.11.2017.
 */

public enum SensorsEnum {

    /**
     * Represents accelerometer from smartphone. Units are in m/s2. Can process 3 values x,y,z.
     */
    INTERNAL_ACCELEROMETER(Sensor.TYPE_ACCELEROMETER,R.string.accelerometer,R.string.meter_per_sec_square_unit,R.string.internal,R.string.accelerometer_xml,R.string.internal_xml,3),
    /**
     * Represents magnetometer from smartphone. Units are in uT. Can process 3 values x,y,z.
     */
    INTERNAL_MAGNETOMETER(Sensor.TYPE_MAGNETIC_FIELD,R.string.magnetometer,R.string.magnetometer_unit,R.string.internal,R.string.magnetometer_xml,R.string.internal_xml,3),
    /**
     * Represents gyroscope from smartphone. Units are in rad/s. Can process 3 values x,y,z.
     */
    INTERNAL_GYROSCOPE(Sensor.TYPE_GYROSCOPE,R.string.gyroscope,R.string.gyroscope_unit,R.string.internal,R.string.gyroscope_xml,R.string.internal_xml,3),
    /**
     * Represents light sensor from smartphone. Units are in Lux. Can process 1 value x.
     */
    INTERNAL_LIGHT(Sensor.TYPE_LIGHT,R.string.optical,R.string.optical_unit,R.string.internal,R.string.optical_xml,R.string.internal_xml,1),
    /**
     * Represents barometer from smartphone. Units are in mBar. Can process 1 value x.
     */
    INTERNAL_BAROMETER(Sensor.TYPE_PRESSURE,R.string.barometer,R.string.barometer_unit,R.string.internal,R.string.barometer_xml,R.string.internal_xml,1),
    /**
     * Represents gravity sensor from smartphone. Units are in m/s2. Can process 3 values x,y,z.
     */
    INTERNAL_GRAVITY(Sensor.TYPE_GRAVITY,R.string.gravity,R.string.meter_per_sec_square_unit,R.string.internal,R.string.gravity_xml,R.string.internal_xml,3),
    /**
     * Represents temperature from smartphone. Units are in Celsius degrees. Can process 1 value x.
     */
    INTERNAL_TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE,R.string.thermometer,R.string.celsius_degree_unit,R.string.internal,R.string.thermometer_xml,R.string.internal_xml,1),
    /**
     * Represents humidity sensor from smartphone. Units are in %rH. Can process 1 value x.
     */
    INTERNAL_HUMIDITY(Sensor.TYPE_RELATIVE_HUMIDITY,R.string.humidity,R.string.humidity_unit,R.string.internal,R.string.humidity_xml,R.string.internal_xml,1),
    /**
     * Represents orientation sensor from smartphone. Units are in radians. Can process 3 values x,y,z.
     */
    INTERNAL_ORIENTATION(98,R.string.orientation,R.string.radian,R.string.internal,R.string.orientation_xml,R.string.internal_xml,3){
        //98 is used because Sensor.TYPE_ORIENTATION is deprecated so i wanted to avoid wrong type
        @Override
        public void resolveSensor(List<SensorData> sensorDataList, float[] data){
            sensorDataList.add(new SensorData(sensorType,new Point3D(data[0],data[1],data[2]),SensorData.getTime()));
        }
    },
    /**
     * Represents accelerometer from BLE device. Units are in m/s2. Can process 3 values x,y,z.
     */
    EXT_MOV_ACCELEROMETER(100,R.string.accelerometer,R.string.meter_per_sec_square_unit,R.string.external,R.string.accelerometer_xml,R.string.external_xml, 3),
    /**
     * Represents gyroscope from BLE device. Units are in degrees/s. Can process 3 values x,y,z.
     */
    EXT_MOV_GYROSCOPE(101,R.string.gyroscope,R.string.degrees_per_second,R.string.external,R.string.gyroscope_xml,R.string.external_xml,3),
    /**
     * Represents magnetometer from BLE device. Units are in uT. Can process 3 values x,y,z.
     */
    EXT_MOV_MAGNETIC(102,R.string.magnetometer,R.string.magnetometer_unit,R.string.external,R.string.magnetometer_xml,R.string.external_xml,3),
    /**
     * Represents humidity sensor from BLE device. Units are in %rH. Can process 1 value x.
     */
    EXT_HUMIDITY(103,R.string.humidity,R.string.humidity_unit,R.string.external,R.string.humidity_xml,R.string.external_xml,1),
    /**
     * Represents temperature from BLE device. Units are in degrees of Celsius and Fahrenheits. Can process 3 values x,y,z.
     */
    EXT_TEMPERATURE(104,R.string.thermometer,R.string.celsius_degree_unit,R.string.external,R.string.thermometer_xml,R.string.external_xml,3){
        @Override
        protected String getSensorUnitName(Context context, int position) {
            if(position == 2) return context.getString(R.string.fahrenheit_degree_unit);
            return super.getSensorUnitName(context);
        }
    },
    /**
     * Represents optical sensor from BLE device. Units are in Lux. Can process 1 value x.
     */
    EXT_OPTICAL(105,R.string.optical,R.string.optical_unit,R.string.external,R.string.optical_xml,R.string.external_xml,1),
    /**
     * Represents barometer from BLE device. Units are in mBar. Can process 1 value x.
     */
    EXT_BAROMETER(106,R.string.barometer,R.string.barometer_unit,R.string.external,R.string.barometer_xml,R.string.external_xml,1),
    /**
     * Represents movement from BLE device. Units are in m/s2. Can process 1 value x. This enum should not be used
     * for processing because in BLE device it contains another 3 sensors. It is enum so profiles are easier to make
     * and BLE device is easier to get going.
     */
    EXT_MOVEMENT(107,R.string.movement,R.string.meter_per_sec_square_unit,R.string.external,R.string.movement_xml,R.string.external_xml,1);


    /**
     * Sensor type id of this sensor
     */
    public int sensorType;
    /**
     * Count of axises this sensor was designed for
     */
    public int itemCount;
    /**
     * Name of the sensor loaded from Android resources
     */
    private String sensorName = null;
    /**
     * Xml names for GPX exporting loaded from Android resources
     */
    private String[] sensorNameXmlFriendly = null;
    /**
     * Android resource id for sensor name
     */
    private int sensorNameRes;
    /**
     * String for sensor unit
     */
    private String sensorUnitName = null;
    /**
     * Android resource id for sensor name which is xml friendly
     */
    private int sensorNameResXml;
    /**
     * Android resource id for sensor physical unit
     */
    private int sensorUnitNameRes;
    /**
     * Android resource id for sensor position - Internal or external
     */
    private int sPositionStringRes;
    /**
     * Android resource id for sensor position which is xml friendly
     */
    private int sPositionStringResXml;
    /**
     * Decimal format used so that data from sensor don't display unnecessary numbers behind decimal point.
     */
    private DecimalFormat decimalFormat;


    /**
     * Sparse array used for resolving sensor type ids to actual SensorEnums
     */
    private static final SparseArray<SensorsEnum> map = new SparseArray<>(SensorsEnum.values().length);

    static {
        for(SensorsEnum item: values()) map.put(item.sensorType,item);
    }

    /**
     * Enum constructor.
     * @param sensorType Sensor type id
     * @param sensorNameRes Android resource id for sensor name
     * @param sensorUnitNameRes Android resource id for physical unit of this sensor
     * @param positionRes Android resource id for sensor position
     * @param sensorNameResXml Android resource id for xml friendly sensor name
     * @param sPositionStringResXml Android resource id for xml friendly sensor position
     * @param itemCount count of axises this sensor was designed for.
     */
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

    /**
     * Resloves provides sensor type id into actual SensorEnum
     * @param sensorType
     * @return return enum of relevant sensor or null if provided id is not supported
     */
    public static SensorsEnum resolveEnum(int sensorType){
        return map.get(sensorType,null);
    }

    /**
     * Get sensor name based on system language
     * @param context Context that will be used to load sensor name string resource
     * @return sensor name in best suiting language
     */
    public String getSensorName(Context context){
        if(sensorName == null && context!=null){
            sensorName = context.getString(sensorNameRes)+ " " + context.getString(sPositionStringRes);
        }
        return sensorName;
    }

    /**
     * Returns array to be used in GPX exporting
     * @param context Context which will load strings from resources if it has not been loaded before
     * @return xml friendly strings
     */
    public String[] getSensorNamesXmlFriendly(Context context){
        if(sensorNameXmlFriendly == null && context!=null){
            sensorNameXmlFriendly = new String[2];
            sensorNameXmlFriendly[0] = context.getString(sensorNameResXml)+ " data_source=\"" + context.getString(sPositionStringResXml) +"\"";
            sensorNameXmlFriendly[1] = context.getString(sensorNameResXml);
        }
        return sensorNameXmlFriendly;
    }

    /**
     * Returns sensor unit string
     * @param context context to be used to load resource
     * @return sensor physical unit string
     */
    public String getSensorUnitName(Context context){
        if(sensorUnitName == null && context!=null){
            sensorUnitName = context.getString(sensorUnitNameRes);
        }
        return sensorUnitName;
    }

    /**
     * Special case of {@link #getSensorUnitName(Context)} used with sensors that provides sensor data in different physical
     * units such as temperature sensor providing data in Celsius and Fahrenheits. This method should
     * be overridden for proper behaviour. If not overridden it behaves as if {@link #getSensorUnitName(Context)} was called.
     * @param context Context that will load string resources
     * @param position position of this data segment
     * @return returns correct sensor physical unit name
     */
    protected String getSensorUnitName(Context context,int position){
        return getSensorUnitName(context);
    }


    /**
     * Formats provided data into three axises
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder threeValuesString(Point3D data, Context c, StringBuilder sb){
        return twoValuesString(data,c,sb).append("Z: ").append(decimalFormat.format(data.z)).append(getSensorUnitName(c,3)).append(System.lineSeparator());
    }

    /**
     * Formats provided data into two axises
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder twoValuesString(Point3D data, Context c, StringBuilder sb){
        return oneValueString(data,c,sb).append("Y: ").append(decimalFormat.format(data.y)).append(getSensorUnitName(c,2)).append(System.lineSeparator());
    }

    /**
     * Formats provided data into one axis
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder oneValueString(Point3D data, Context c, StringBuilder sb){
       return sb.append("X: ").append(decimalFormat.format(data.x)).append(getSensorUnitName(c,1)).append(System.lineSeparator());
    }

    /**
     * Creates string with sensor data based on axis count of this sensor
     * @param c Context with which missing resources will be loaded
     * @param data data to be processed
     * @return Returns string with sensor data based on axis count of this sensor
     */
    public String getStringData(Context c, Point3D data){
        switch (itemCount){
            case 1:
                return oneValueString(data,c,new StringBuilder()).toString();
            case 2:
                return twoValuesString(data,c,new StringBuilder()).toString();
            case 3:
                return threeValuesString(data,c,new StringBuilder()).toString();
            default:
                return "Unsupported";
        }
    }

    /**
     * Method for getting {@link Point3D} objects from cursor. In the end it was not used because of
     * possible performance issues - To call this method, sensor id would have to be resolved and since
     * cursor is usually processed in while cycle it would add another operations and would slow senor data
     * loading. It is usable with cursors containing small amount of rows.
     * @param c Cursor from which {@link Point3D} should be created.
     * @return {@link Point3D} object with sensor data.
     */
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

    /**
     * Resolves external sensor based on provided UUID. Note that this method only returns
     * enum's sensor type id and not entire SensorEnum. Use {@link #resolveEnum(int)} to get sensor enum.
     * @param sensorId UUID to be resolved
     * @return Sensor type id of external sensor.
     */
    public static int resolveSensor(UUID sensorId){
        if(sensorId.equals(SensorTagGatt.UUID_MOV_DATA))return EXT_MOVEMENT.sensorType;
        if(sensorId.equals(SensorTagGatt.UUID_HUM_DATA))return EXT_HUMIDITY.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_IRT_DATA))return EXT_TEMPERATURE.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_OPT_DATA))return EXT_OPTICAL.sensorType;
        else if(sensorId.equals(SensorTagGatt.UUID_BAR_DATA))return EXT_BAROMETER.sensorType;
        else return -1;
    }

    /**
     * Processes Android SensorEvent and returns {@link Point3D} object with sensor data. Sensor type id is found automatically.
     * @param event Sensor event to be processed.
     * @return {@link Point3D} object containing sensor data.
     */
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

    /**
     * Processes Android SensorEvent and adds its result into provided list as {@link SensorData} object.
     * @param event Sensor event to be processed.
     * @param sensorDataList List to which the result should be added
     */
    public static void resolveSensor(SensorEvent event, List<SensorData> sensorDataList){
        sensorDataList.add(new SensorData(event.sensor.getType(),resolveSensor(event),SensorData.getTime()));
    }

    /**
     * Works similarly to {@link #resolveSensor(SensorEvent, List)} method only this method processes float array instead of SensorEvent.
     * This method used for processing data from sensors that provide data in form of float array. For example emulated internal orientation sensor.
     * This method should be overridden before using. Otherwise it throws {@link UnsupportedOperationException} exception
     * @param sensorDataList list to which result should be added
     * @param data Data to process
     */
    public void resolveSensor(List<SensorData> sensorDataList, float[] data){
        throw new UnsupportedOperationException("Error: this method should be overridden by enums which requires this method.");
    }
}
