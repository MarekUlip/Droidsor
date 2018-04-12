/**************************************************************************************************
 Filename:       Sensor.java
 Revised:        $Date: 2013-08-30 11:44:31 +0200 (fr, 30 aug 2013) $
 Revision:       $Revision: 599e5650a33a4a142d060c959561f9e9b0d88146$

 Copyright (c) 2013 - 2014 Texas Instruments Incorporated

 All rights reserved not granted herein.
 Limited License.

 Texas Instruments Incorporated grants a world-wide, royalty-free,
 non-exclusive license under copyrights and patents it now or hereafter
 owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
 this software subject to the terms herein.  With respect to the foregoing patent
 license, such license is granted  solely to the extent that any such patent is necessary
 to Utilize the software alone.  The patent license shall not apply to any combinations which
 include this software, other than combinations with devices manufactured by or for TI ('TI Devices').
 No hardware patent is licensed hereunder.

 Redistributions must preserve existing copyright notices and reproduce this license (including the
 above copyright notice and the disclaimer and (if applicable) source code license limitations below)
 in the documentation and/or other materials provided with the distribution

 Redistribution and use in binary form, without modification, are permitted provided that the following
 conditions are met:

 * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
 software provided in binary form.
 * any redistribution and use are licensed by TI for use only with TI Devices.
 * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

 If software source code is provided to you, modification and redistribution of the source code are permitted
 provided that the following conditions are met:

 * any redistribution and use of the source code, including any resulting derivative works, are licensed by
 TI for use only with TI Devices.
 * any redistribution and use of any object code compiled from the source code and any resulting derivative
 works, are licensed by TI for use only with TI Devices.

 Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
 promote products derived from this software without specific prior written permission.

 DISCLAIMER.

 THIS SOFTWARE IS PROVIDED BY TI AND TI'S LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL TI AND TI'S LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
/**
 * This class has been modified for use with Droidsor.
 */
package com.marekulip.droidsor.bluetoothsensormanager.tisensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.marekulip.droidsor.sensorlogmanager.Point3D;

import java.util.UUID;

import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_ACC_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_ACC_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_ACC_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_GYR_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_GYR_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_GYR_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_HUM_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_HUM_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_HUM_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_IRT_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_IRT_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_IRT_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MAG_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MAG_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MAG_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MOV_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MOV_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_MOV_SERV;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_OPT_CONF;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_OPT_DATA;
import static com.marekulip.droidsor.bluetoothsensormanager.tisensor.SensorTagGatt.UUID_OPT_SERV;
import static java.lang.Math.pow;


/**
 * This enum encapsulates the differences amongst the sensors. The differences include UUID values and how to interpret the
 * characteristic-containing-measurement.
 */
public enum TISensor {
	IR_TEMPERATURE(UUID_IRT_SERV, UUID_IRT_DATA, UUID_IRT_CONF) {
		@Override
		public Point3D convert(final byte [] value) {

			/*
			 * The IR Temperature sensor produces two measurements; Object ( AKA target or IR) Temperature, and Ambient ( AKA die ) temperature.
			 * Both need some conversion, and Object temperature is dependent on Ambient temperature.
			 * They are stored as [ObjLSB, ObjMSB, AmbLSB, AmbMSB] (4 bytes) Which means we need to shift the bytes around to get the correct values.
			 */

			double ambient = extractAmbientTemperature(value);
			double target = extractTargetTemperature(value, ambient);
			double targetNewSensor = extractTargetTemperatureTMP007(value);
			return new Point3D(ambient, target, targetNewSensor);
		}

		private double extractAmbientTemperature(byte [] v) {
			int offset = 2;
			return shortUnsignedAtOffset(v, offset) / 128.0;
		}

		private double extractTargetTemperature(byte [] v, double ambient) {
			Integer twoByteValue = shortSignedAtOffset(v, 0);

			double Vobj2 = twoByteValue.doubleValue();
			Vobj2 *= 0.00000015625;

			double Tdie = ambient + 273.15;

			double S0 = 5.593E-14; // Calibration factor
			double a1 = 1.75E-3;
			double a2 = -1.678E-5;
			double b0 = -2.94E-5;
			double b1 = -5.7E-7;
			double b2 = 4.63E-9;
			double c2 = 13.4;
			double Tref = 298.15;
			double S = S0 * (1 + a1 * (Tdie - Tref) + a2 * pow((Tdie - Tref), 2));
			double Vos = b0 + b1 * (Tdie - Tref) + b2 * pow((Tdie - Tref), 2);
			double fObj = (Vobj2 - Vos) + c2 * pow((Vobj2 - Vos), 2);
			double tObj = pow(pow(Tdie, 4) + (fObj / S), .25);

			return tObj - 273.15;
		}
		private double extractTargetTemperatureTMP007(byte [] v) {
			int offset = 0;
			return shortUnsignedAtOffset(v, offset) / 128.0;
		}
	},

