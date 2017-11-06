package com.example.marekulip.droidsor.grapview;

/**
 * Created by Fredred on 03.09.2017.
 */

public class Entry {
    //private int xValue;
    private String xLabel;
    private float yValue;

    public Entry (String xValue, float yValue){
        xLabel = xValue;
        this.yValue = yValue;
    }

    public Entry(int xValue,int yValue){
        this(String.valueOf(xValue),yValue);
    }

    public float getyValue(){
        return yValue;
    }

    public String getXValue(){
       return xLabel;
        // return xLabel != null ? xLabel : String.valueOf(xValue);
    }
}
