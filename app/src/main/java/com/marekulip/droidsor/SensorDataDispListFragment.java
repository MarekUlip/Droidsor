package com.marekulip.droidsor;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import android.util.SparseArray;

import com.marekulip.droidsor.adapters.SensorDataDispArrAdapter;
import com.marekulip.droidsor.sensorlogmanager.Point3D;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 19-Sep-17.
 * Fragment that displays list of sensor data.
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
    }

    /**
     * Sets sensor type ids of sensor that are expected to be shown in the list. To display items from sensors
     * call {@link #setNewData(List, SparseArray)}.
     * Ids not specified here will not be displayed even if sent via {@link #setNewData(List, SparseArray)} method.
     * @param sensorTypes list of sensor type ids
     */
    public void setSensorsToShow(List<Integer> sensorTypes){
        items.clear();
        SensorsEnum sensor;
        for(Integer i: sensorTypes){
            sensor = SensorsEnum.resolveEnum(i);
            //Log.d(TAG, "setSensorsToShow: "+i);
            items.add(new SensorItem(sensor.getSensorName(getContext()),sensor.getStringData(getContext(),Point3D.getDefaultPoint3D()),i));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Sets new data that will be shown in the list. Note that ids not set int {@link #setSensorsToShow(List)}
     * method will not be displayed here.
     * @param sensorTypes ids of sensor that are sent with this method
     * @param newData data of sensors
     */
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
