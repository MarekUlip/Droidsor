package com.example.marekulip.droidsor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseArray;

import com.example.marekulip.droidsor.adapters.SensorDataDispArrAdapter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marek Ulip on 19-Sep-17.
 */

public class SensorDataDispListFragment extends ListFragment {
    private static final String TAG = SensorDataDispListFragment.class.toString();
    private SensorDataDispArrAdapter adapter;
    private List<SensorItem> items = new ArrayList<>();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
        adapter = new SensorDataDispArrAdapter(getContext(),R.layout.sensor_data_displayer_list_item,items);
        setListAdapter(adapter);
        Log.d(TAG, "onActivityCreated: ");
    }

    public void setSensorsToShow(List<Integer> sensorTypes){
        items.clear();
        SensorsEnum sensor;
        for(Integer i: sensorTypes){
            sensor = SensorsEnum.resolveEnum(i);
            Log.d(TAG, "setSensorsToShow: "+i);
            items.add(new SensorItem(sensor.getSensorName(getContext()),sensor.getStringData(getContext(),Point3D.getDefaultPoint3D()),i));
        }
        adapter.notifyDataSetChanged();
    }

    public synchronized void setNewData(ArrayDeque<SensorData> sensorDataQueue){
        SparseArray<SensorData> map = new SparseArray<>();
        SensorData sensorData;
        while(!sensorDataQueue.isEmpty()){
            //Log.d(TAG, "setNewData: ");
            sensorData = sensorDataQueue.pop(); //TODO solve no such element exception
            map.put(sensorData.sensorType,sensorData);
        }

        for(int i = 0, size = map.size(), key; i < size; i++) {
            for(SensorItem item: items){
                key = map.keyAt(i);
                if(item.sensorType == key){
                    item.sensorValue = SensorsEnum.resolveEnum(key).getStringData(getContext(),map.valueAt(i).values);
                    break;
                }
            }
        }
        adapter.notifyDataSetChanged();
        //TODO possible errors requires testing
        /*Map<Integer,SensorData> map = new HashMap<>();
        SensorData sensorData;
        while(!sensorDataQueue.isEmpty()){
            //Log.d(TAG, "setNewData: ");
            sensorData = sensorDataQueue.pop();
            map.put(sensorData.sensorType,sensorData);
        }
        boolean wasFound;

        keyLooper:
        for(Integer i: map.keySet()){
            for(SensorItem item: items){
                //Log.d(TAG, "setNewData: "+item.sensorType);
                if (item.sensorType == i){
                    //Log.d(TAG, "setNewData: Found id"+i);
                    item.sensorValue = SensorsEnum.resolveEnum(i).getStringData(getContext(),map.get(i).values);
                    continue keyLooper;
                }
            }
            //items.add(new SensorItem(SensorsEnum.resolveEnum(i).getSensorName(getContext()),SensorsEnum.resolveEnum(i).getStringData(getContext(),map.get(i).values),i));
        }
        adapter.notifyDataSetChanged();*/
    }

    public void setNewData(List<Integer> sensorTypes, SparseArray<SensorData> newData){
        for(int i = 0, size = sensorTypes.size(), key; i < size; i++) {
            for(SensorItem item: items){
                key = sensorTypes.get(i);
                if(item.sensorType == key){
                    item.sensorValue = SensorsEnum.resolveEnum(key).getStringData(getContext(),newData.get(key).values);
                    break;
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

}
