package com.example.marekulip.droidsor.bluetoothsensormanager.tisensor;

import java.util.List;

/**
 * Created by Marek Ulip on 03-Oct-17.
 */

public enum BarometerCalibrationCoefficients {
    INSTANCE;
    volatile public List<Integer> barometerCalibrationCoefficients;
    volatile public double heightCalibration;
}
