package com.example.marekulip.droidsor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.marekulip.droidsor.adapters.SensorDataDispArrAdapter;

import java.util.ArrayList;
import java.util.List;

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
        showMobileSensors();
        adapter = new SensorDataDispArrAdapter(getContext(),R.layout.sensor_data_displayer_list_item,items);
        setListAdapter(adapter);
        Log.d(TAG, "onActivityCreated: ");
    }

    public void showBLESensors(){
        items.clear();
        items.add(new SensorItem("BLE Humidity","86.7 %rH"));
        items.add(new SensorItem("BLE Temperature","18,0°C"));
        items.add(new SensorItem("BLE Barometer","782.8 mBar"));
        items.add(new SensorItem("BLE Accelerometer","10 m/s"+System.lineSeparator()+"3 m/s"+System.lineSeparator()+"1 m/s"));
    }

    public void showMobileSensors(){
        items.clear();
        items.add(new SensorItem("Humidity","86.7 %rH"));
        items.add(new SensorItem("Temperature","18,0°C"));
        items.add(new SensorItem("Barometer","782.8 mBar"));
        items.add(new SensorItem("Accelerometer","10 m/s"+System.lineSeparator()+"3 m/s"+System.lineSeparator()+"1 m/s"));
    }

    public void showAllSensors(){
        Log.d(TAG, "showAllSensors: ");
        items.clear();
        items.add(new SensorItem("Humidity","86.7 %rH"));
        items.add(new SensorItem("Temperature","18,0°C"));
        items.add(new SensorItem("Barometer","782.8 mBar"));
        items.add(new SensorItem("Accelerometer","10 m/s"+System.lineSeparator()+"3 m/s"+System.lineSeparator()+"1 m/s"));
        items.add(new SensorItem("BLE Humidity","86.7 %rH"));
        items.add(new SensorItem("BLE Temperature","18,0°C"));
        items.add(new SensorItem("BLE Barometer","782.8 mBar"));
        items.add(new SensorItem("BLE Accelerometer","10 m/s"+System.lineSeparator()+"3 m/s"+System.lineSeparator()+"1 m/s"));
        adapter.notifyDataSetChanged();
    }
}
