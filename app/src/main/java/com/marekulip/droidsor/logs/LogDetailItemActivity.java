package com.marekulip.droidsor.logs;

import android.arch.lifecycle.ViewModelProviders;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.SensorDataTable;
import com.marekulip.droidsor.gpxfileexporter.LogExporter;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.marekulip.droidsor.viewmodels.LogDetailViewModel;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity used to display detail of data from one sensor from the log
 */

public class LogDetailItemActivity extends AppCompatActivity {

    /**
     * Id of the log
     */
    private long logId;
    /**
     * Id of the sensor within the log
     */
    private int sensorId;
    public static final String SENSOR_ID = "sensor_id";
    public static final String LOG_ID = "log_id";
    /**
     * LogDetailItem for chart.
     */
    private LogDetailItem item;
    /**
     * Chart async task used to display progress
     */
    private LoadChartTask chartTask;

    /**
     * View model for this activity. It is the same model as with {@link LogsDetailFragment} class
     * which uses list and thus this class works with this view models list thought it will always
     * have only one item at this activity.
     */
    private LogDetailViewModel logDetailViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_detail_item);
        logId = getIntent().getLongExtra(LOG_ID,0);
        sensorId = getIntent().getIntExtra(SENSOR_ID,0);
        logDetailViewModel = ViewModelProviders.of(this).get(LogDetailViewModel.class);
        if(logDetailViewModel.getItems().isEmpty()){
            chartTask = new LoadChartTask();
            chartTask.execute();
        }
        else {
            item = logDetailViewModel.getItems().get(0);
            prepForCachedItems();
        }
    }
    private void prepForCachedItems(){
        setUpGraph();
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
        findViewById(R.id.textview_progress_bar_text).setVisibility(View.GONE);
        findViewById(R.id.log_chart).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.text_sensor_units)).setText(SensorsEnum.resolveEnum(sensorId).getSensorUnitName(LogDetailItemActivity.this));
        ((TextView)findViewById(R.id.text_sensor_name)).setText(SensorsEnum.resolveEnum(sensorId).getSensorName(LogDetailItemActivity.this));
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(chartTask!=null)chartTask.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            List<Integer> sensorTypes = new ArrayList<>();
            sensorTypes.add(sensorId);
            LogExporter.exportLog(this,logId,sensorTypes);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Prepares items so that they can be displayed in the chart
     * @param entryHolder holder for item
     */
    private void prepItemsForGraph(EntryHolder entryHolder){
        List<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet dataSet;
        String[] axisLabels = SensorsEnum.resolveEnum(entryHolder.sensorType).getDataDescriptions(this);
        int[] colors = {Color.RED,Color.BLUE,Color.GREEN};
        for(int j = 0; j<entryHolder.entries.size(); j++){
            //Create LineDataSet for every point.
            dataSet = new LineDataSet(entryHolder.entries.get(j),axisLabels[j]);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSet.setColor(colors[j]);
            dataSet.setDrawCircles(false);
            dataSets.add(dataSet);
        }
        item = new LogDetailItem(SensorsEnum.resolveEnum(entryHolder.sensorType).getSensorName(this),SensorsEnum.resolveEnum(entryHolder.sensorType).getSensorUnitName(this),new LineData(dataSets),entryHolder.sensorType,entryHolder.labels);
        logDetailViewModel.getItems().clear();
        logDetailViewModel.getItems().add(item);
    }

    /**
     * Prepares chart and sets items to it. This method should be called after the items were prepared in async task.
     */
    private void setUpGraph(){
        LineChart graphView =  findViewById(R.id.log_chart);
        graphView.setTouchEnabled(true);
        graphView.getDescription().setEnabled(false);
        // enable scaling and dragging
        graphView.setDragEnabled(true);
        graphView.setScaleEnabled(true);

        // set an alternative background color
        graphView.setBackgroundColor(Color.WHITE);
        graphView.getAxisRight().setEnabled(false);
        graphView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int val = (int) value;
                if (val<0)val = 0;
                if (val >= item.xLabels.size()) return "";
                return item.xLabels.get(val);
            }
        };

        XAxis xAxis = graphView.getXAxis();
        xAxis.setGranularity(countGranularity(item.xLabels.size())); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);


        graphView.setData(item.lineData);
        graphView.invalidate();
        graphView.zoom(4,0,0,0);
        graphView.setVisibleXRangeMaximum(item.xLabels.size());
    }

    /**
     * Counts granularity to be used in chart.
     * @param count Count of items on which granularity will be counted
     * @return estimated granularity value to be used
     */
    private float countGranularity(int count){
        if(count <= 10) return 1;
        else if(count <= 100) return 5;
        else if(count <= 1000) return 10;
        else return 25;
    }

    /**
     * Holder of all items to ease transport between. After it is filled it is used to create chart data.
     */
    private class EntryHolder{
        /**
         * Sensor type id
         */
        final int sensorType;
        /**
         * List containing lists. Each contained list represents one chart line.
         */
        final List<List<Entry>> entries = new ArrayList<>();
        /**
         * List of time labels All chart lines should have same time at the point so it is not necessary
         * to create list of lists here.
         */
        final List<String> labels = new ArrayList<>();
        EntryHolder(int sensorType){
            this.sensorType = sensorType;
        }
    }

    /**
     * AsyncTaks to load chart data and display progress while loading it.
     */
    private class LoadChartTask extends AsyncTask<Void, Integer, Void> {
        private ProgressBar progressBar;
        private TextView progressTextView;

        @Override
        protected Void doInBackground(Void... voids) {
            progressBar = findViewById(R.id.progress_bar);
            progressTextView = findViewById(R.id.textview_progress_bar_text);
            setupEntryHolder();

            return null;
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            if(progress[0]==100){
                progressTextView.setText(R.string.showing_data);
            }else {
                progressTextView.setText(R.string.processing_data);
            }
            progressBar.setProgress(progress[0]);
        }
        @Override
        protected void onPostExecute(Void voidRes) {
            setUpGraph();
            progressBar.setVisibility(View.GONE);
            progressTextView.setVisibility(View.GONE);
            findViewById(R.id.log_chart).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text_sensor_units)).setText(SensorsEnum.resolveEnum(sensorId).getSensorUnitName(LogDetailItemActivity.this));
            ((TextView)findViewById(R.id.text_sensor_name)).setText(SensorsEnum.resolveEnum(sensorId).getSensorName(LogDetailItemActivity.this));

        }

        /**
         * Prepares EntryHolder so it can be then used to setUpGraph
         */
        private void setupEntryHolder(){
            Cursor c = getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null, SensorDataTable.LOG_ID + " = ? and "+SensorDataTable.SENSOR_TYPE + " = ?",new String[]{String.valueOf(logId),String.valueOf(sensorId)},null);
            EntryHolder item = null;
            int itemCount;
            if(c!= null&& c.moveToFirst()){
                int progress = c.getCount()/100;
                if(progress<1) progress = 1;
                int type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
                item = new EntryHolder(sensorId);
                itemCount = SensorsEnum.resolveEnum(type).itemCount;
                //Determines how many chart lines should be drawn and
                switch (itemCount){
                    case 1: item.entries.add(new ArrayList<Entry>());
                        break;
                    case 2:
                        item.entries.add(new ArrayList<Entry>());
                        item.entries.add(new ArrayList<Entry>());
                        break;
                    case 3:
                        item.entries.add(new ArrayList<Entry>());
                        item.entries.add(new ArrayList<Entry>());
                        item.entries.add(new ArrayList<Entry>());
                }
                item.labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
                int size = item.entries.get(0).size();
                do{
                    if(size%progress == 0)publishProgress(size/progress);
                    switch (itemCount){//Add lines point
                        case 1: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            break;
                        case 2: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            item.entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                            break;
                        case 3: item.entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            item.entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                            item.entries.get(2).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                            break;
                    }
                    item.labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
                    size++;
                }while (c.moveToNext());
                c.close();
            }
            prepItemsForGraph(item);
        }
    }
}
