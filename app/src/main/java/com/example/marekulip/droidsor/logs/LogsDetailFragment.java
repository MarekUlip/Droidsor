package com.example.marekulip.droidsor.logs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.marekulip.droidsor.DroidsorSettingsFramgent;
import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.SenorDataItemsCountTable;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.example.marekulip.droidsor.gpxfileexporter.LogExporter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorLog;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsDetailFragment extends ListFragment {
    private static final String TAG = LogsDetailFragment.class.toString();
    private LogDetailArrayAdapter adapter;
    private List<LogDetailItem> items = new ArrayList<>();
    private long id = -1;
    private boolean isSelectionModeOn = false;
    private List<Integer> idList = new ArrayList<>();
    private SparseIntArray weights = new SparseIntArray();
    private int prefferedCount = 750;
    private Context context;
    private Activity mActivity;

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
        //long time = System.currentTimeMillis();
        //loadItems(id);
        //Log.d(TAG, "onActivityCreated: Without weights: "+(System.currentTimeMillis()-time));
        //items.clear();
        //time = System.currentTimeMillis();
        prefferedCount = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DroidsorSettingsFramgent.COUNT_OF_POINTS,"750"));
        loadItemsWithWeights(id);
        //Log.d(TAG, "onActivityCreatedd:  With weights: "+(System.currentTimeMillis()-time));

        //adapter = new LogDetailArrayAdapter(getContext(),R.layout.log_list_item,items);
        //getListView().setAdapter(adapter);
        //Log.d(TAG, "onActivityCreated: Showed graph"+(System.currentTimeMillis()-time));
        //Log.d(TAG, "onActivityCreated: ");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context.getApplicationContext();
        mActivity = getActivity();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        int type = items.get(position).sensorType;
        if(isSelectionModeOn){
            if(idList.contains(type)){
                idList.remove((Integer)type);
                v.setBackgroundColor(Color.TRANSPARENT);
            }else{
                Log.d(TAG, "onListItemClick: Adding sensor type "+type);
                idList.add(type);
                v.setBackgroundColor(Color.GRAY);
            }
        }else {
            Intent i = new Intent(getActivity(), LogDetailItemActivity.class);
            i.putExtra(LogDetailItemActivity.LOG_ID,this.id);
            i.putExtra(LogDetailItemActivity.SENSOR_ID,type);
            startActivity(i);
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
        if(item.getItemId()==Menu.FIRST){
            List<Integer> sensorTypes = new ArrayList<>();
            sensorTypes.add(items.get(info.position).sensorType);
            LogExporter.exportLog(getContext(),id,sensorTypes);
            //exportItems(info.position);
        }
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
        if(!idList.isEmpty())
            LogExporter.exportLog(getContext(),id,idList);//exportItems(-1);
    }

    private void loadItemsWithWeights(final long id){
        Cursor c = context.getContentResolver().query(DroidsorProvider.SENSOR_DATA_COUNT_URI,null, SenorDataItemsCountTable.LOG_ID+" = ?",new String[]{String.valueOf(id)},null);
        if(c!=null && c.moveToFirst()){
            weights.put(c.getInt(c.getColumnIndexOrThrow(SenorDataItemsCountTable.SENSOR_TYPE)),resolveWeight(c.getInt(c.getColumnIndexOrThrow(SenorDataItemsCountTable.COUNT_OF_ITEMS))));
            while (c.moveToNext()){
                weights.put(c.getInt(c.getColumnIndexOrThrow(SenorDataItemsCountTable.SENSOR_TYPE)),resolveWeight(c.getInt(c.getColumnIndexOrThrow(SenorDataItemsCountTable.COUNT_OF_ITEMS))));
            }
            c.close();
        }
        else {
            return;
        }
        getActivity().findViewById(R.id.list_fragment_progress_bar).setVisibility(View.VISIBLE);
        getActivity().findViewById(android.R.id.empty).setVisibility(View.GONE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(getActivity()==null)return;
                final List<EntryHolder> lst = new ArrayList<>();
                /*for(int i = 0, size = weights.size(), key, itemCount, index; i<size; i++){
                    key = weights.keyAt(i);
                    itemCount = SensorsEnum.resolveEnum(key).itemCount;
                    lst.add(new EntryHolder(key));
                    index = lst.size()-1;
                    switch (itemCount){
                        case 3: lst.get(index).entries.add(new ArrayList<Entry>());
                        case 2: lst.get(index).entries.add(new ArrayList<Entry>());
                        case 1: lst.get(index).entries.add(new ArrayList<Entry>());
                    }
                    loadSensorDataWithWeight(id,key,weights.valueAt(i),index,itemCount,lst);
                }*/
                loadItems();
                //prepItemsForGraph(lst);
                 /*try {
                     while(getActivity()==null){
                         Log.d(TAG, "run: sleeping");
                         Thread.sleep(100);
                     }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                 if(getActivity()==null)return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new LogDetailArrayAdapter(context,R.layout.log_list_item,items);
                        getListView().setAdapter(adapter);
                        getActivity().findViewById(R.id.list_fragment_progress_bar).setVisibility(View.GONE);
                    }
                });
            }
        }).start();

    }

    private void prepItemsForGraph(List<EntryHolder> lst){
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
                dataSet.setDrawValues(false);//TODO change maybe
                dataSets.add(dataSet);
            }
            items.add(new LogDetailItem(SensorsEnum.resolveEnum(lst.get(i).sensorType).getSensorName(context),SensorsEnum.resolveEnum(lst.get(i).sensorType).getSensorUnitName(getContext()),new LineData(dataSets),lst.get(i).sensorType,lst.get(i).labels));
        }
    }

    private void loadSensorDataWithWeight(long logId, int sensorType, int weight,int listPosition,int itemCount, final List<EntryHolder> lst){//TODO try to make only one query
        Cursor cursor = context.getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,SensorDataTable.LOG_ID + " = ? and "+SensorDataTable.SENSOR_TYPE+" = ? and "+SensorDataTable.SAMPLE_WEIGHT +" >= ?",new String[]{String.valueOf(logId),String.valueOf(sensorType),String.valueOf(weight)},null);
        if(cursor!=null && cursor.moveToFirst()){
            int size = lst.get(listPosition).entries.get(0).size();
            switch (itemCount){
                case 1: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    break;
                case 2: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    lst.get(listPosition).entries.get(1).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    break;
                case 3: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                    lst.get(listPosition).entries.get(1).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                    lst.get(listPosition).entries.get(2).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                    break;
            }
            lst.get(listPosition).labels.add(DateFormat.getTimeInstance().format(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            while (cursor.moveToNext()){
                size = lst.get(listPosition).entries.get(0).size();
                switch (itemCount){
                    case 1: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        break;
                    case 2: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        lst.get(listPosition).entries.get(1).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                        break;
                    case 3: lst.get(listPosition).entries.get(0).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X))));
                        lst.get(listPosition).entries.get(1).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y))));
                        lst.get(listPosition).entries.get(2).add(new Entry(size,cursor.getFloat(cursor.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))));
                        break;
                }
                lst.get(listPosition).labels.add(DateFormat.getTimeInstance().format(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)))));
            }
            cursor.close();
        }
    }

    private int resolveWeight(int count){
        //Log.d(TAG, "resolveWeight: "+count);
        if(count<prefferedCount)return 1;
        for(int i = 0; i< SensorLog.weights.length;i++){
            if(count/SensorLog.weights[i]>=70&&count/SensorLog.weights[i]<700)return SensorLog.weights[i];
        }
        return 1;
    }

    private void loadItems(){
        String where = SensorDataTable.LOG_ID + " = ? and (";
        where += makePlaceholders(weights.size());
        where +=")";
        String[] params = new String[weights.size()*2+1];
        params[0] = String.valueOf(id);
        makeParameters(weights,params);
        Cursor c = getContext().getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,where,params,null);
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
        prepItemsForGraph(lst);
    }

    private void loadItems(long id){
        Cursor c = getContext().getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,SensorDataTable.LOG_ID + " = ?",new String[]{String.valueOf(id)},null);
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
        prepItemsForGraph(lst);
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
     * Used for making placeholder parameters in SQLite select where clause IN is used
     * @param len number of placeholders
     * @return placeholder string
     */
    private String makePlaceholders(int len) {
        StringBuilder sb = new StringBuilder(len * 2 - 1);
        sb.append("(").append(SensorDataTable.SENSOR_TYPE).append(" = ? and ").append(SensorDataTable.SAMPLE_WEIGHT).append(" = ?)");
        for (int i = 1; i < len; i++) {
            sb.append(" or ").append("(").append(SensorDataTable.SENSOR_TYPE).append(" = ? and ").append(SensorDataTable.SAMPLE_WEIGHT).append(" = ?)");
        }
        return sb.toString();
    }
    private void makeParameters(SparseIntArray items, String[] params) {
        if (params.length < 2) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            for (int i = 1; i <= idList.size(); i++) {
                params[i] = String.valueOf(idList.get(i-1));
            }
            for(int i = 0, size = items.size(), key, index = 1; i<size; i++){
                key = items.keyAt(i);
                params[index++] = String.valueOf(key);
                params[index++] = String.valueOf(items.valueAt(i));
            }
        }
    }
}
