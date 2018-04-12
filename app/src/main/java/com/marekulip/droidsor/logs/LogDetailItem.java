package com.marekulip.droidsor.logs;

import com.github.mikephil.charting.data.LineData;

import java.util.List;

/**
 * Class representing sensor from log designed to be used in chart or to carry data for chart
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailItem {
    public LineData lineData;
    /**
     * Time labels fo x axis
     */
    public List<String> xLabels;
    /**
     * Name of the sensor representing this item
     */
    public String sensorName;
    /**
     * Physical unit name for Y axis
     */
    public String yLabel;
    /**
     * Id of sensor type
     */
    public int sensorType;

    public LogDetailItem(String sensorName, String yLabel, LineData lineData, int sensorType, List<String> xLabels){
        this.sensorName = sensorName;
        this.lineData = lineData;
        this.yLabel = yLabel;
        this.sensorType = sensorType;
        this.xLabels = xLabels;
    }
}
