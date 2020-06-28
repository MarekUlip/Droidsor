package com.marekulip.droidsor.sensorlogmanager;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import androidx.core.os.ConfigurationCompat;

import com.marekulip.droidsor.DroidsorSettingsFramgent;
import com.marekulip.droidsor.R;
import com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt;
import com.marekulip.droidsor.database.SensorDataTable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Enum used to represent all supported sensors.
 * Created by Marek Ulip on 13.11.2017.
 */

public enum SensorsEnum {

    /**
     * Represents accelerometer from smartphone. Units are in m/s2. Can process 3 values x,y,z.
     */
    INTERNAL_ACCELEROMETER(Sensor.TYPE_ACCELEROMETER,true,R.string.accelerometer,R.string.meter_per_sec_square_unit,R.string.accelerometer_xml,3),
    /**
     * Represents magnetometer from smartphone. Units are in uT. Can process 3 values x,y,z.
     */
    INTERNAL_MAGNETOMETER(Sensor.TYPE_MAGNETIC_FIELD,true,R.string.magnetometer,R.string.magnetometer_unit,R.string.magnetometer_xml,3),
    /**
     * Represents gyroscope from smartphone. Units are in rad/s. Can process 3 values x,y,z.
     */
    INTERNAL_GYROSCOPE(Sensor.TYPE_GYROSCOPE,true,R.string.gyroscope,R.string.gyroscope_unit,R.string.gyroscope_xml,3),
    /**
     * Represents light sensor from smartphone. Units are in Lux. Can process 1 value x.
     */
    INTERNAL_LIGHT(Sensor.TYPE_LIGHT,true,R.string.optical,R.string.optical_unit,R.string.optical_xml,R.array.optical_data_desc,1),
    /**
     * Represents barometer from smartphone. Units are in mBar. Can process 1 value x.
     */
    INTERNAL_BAROMETER(Sensor.TYPE_PRESSURE,true,R.string.barometer,R.string.barometer_unit,R.string.barometer_xml,R.array.pressure_data_desc,1),
    /**
     * Represents gravity sensor from smartphone. Units are in m/s2. Can process 3 values x,y,z.
     */
    INTERNAL_GRAVITY(Sensor.TYPE_GRAVITY,true,R.string.gravity,R.string.meter_per_sec_square_unit,R.string.gravity_xml,3),
    /**
     * Represents temperature from smartphone. Units are in Celsius degrees. Can process 1 value x.
     */
    INTERNAL_TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE,true,R.string.thermometer,R.string.celsius_degree_unit,R.string.thermometer_xml,R.array.temperature_data_desc,1),
    /**
     * Represents humidity sensor from smartphone. Units are in %rH. Can process 1 value x.
     */
    INTERNAL_HUMIDITY(Sensor.TYPE_RELATIVE_HUMIDITY,true,R.string.humidity,R.string.humidity_unit,R.string.humidity_xml,R.array.humidity_data_desc,1),
    /**
     * Represents orientation sensor from smartphone. Units are in radians. Can process 3 values x,y,z.
     */
    INTERNAL_ORIENTATION(98,true,R.string.orientation,R.string.radian,R.string.orientation_xml,3){
        //98 is used because Sensor.TYPE_ORIENTATION is deprecated so i wanted to avoid wrong type
        @Override
        public void resolveSensor(List<SensorData> sensorDataList, float[] data){
            sensorDataList.add(new SensorData(sensorType,new Point3D(data[0],data[1],data[2]),SensorData.getTime(),true));
        }
    },
    /**
     * Represents GPS as a sensors. Contains longitude, latitude and altitude.
     */
    GPS(97,true,R.string.gps,R.string.degrees,R.string.gps_xml, R.array.gps_data_desc,3){
        @Override
        public String getSensorUnitName(Context context, int position) {
            if(position == 3){
                return "m";
            }
            return getSensorUnitName(context);
        }
    },
    /**
     * Represents microphone from smartphone. Units are in dB and returns only 1 value
     */
    INTERNAL_MICROPHONE(201,true,R.string.microphone,R.string.decibels,R.string.microphone_xml,1),
    /**
     * Represents battery from smartphone. Units are Celsius for temeprature and percentages for level
     */
    INTERNAL_BATTERY(202,true,R.string.battery,R.string.celsius_degree_unit,R.string.battery_xml,R.array.battery_desc,2){
        @Override
        public String getSensorUnitName(Context context, int position) {
            if(position == 2){
                return "%";
            }
            return getSensorUnitName(context);
        }
    },
    /**
     * Represents accelerometer from BLE device. Units are in m/s2. Can process 3 values x,y,z.
     */
    EXT_MOV_ACCELEROMETER(100,false,R.string.accelerometer,R.string.gravity_accel_unit,R.string.accelerometer_xml, 3),
    /**
     * Represents gyroscope from BLE device. Units are in degrees/s. Can process 3 values x,y,z.
     */
    EXT_MOV_GYROSCOPE(101,false,R.string.gyroscope,R.string.degrees_per_second,R.string.gyroscope_xml,3),
    /**
     * Represents magnetometer from BLE device. Units are in uT. Can process 3 values x,y,z.
     */
    EXT_MOV_MAGNETIC(102,false,R.string.magnetometer,R.string.magnetometer_unit,R.string.magnetometer_xml,3),
    /**
     * Represents humidity sensor from BLE device. Units are in %rH. Can process 1 value x.
     */
    EXT_HUMIDITY(103,false,R.string.humidity,R.string.humidity_unit,R.string.humidity_xml,R.array.humidity_data_desc,1),
    /**
     * Represents temperature from BLE device. Units are in degrees of Celsius and Fahrenheits. Can process 3 values x,y,z.
     */
    EXT_TEMPERATURE(104,false,R.string.thermometer,R.string.celsius_degree_unit,R.string.thermometer_xml,R.array.tmp007_data_desc,2),
    /**
     * Represents optical sensor from BLE device. Units are in Lux. Can process 1 value x.
     */
    EXT_OPTICAL(105,false,R.string.optical,R.string.optical_unit,R.string.optical_xml,R.array.optical_data_desc,1),
    /**
     * Represents barometer from BLE device. Units are in mBar. Can process 1 value x.
     */
    EXT_BAROMETER(106,false,R.string.barometer,R.string.barometer_unit,R.string.barometer_xml,R.array.pressure_data_desc,1),
    /**
     * Represents movement from BLE device. Units are in m/s2. Can process 1 value x. This enum should not be used
     * for processing because in BLE device it contains another 3 sensors. It is enum so profiles are easier to make
     * and BLE device is easier to get going.
     */
    EXT_MOVEMENT(107,false,R.string.movement,R.string.meter_per_sec_square_unit,R.string.movement_xml,1);


    /**
     * Sensor type id of this sensor
     */
    public int sensorType;
    /**
     * Count of axises this sensor was designed for
     */
    public int itemCount;

    /**
     * Indicates whether this sensor is used in mobile devices.
     */
    public boolean isInternal;
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
    private NumberFormat decimalFormat;

    /**
     * Android resource id for string array containing data descriptions.
     */
    private int dataDescriptionsRes;

    /**
     * Sensor data descriptions i.e. X,Y,Z for senors that measure three axises.
     */
    private String[] dataDescriptions;

    /**
     * Maximum number of numbers behind decimal point.
     */
    private int maxFracDecimalCount = -1;


    /**
     * Sparse array used for resolving sensor type ids to actual SensorEnums
     */
    private static final SparseArray<SensorsEnum> map = new SparseArray<>(SensorsEnum.values().length);

    static {
        for(SensorsEnum item: values()) map.put(item.sensorType,item);
    }

    /**
     * Enum constructor to set specific data descriptions.
     * @param sensorType Sensor type id
     * @param isInternal Indicates whether this sensor is used in mobile devices.
     * @param sensorNameRes Android resource id for sensor name
     * @param sensorUnitNameRes Android resource id for physical unit of this sensor
     * @param sensorNameResXml Android resource id for xml friendly sensor name
     * @param itemCount count of axises this sensor was designed for.
     */
    SensorsEnum(int sensorType, boolean isInternal, int sensorNameRes, int sensorUnitNameRes, int sensorNameResXml, int dataDescriptionsRes,int itemCount){
        this.sensorType = sensorType;
        this.sensorNameRes = sensorNameRes;
        this.sensorUnitNameRes = sensorUnitNameRes;
        this.itemCount = itemCount;
        this.sensorNameResXml = sensorNameResXml;
        this.isInternal = isInternal;
        if(isInternal){
            sPositionStringRes = R.string.internal;
            sPositionStringResXml = R.string.internal_xml;
        } else {
            sPositionStringRes = R.string.external;
            sPositionStringResXml = R.string.external_xml;
        }

        decimalFormat = new DecimalFormat("00.00");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        this.dataDescriptionsRes = dataDescriptionsRes;
    }

    /**
     * Enum basic constructor.
     * @param sensorType Sensor type id
     * @param isInternal Indicates whether this sensor is used in mobile devices.
     * @param sensorNameRes Android resource id for sensor name
     * @param sensorUnitNameRes Android resource id for physical unit of this sensor
     * @param sensorNameResXml Android resource id for xml friendly sensor name
     * @param itemCount count of axises this sensor was designed for.
     */
    SensorsEnum(int sensorType, boolean isInternal,int sensorNameRes, int sensorUnitNameRes, int sensorNameResXml, int itemCount){
        this(sensorType,isInternal,sensorNameRes,sensorUnitNameRes,sensorNameResXml,0,itemCount);
        dataDescriptions = new String[]{"X: ","Y: ","Z: "};
    }

    /**
     * Resloves provides sensor type id into actual SensorEnum
     * @param sensorType sensor type id to resolve
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
    public String getSensorUnitName(Context context,int position){
        return getSensorUnitName(context);
    }

    /**
     * Returns string array describing meaning of sensor data
     * @param context context to be used to load resource if necessary
     * @return string array
     */
    public String[] getDataDescriptions(Context context){
        if(dataDescriptions == null){
            dataDescriptions = context.getResources().getStringArray(dataDescriptionsRes);
        }
        return dataDescriptions;
    }


    /**
     * Formats provided data into three axises
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder threeValuesString(Point3D data, Context c, StringBuilder sb){
        return twoValuesString(data,c,sb).append(getDataDescriptions(c)[2]).append(decimalFormat.format(data.z)).append(getSensorUnitName(c,3)).append(System.lineSeparator());

    }

    /**
     * Formats provided data into two axises
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder twoValuesString(Point3D data, Context c, StringBuilder sb){
        return oneValueString(data,c,sb).append(getDataDescriptions(c)[1]).append(decimalFormat.format(data.y)).append(getSensorUnitName(c,2)).append(System.lineSeparator());
    }

    /**
     * Formats provided data into one axis
     * @param data Data to be formatted
     * @param c Context that will be used to load sensor physical unit.
     * @param sb String builder which will be returned after formatting
     * @return StringBuilder containing formatted string
     */
    private StringBuilder oneValueString(Point3D data, Context c, StringBuilder sb){
       return sb.append(getDataDescriptions(c)[0]).append(decimalFormat.format(data.x)).append(getSensorUnitName(c,1)).append(System.lineSeparator());
    }

    private void initLocalNumberFormat(Context c){
        decimalFormat = NumberFormat.getInstance(ConfigurationCompat.getLocales(c.getResources().getConfiguration()).get(0));
        decimalFormat.setMaximumFractionDigits(maxFracDecimalCount);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
    }

    /**
     * Creates string with sensor data based on axis count of this sensor
     * @param c Context with which missing resources will be loaded
     * @param data data to be processed
     * @return Returns string with sensor data based on axis count of this sensor
     */
    public String getStringData(Context c, Point3D data){
        if(maxFracDecimalCount == -1) {
            maxFracDecimalCount = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(c).getString(DroidsorSettingsFramgent.MAX_NUM_OF_DECIMALS, "2")));
            initLocalNumberFormat(c);
        }
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
        sensorDataList.add(new SensorData(event.sensor.getType(),resolveSensor(event),SensorData.getTime(),true));
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
