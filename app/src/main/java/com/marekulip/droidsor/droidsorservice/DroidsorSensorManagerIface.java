package com.marekulip.droidsor.droidsorservice;

import android.util.SparseIntArray;

import java.util.List;

public interface DroidsorSensorManagerIface {
    int defaultSensorFrequency = 500;
    void setSensorsToListen(SparseIntArray sensors);
    void getListenedSensorTypes(List<Integer> sensors);
    void startListening();
    void stopListening();
    void getAllAvailableSensorTypes(List<Integer> sensors);
}