	MOVEMENT_ACC(UUID_MOV_SERV,UUID_MOV_DATA, UUID_MOV_CONF,(byte)3) {
		@Override
		public Point3D convert(final byte[] value) {
			// Range 8G
			final float SCALE = (float) 4096.0;

			int x = (value[7]<<8) + value[6];
			int y = (value[9]<<8) + value[8];
			int z = (value[11]<<8) + value[10]; 
			return new Point3D(((x / SCALE) * -1), y / SCALE, ((z / SCALE)*-1));
		}
	},
	MOVEMENT_GYRO(UUID_MOV_SERV,UUID_MOV_DATA, UUID_MOV_CONF,(byte)3) {
		@Override
		public Point3D convert(final byte[] value) {

			final float SCALE = (float) 128.0;

			int x = (value[1]<<8) + value[0];
			int y = (value[3]<<8) + value[2];
			int z = (value[5]<<8) + value[4]; 
			return new Point3D(x / SCALE, y / SCALE, z / SCALE);
		}
	},
	MOVEMENT_MAG(UUID_MOV_SERV,UUID_MOV_DATA, UUID_MOV_CONF,(byte)3) {
		@Override
		public Point3D convert(final byte[] value) {
			final float SCALE = (float) (32768 / 4912);
			if (value.length >= 18) {
				int x = (value[13]<<8) + value[12];
				int y = (value[15]<<8) + value[14];
				int z = (value[17]<<8) + value[16]; 
				return new Point3D(x / SCALE, y / SCALE, z / SCALE);
			}
			else return new Point3D(0,0,0);
		}
	},
	ACCELEROMETER(UUID_ACC_SERV, UUID_ACC_DATA, UUID_ACC_CONF,(byte)3) {
		@Override
		public Point3D convert(final byte[] value) {
			/*
			 * The accelerometer has the range [-2g, 2g] with unit (1/64)g.
			 * To convert from unit (1/64)g to unit g we divide by 64.
			 * (g = 9.81 m/s^2)
			 * The z value is multiplied with -1 to coincide with how we have arbitrarily defined the positive y direction. (illustrated by the apps accelerometer
			 * image)
			 */


			// Range 8G
			final float SCALE = (float) 4096.0;

			int x = (value[0]<<8) + value[1];
			int y = (value[2]<<8) + value[3];
			int z = (value[4]<<8) + value[5];
			return new Point3D(x / SCALE, y / SCALE, z / SCALE);


		}
	},

	HUMIDITY(UUID_HUM_SERV, UUID_HUM_DATA, UUID_HUM_CONF) {
		@Override
		public Point3D convert(final byte[] value) {
			int a = shortUnsignedAtOffset(value, 2);
			// bits [1..0] are status bits and need to be cleared according
			// to the user guide, but the iOS code doesn't bother. It should
			// have minimal impact.
			//a = a - (a % 4);

			//return new Point3D((-6f) + 125f * (a / 65535f), 0, 0);
			return new Point3D(((double)a / 65536f)*100, 0, 0);
		}
	},

	MAGNETOMETER(UUID_MAG_SERV, UUID_MAG_DATA, UUID_MAG_CONF) {
		@Override
		public Point3D convert(final byte [] value) {
			// Multiply x and y with -1 so that the values correspond with the image in the app
			float x = shortSignedAtOffset(value, 0) * (2000f / 65536f) * -1;
			float y = shortSignedAtOffset(value, 2) * (2000f / 65536f) * -1;
			float z = shortSignedAtOffset(value, 4) * (2000f / 65536f);

			//return new Point3D(x - mcal.x, y - mcal.y, z - mcal.z);
			return new Point3D(x,y,z);
		}
	},

