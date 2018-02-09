package com.example.marekulip.droidsor.logs;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.gpxfileexporter.LogExporter;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogDetailItemActivity extends AppCompatActivity {

    private long logId;
    private int sensorId;
    public static final String SENSOR_ID = "sensor_id";
    public static final String LOG_ID = "log_id";
    private LogDetailItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_detail_item);
        logId = getIntent().getLongExtra(LOG_ID,0);
        sensorId = getIntent().getIntExtra(SENSOR_ID,0);

        prepGUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            List<Integer> sensorTypes = new ArrayList<>();
            sensorTypes.add(sensorId);
            LogExporter.exportLog(this,logId,sensorTypes);
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepGUI(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                setItem();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUpGraph();
                        findViewById(R.id.progressBar2).setVisibility(View.GONE);
                        findViewById(R.id.log_chart).setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void setItem(){
        Cursor c = getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null, SensorDataTable.LOG_ID + " = ? and "+SensorDataTable.SENSOR_TYPE + " = ?",new String[]{String.valueOf(logId),String.valueOf(sensorId)},null);
        EntryHolder item = null;
        int itemCount;
        if(c!= null&& c.moveToFirst()){
            int type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
            item = new EntryHolder(sensorId);
            //lst.get(0).entries.add(new ArrayList<Entry>());
            itemCount = SensorsEnum.resolveEnum(type).itemCount;

            switch (itemCount){

                case 1: item.entries.add(new ArrayList<Entry>());
                    item.entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    break;
                case 2:
                    item.entries.add(new ArrayList<Entry>());
                    item.entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    item.entries.add(new ArrayList<Entry>());
                    item.entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    break;
                case 3:
                    item.entries.add(new ArrayList<Entry>());
                    item.entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    item.entries.add(new ArrayList<Entry>());
                    item.entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    item.entries.add(new ArrayList<Entry>());
                    item.entries.get(2).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));

                /*case 3: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                case 2: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                case 1: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(2).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));*/
            }
            item.labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            //lst.get(0).entries.add(new Entry(time,value));
            int position;
            int size = item.entries.get(0).size();
            while (c.moveToNext()){
                size++;
                switch (itemCount){
                    case 1: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        break;
                    case 2: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        item.entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                        break;
                    case 3: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        item.entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                        item.entries.get(2).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                        break;
                    //case 2: lst.get(position).entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    //case 1: lst.get(position).entries.get(2).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                }
                item.labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            }
            c.close();
        }
        prepItemsForGraph(item);
    }

    private void prepItemsForGraph(EntryHolder entryHolder){
        List<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet dataSet;
        String[] axisLabels = {"X", "Y", "Z"};
        int[] colors = {Color.RED,Color.BLUE,Color.GREEN};
        for(int j = 0; j<entryHolder.entries.size(); j++){
            dataSet = new LineDataSet(entryHolder.entries.get(j),axisLabels[j]);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSet.setColor(colors[j]);
            dataSet.setDrawCircles(false);
            dataSets.add(dataSet);
        }
        item = new LogDetailItem(SensorsEnum.resolveEnum(entryHolder.sensorType).getSensorName(this),SensorsEnum.resolveEnum(entryHolder.sensorType).getSensorUnitName(this),new LineData(dataSets),entryHolder.sensorType,entryHolder.labels);

    }

    private void setUpGraph(){
        LineChart graphView =  findViewById(R.id.log_chart);
        graphView.setTouchEnabled(true);
        graphView.getDescription().setEnabled(false);
        // enable scaling and dragging
        graphView.setDragEnabled(true);
        graphView.setScaleEnabled(true);
        graphView.setPinchZoom(true);

        // set an alternative background color
        graphView.setBackgroundColor(Color.WHITE);
        graphView.getAxisRight().setEnabled(false);
        graphView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if ((int) value > item.xLabels.size()) return "";
                return item.xLabels.get((int) value);
            }
        };

        XAxis xAxis = graphView.getXAxis();
        xAxis.setGranularity(50f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);


        graphView.setData(item.lineData);
        graphView.invalidate();

        graphView.setVisibleXRangeMaximum(120);
    }

    private class EntryHolder{
        int sensorType;
        List<List<Entry>> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        EntryHolder(int sensorType){
            this.sensorType = sensorType;
        }
    }
}
