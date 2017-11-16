package com.example.marekulip.droidsor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.marekulip.droidsor.adapters.SensorDataDispArrAdapter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;
import com.example.marekulip.droidsor.sensorlogmanager.SensorDataPackage;
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
            items.add(new SensorItem(sensor.getSensorName(getContext()),sensor.getStringData(getContext(),Point3D.getDefaultPoint3D()),i));
        }
    }

    /*public void setNewData(SensorDataPackage dataPackage){
        //items.
        Map<Integer,SensorData> map = new HashMap<>();
        int size = dataPackage.getDatas().size();
        for(int i = 0; i<size;i++){
            map.put(dataPackage.getSensorTypes().get(i),dataPackage.getDatas().get(i));
        }
        for(Integer i: map.keySet()){
            for(SensorItem item: items){
                if (item.sensorType == i){
                    item.sensorValue = SensorsEnum.resolveEnum(i).getStringData(getContext(),map.get(i).values);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }*/

    public void setNewData(ArrayDeque<SensorDataPackage> dataPackages){
        Map<Integer,SensorData> map = new HashMap<>();
        SensorDataPackage dataPackage;
        int size;
        while(!dataPackages.isEmpty()){
            //Log.d(TAG, "setNewData: ");
            dataPackage = dataPackages.pop();
            size = dataPackage.getDatas().size();
            if(size == 1){
                map.put(dataPackage.getSensorTypes().get(0),dataPackage.getDatas().get(0));
                continue;
            }
            for(int i = 0; i<size;i++){
                map.put(dataPackage.getSensorTypes().get(i),dataPackage.getDatas().get(i));
            }
        }
        boolean wasFound;

        keyLooper:
        for(Integer i: map.keySet()){
            for(SensorItem item: items){
                Log.d(TAG, "setNewData: "+item.sensorType);
                if (item.sensorType == i){
                    Log.d(TAG, "setNewData: Found id"+i);
                    item.sensorValue = SensorsEnum.resolveEnum(i).getStringData(getContext(),map.get(i).values);
                    continue keyLooper;
                }
            }
            items.add(new SensorItem(SensorsEnum.resolveEnum(i).getSensorName(getContext()),SensorsEnum.resolveEnum(i).getStringData(getContext(),map.get(i).values),i));
        }
        adapter.notifyDataSetChanged();
    }

}
