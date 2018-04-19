package com.marekulip.droidsor.logs;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter used to display details from log
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailArrayAdapter extends ArrayAdapter<LogDetailItem> {

    /**
     * Ids of items selected by mark more feature
     */
    private List<Integer> selectedIds = new ArrayList<>();
    /**
     * Indicates whether mark more feature is enabled
     * It is static because sometimes it happens that this value is returned to default value
     */
    private static boolean isSelectionModeOn=false;

    public LogDetailArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<LogDetailItem> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final LogDetailItem item = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.log_list_item,parent,false);
        }

        //Make sure items highlight is correct
        if(isSelectionModeOn){
            if(selectedIds.contains(item.sensorType))convertView.setBackgroundColor(Color.GRAY);
            else convertView.setBackgroundColor(Color.TRANSPARENT);
        }else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        LineChart graphView =  convertView.findViewById(R.id.log_chart);
        TextView sensorValue = convertView.findViewById(R.id.sensor_name);
        TextView sensorUnit = convertView.findViewById(R.id.text_sensor_units);
        View graphWrapper = convertView.findViewById(R.id.graph_wrapper);
        if(isSelectionModeOn){
            //Graph should not be visible when selecting items
            graphWrapper.setVisibility(View.GONE);
        }else {
            graphWrapper.setVisibility(View.VISIBLE);
            sensorUnit.setText(SensorsEnum.resolveEnum(item.sensorType).getSensorUnitName(getContext()));
            graphView.setTouchEnabled(true);
            graphView.getDescription().setEnabled(false);
            // enable scaling and dragging
            graphView.setDragEnabled(true);
            graphView.setScaleEnabled(true);

            // set an alternative background color
            graphView.setBackgroundColor(Color.WHITE);
            graphView.getAxisRight().setEnabled(false);
            graphView.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

            IAxisValueFormatter formatter = new IAxisValueFormatter() {

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if ((int) value >= item.xLabels.size()) return "";
                    return item.xLabels.get((int) value);
                }
            };

            XAxis xAxis = graphView.getXAxis();
            xAxis.setGranularity(countGranularity(item.xLabels.size())); // minimum axis-step (interval) is 1
            xAxis.setValueFormatter(formatter);

            graphView.setData(item.lineData);
            graphView.fitScreen();
            graphView.invalidate();
        }

        sensorValue.setText(item.sensorName);

        return convertView;
    }

    private float countGranularity(int count){
        if(count <= 10) return 1;
        else if(count <= 100) return 5;
        else if(count <= 1000) return 10;
        else return 25;
    }

    /**
     * Sets the list of selected ids. This ids will be highlighted
     * @param list list of selected ids
     */
    public void setSelectedIds(List<Integer> list){
        selectedIds = list;
    }

    /**
     * Sets mark more feature
     * @param selectionModeOn true to turn on false to turn off
     */
    public void setSelectionModeOn(boolean selectionModeOn){
        isSelectionModeOn = selectionModeOn;
    }

}
