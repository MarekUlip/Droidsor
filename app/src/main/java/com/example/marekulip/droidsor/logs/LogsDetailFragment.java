package com.example.marekulip.droidsor.logs;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.example.marekulip.droidsor.grapview.Entry;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;

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
        startActivity(new Intent(getActivity(),LogDetailActivity.class));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,Menu.FIRST,0,"Export this sensor");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        if(item.getItemId()==Menu.FIRST)exportItems(info.position);
        //switch ()
        return super.onContextItemSelected(item);
    }

    private void loadItems(int id){
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(SensorDataTable.TABLE_NAME,null,SensorDataTable.LOG_ID + " = ?",new String[]{String.valueOf(id)},null,null,null);
        List<EntryHolder> lst = new ArrayList<>();
        if(c!= null&& c.moveToFirst()){
            int type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
            lst.add(new EntryHolder(type));
            float value = c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X));
            String time =  DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG))));//Calendar.
            lst.get(0).entries.add(new Entry(time,value));
            int position;
            while (c.moveToNext()){
                type = c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE));
                for(position = 0; position<lst.size();position++){
                    if(lst.get(position).sensorType==type)break;
                }
                if(position==lst.size()){
                    lst.add(new EntryHolder(type));
                    position = lst.size()-1;
                }
                value = c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X));
                time =  DateFormat.getTimeInstance().format(new Date(c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG))));//Calendar.
                lst.get(position).entries.add(new Entry(time,value));
            }
            c.close();
        }
        database.close();
        dbHelper.close();
        Log.d(TAG, "loadItems: "+lst.size());

        for(int i = 0;i< lst.size();i++){
            items.add(new LogDetailItem("Sensor "+lst.get(i).sensorType,"measures",lst.get(i).entries,lst.get(i).sensorType));
        }
    }

    private class EntryHolder{
        int sensorType;
        List<Entry> entries = new ArrayList<>();
        public EntryHolder(int sensorType){
            this.sensorType = sensorType;
        }
    }

    private void exportItems(int position){
        List<SensorData> data = new ArrayList<>();
        int sensorType = items.get(position).sensorType;
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(SensorDataTable.TABLE_NAME,null,SensorDataTable.LOG_ID+ " = ? AND "+SensorDataTable.SENSOR_TYPE+" = ?",new String[]{String.valueOf(id),String.valueOf(sensorType)},null,null,null);
        if(c!=null && c.moveToFirst()){
            data.add(new SensorData(new Point3D(
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
            ),c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                    c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
                    ));
            while (c.moveToNext()){
                data.add(new SensorData(new Point3D(
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                ),c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                        c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
                ));
            }
            c.close();
            GPXExporter.exportLogItems(data,id+" Sensor "+sensorType+" "+DateFormat.getDateTimeInstance().format(System.currentTimeMillis()), getContext());
        }
    }

    /*public void showLogs(){
        items.clear();
        List<Entry> lst = new ArrayList<>();
        for(int i = 0; i< 100;i++){
            lst.add(new Entry("12:45:55",(int)(Math.random()*50)));
        }
        items.add(new LogDetailItem("Accelerometer","m/s",lst));

        List<Entry> lst2 = new ArrayList<>();
        for(int i = 0; i< 100;i++){
            lst2.add(new Entry("12:45:55",(int)(Math.random()*50)));
        }
        items.add(new LogDetailItem("BLE Accelerometer","m/s",lst2));

        lst = new ArrayList<>();
        for(int i = 0; i< 1000;i++){
            lst.add(new Entry("12:45:55",(int)(Math.random()*50)));
        }
        items.add(new LogDetailItem("Humidity","%rH",lst));

        lst = new ArrayList<>();
        for(int i = 0; i< 100;i++){
            lst.add(new Entry("12:45:55",(int)(Math.random()*50)));
        }
        items.add(new LogDetailItem("BLE Barometer","mBar",lst));



    }*/
}
