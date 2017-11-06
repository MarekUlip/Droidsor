package com.example.marekulip.droidsor.logs;

import com.example.marekulip.droidsor.grapview.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailItem {
    public List<Entry> graphItems = new ArrayList<>();
    public String sensorName;
    public String yLabel;
    public int sensorType;

    public LogDetailItem(String sensorName, String yLabel, List<Entry> graphItems, int sensorType){
        this.sensorName = sensorName;
        this.graphItems = graphItems;
        this.yLabel = yLabel;
        this.sensorType = sensorType;
    }
}
