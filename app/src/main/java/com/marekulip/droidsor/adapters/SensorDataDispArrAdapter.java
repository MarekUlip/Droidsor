package com.marekulip.droidsor.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.SensorItem;

import java.util.List;

/**
 * Created by Marek Ulip on 19-Sep-17.
 * Adapter used to display data from sensors on main page
 */

public class SensorDataDispArrAdapter extends ArrayAdapter<SensorItem> {
    public SensorDataDispArrAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<SensorItem> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        final SensorItem item = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sensor_data_displayer_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.position = position;
            viewHolder.sensorName = convertView.findViewById(R.id.sensor_name);
            viewHolder.sensorValue = convertView.findViewById(R.id.sensor_value);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.position = position;
        }

        /*TextView sensorName =  convertView.findViewById(R.id.sensor_name);
        TextView sensorValue = convertView.findViewById(R.id.sensor_value);*/

        viewHolder.sensorName.setText(item.sensorName);
        viewHolder.sensorValue.setText(item.sensorValue);
        return convertView;
    }

    private static class ViewHolder{
        int position;
        TextView sensorName;
        TextView sensorValue;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
