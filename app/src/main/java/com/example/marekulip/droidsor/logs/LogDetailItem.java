package com.example.marekulip.droidsor.logs;

import com.github.mikephil.charting.data.LineData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailItem {
    //public List<Entry> graphItems = new ArrayList<>();
    //public List<ILineDataSet> graphItems;
    public LineData lineData;
    public List<String> xLabels = new ArrayList<>();
    public String sensorName;
    public String yLabel;
    public int sensorType;

    public LogDetailItem(String sensorName, String yLabel, LineData lineData, int sensorType, List<String> xLabels){
        this.sensorName = sensorName;
        //this.graphItems = graphItems;
        this.lineData = lineData;
        this.yLabel = yLabel;
        this.sensorType = sensorType;
        this.xLabels = xLabels;
    }
}
