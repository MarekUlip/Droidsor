package com.example.marekulip.droidsor.logs;

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
import com.example.marekulip.droidsor.grapview.LineGraphView;

import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailArrayAdapter extends ArrayAdapter<LogDetailItem> {
    public LogDetailArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<LogDetailItem> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LogDetailItem item = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.log_list_item,parent,false);
        }

        LineGraphView graphView =  convertView.findViewById(R.id.log_chart);
        TextView sensorValue = convertView.findViewById(R.id.sensor_name);

        sensorValue.setText(item.sensorName);
        graphView.setxAxisLabel("Time");
        graphView.setyAxisLabel(item.yLabel);
        graphView.setGraphName("Graph name");
        graphView.setGraphItem(item.graphItems);

        return convertView;
    }
}