	LUXOMETER(UUID_OPT_SERV, UUID_OPT_DATA, UUID_OPT_CONF) {
		@Override
		public Point3D convert(final byte [] value) {
			int mantissa;
			int exponent;
			Integer sfloat= shortUnsignedAtOffset(value, 0);

			mantissa = sfloat & 0x0FFF;
			exponent = (sfloat >> 12) & 0xFF;

			double output;
			double magnitude = pow(2.0f, exponent);
			output = (mantissa * magnitude);

			return new Point3D(output / 100.0f, 0, 0);
		}
	},

	GYROSCOPE(UUID_GYR_SERV, UUID_GYR_DATA, UUID_GYR_CONF, (byte)7) {
		@Override
		public Point3D convert(final byte [] value) {

			float y = shortSignedAtOffset(value, 0) * (500f / 65536f) * -1;
			float x = shortSignedAtOffset(value, 2) * (500f / 65536f);
			float z = shortSignedAtOffset(value, 4) * (500f / 65536f);

			return new Point3D(x,y,z);      
		}
	},

	BAROMETER(SensorTagGatt.UUID_BAR_SERV, SensorTagGatt.UUID_BAR_DATA, SensorTagGatt.UUID_BAR_CONF) {
		@Override
		public Point3D convert(final byte [] value) {
			if (value.length > 4) {
				//Integer val = twentyFourBitUnsignedAtOffset(value, 3);
				Integer val = ((value[3] & 0xff) + ((value[4]<<8) & 0xff00) + ((value[5]<<16) & 0xff0000));
				return new Point3D((double) val / 100.0, 0, 0);
			}
			else {
				int mantissa;
				int exponent;
				Integer sfloat = shortUnsignedAtOffset(value, 2);

				mantissa = sfloat & 0x0FFF;
				exponent = (sfloat >> 12) & 0xFF;

				double output;
				double magnitude = pow(2.0f, exponent);
				output = (mantissa * magnitude);
				return new Point3D(output / 100.0f, 0, 0);
			}
		}
	};

	/**
	 * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's complement values as LSB MSB, which cannot be directly parsed
	 * as getIntValue(FORMAT_SINT16, offset) because the bytes are stored as little-endian.
	 * 
	 * This function extracts these 16 bit two's complement values.
	 * */
	private static Integer shortSignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = (int) c[offset] & 0xFF;
		Integer upperByte = (int) c[offset+1]; // // Interpret MSB as signed
		return (upperByte << 8) + lowerByte;
	}

	private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = (int) c[offset] & 0xFF;
		Integer upperByte = (int) c[offset+1] & 0xFF;
		return (upperByte << 8) + lowerByte;
	}
    private static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer mediumByte = (int) c[offset+1] & 0xFF;
        Integer upperByte = (int) c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

	public void onCharacteristicChanged(BluetoothGattCharacteristic c) {
		throw new UnsupportedOperationException("Error: the individual enum classes are supposed to override this method.");
	}


	public Point3D convert(byte[] value) {
		throw new UnsupportedOperationException("Error: the individual enum classes are supposed to override this method.");
	}

	private final UUID service, data, config;
	private byte enableCode; // See getEnableSensorCode for explanation.
	public static final byte DISABLE_SENSOR_CODE = 0;
	public static final byte ENABLE_SENSOR_CODE = 1;
	public static final byte CALIBRATE_SENSOR_CODE = 2;

	/**
	 * Constructor called by the Gyroscope and Accelerometer because it more than a boolean enable
	 * code.
	 */
	TISensor(UUID service, UUID data, UUID config, byte enableCode) {
		this.service = service;
		this.data = data;
		this.config = config;
		this.enableCode = enableCode;
	}

	/**
	 * Constructor called by all the sensors except Gyroscope
	 * */
	TISensor(UUID service, UUID data, UUID config) {
		this.service = service;
		this.data = data;
		this.config = config;
		this.enableCode = ENABLE_SENSOR_CODE; // This is the sensor enable code for all sensors except the gyroscope
	}

	/**
	 * @return the code which, when written to the configuration characteristic, turns on the sensor.
	 * */
	public byte getEnableSensorCode() {
		return enableCode;
	}

	public UUID getService() {
		return service;
	}

	public UUID getData() {
		return data;
	}

	public UUID getConfig() {
		return config;
	}

}