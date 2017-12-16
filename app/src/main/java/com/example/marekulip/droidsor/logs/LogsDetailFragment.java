package com.example.marekulip.droidsor.logs;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsDetailFragment extends ListFragment {
    private static final String TAG = LogsDetailFragment.class.toString();
    private LogDetailArrayAdapter adapter;
    private List<LogDetailItem> items = new ArrayList<>();
    private int id;
    private boolean isSelectionModeOn = false;
    private List<Integer> idList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
        id = getArguments().getInt("id");
        //showLogs();
        loadItems(id);
        adapter = new LogDetailArrayAdapter(getContext(),R.layout.log_list_item,items);
        getListView().setAdapter(adapter);
        Log.d(TAG, "onActivityCreated: ");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(isSelectionModeOn){
            v.setBackgroundColor(Color.GRAY);
            int type = items.get(position).sensorType;
            Log.d(TAG, "onListItemClick: Adding sensor type "+type);
            idList.add(type);
        }else {
            startActivity(new Intent(getActivity(), LogDetailActivity.class));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,Menu.FIRST,0,getString(R.string.export_sensor));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        if(item.getItemId()==Menu.FIRST)exportItems(info.position);
        //switch ()
        return super.onContextItemSelected(item);
    }

    public void setSelectionMode(boolean isSelectionModeOn){
        if(isSelectionModeOn){
            idList.clear();
            adapter.setSelectedIds(idList);
        }
        adapter.setSelectionModeOn(isSelectionModeOn);
        this.isSelectionModeOn = isSelectionModeOn;
        adapter.notifyDataSetChanged();
    }

    public void exportSelected(){
        if(!idList.isEmpty()) exportItems(-1);
    }

    private void loadItems(int id){
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(SensorDataTable.TABLE_NAME,null,SensorDataTable.LOG_ID + " = ?",new String[]{String.valueOf(id)},null,null,null);
        List<EntryHolder> lst = new ArrayList<>();
        int itemCount;
        if(c!= null&& c.moveToFirst()){
            Log.d(TAG, "loadItems: starting");
            int type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
            lst.add(new EntryHolder(type));
            //lst.get(0).entries.add(new ArrayList<Entry>());
            itemCount = SensorsEnum.resolveEnum(type).itemCount;

            switch (itemCount){

                case 1: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    break;
                case 2:
                    lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    break;
                case 3:
                    lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(2).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));

                /*case 3: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(0).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                case 2: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(1).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                case 1: lst.get(0).entries.add(new ArrayList<Entry>());
                    lst.get(0).entries.get(2).add(new Entry(0,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));*/
            }
            lst.get(0).labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            //lst.get(0).entries.add(new Entry(time,value));
            int position;
            int size;
            while (c.moveToNext()){
                type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
                itemCount = SensorsEnum.resolveEnum(type).itemCount;
                for(position = 0; position<lst.size();position++){
                    if(lst.get(position).sensorType==type)break;
                }
                if(position==lst.size()){
                    lst.add(new EntryHolder(type));
                    position = lst.size()-1;
                    switch (itemCount){
                        case 3: lst.get(position).entries.add(new ArrayList<Entry>());
                        case 2: lst.get(position).entries.add(new ArrayList<Entry>());
                        case 1: lst.get(position).entries.add(new ArrayList<Entry>());
                    }
                }
                size = lst.get(position).entries.get(0).size();
                switch (itemCount){
                    case 1: lst.get(position).entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            break;
                    case 2: lst.get(position).entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            lst.get(position).entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                            break;
                    case 3: lst.get(position).entries.get(0).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                            lst.get(position).entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                            lst.get(position).entries.get(2).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                            break;
                            //case 2: lst.get(position).entries.get(1).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    //case 1: lst.get(position).entries.get(2).add(new Entry(size,c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                }
                lst.get(position).labels.add(DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            }
            Log.d(TAG, "loadItems: closing");
            c.close();
        }
        database.close();
        dbHelper.close();
        List<ILineDataSet> dataSets;
        LineDataSet dataSet;
        String[] axisLabels = {"X", "Y", "Z"};
        int[] colors = {Color.RED,Color.BLUE,Color.GREEN};
        for(int i = 0;i< lst.size();i++){
            dataSets = new ArrayList<>();
            for(int j = 0; j<lst.get(i).entries.size(); j++){
                dataSet = new LineDataSet(lst.get(i).entries.get(j),axisLabels[j]);
                dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                dataSet.setColor(colors[j]);
                dataSet.setDrawCircles(false);
                dataSets.add(dataSet);
            }
            items.add(new LogDetailItem(SensorsEnum.resolveEnum(lst.get(i).sensorType).getSensorName(getContext()),SensorsEnum.resolveEnum(lst.get(i).sensorType).getSensorUnitName(getContext()),new LineData(dataSets),lst.get(i).sensorType,lst.get(i).labels));
        }
    }

    private class EntryHolder{
        int sensorType;
        List<List<Entry>> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        EntryHolder(int sensorType){
            this.sensorType = sensorType;
        }
    }



    /**
     * Export one or more items.
     * @param pos position of one selected item in list. Is used only when selection mode is off.
     */
    private void exportItems(final int pos){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(),R.string.started_exporting,Toast.LENGTH_LONG).show();
                List<SensorData> data = new ArrayList<>();
                int sensorType = -1;
                if(!isSelectionModeOn)sensorType = items.get(pos).sensorType;
                SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
                SQLiteDatabase database = dbHelper.getReadableDatabase();
                Cursor c;
                if(!isSelectionModeOn) c = database.query(SensorDataTable.TABLE_NAME,null,SensorDataTable.LOG_ID+ " = ? AND "+SensorDataTable.SENSOR_TYPE+" = ?",new String[]{String.valueOf(id),String.valueOf(sensorType)},null,null,null);
                else{
                    String[] params= new String[idList.size()+1];
                    params[0] = String.valueOf(id);
                    makeParameters(params);
                    //new String[]{String.valueOf(id),String.valueOf(sensorType)};
                    c = database.query(SensorDataTable.TABLE_NAME,null,SensorDataTable.LOG_ID+ " = ? AND "+SensorDataTable.SENSOR_TYPE+" IN ("+makePlaceholders(idList.size())+")",params,null,null,null);
                }

                if(c!=null && c.moveToFirst()) {
                    data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                            ,new Point3D(
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                    ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
                    ));
                    while (c.moveToNext()) {
                        data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                                ,new Point3D(
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                        ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
                        ));
                    }
                    c.close();
                    GPXExporter.exportLogItems(data, id + " Sensor " + sensorType + " " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()), getContext());
                    Toast.makeText(getContext(), R.string.exporting_done, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Used for making placeholder parameters in SQLite select where clause IN is used
     * @param len number of placeholders
     * @return placeholder string
     */
    private String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
    private void makeParameters(String[] params) {
        if (params.length < 2) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            for (int i = 1; i <= idList.size(); i++) {
                params[i] = String.valueOf(idList.get(i-1));
            }
        }
    }
}
