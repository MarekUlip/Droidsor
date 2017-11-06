package com.example.marekulip.droidsor.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.SensorItem;

import java.util.List;

/**
 * Created by Marek Ulip on 19-Sep-17.
 */

public class SensorDataDispArrAdapter extends ArrayAdapter<SensorItem> {
    public SensorDataDispArrAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<SensorItem> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        SensorItem item = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sensor_data_displayer_list_item,parent,false);
        }

        TextView sensorName =  convertView.findViewById(R.id.sensor_name);
        TextView sensorValue = convertView.findViewById(R.id.sensor_value);

        sensorName.setText(item.sensorName);
        sensorValue.setText(item.sensorValue);

        return convertView;
    }
}
