package com.example.marekulip.droidsor.adapters;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.marekulip.droidsor.DroidsorSettingsFramgent;
import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.SensorItem;
import com.example.marekulip.droidsor.opengl.OpenGLActivity;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Created by Marek Ulip on 19-Sep-17.
 */

public class SensorDataDispArrAdapter extends ArrayAdapter<SensorItem> {

    private boolean show3DBut = true;

    public SensorDataDispArrAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<SensorItem> objects) {
        super(context, resource, objects);
        show3DBut = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DroidsorSettingsFramgent.SHOW_THREE_D_BUT,true);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final SensorItem item = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sensor_data_displayer_list_item,parent,false);
        }

        TextView sensorName =  convertView.findViewById(R.id.sensor_name);
        TextView sensorValue = convertView.findViewById(R.id.sensor_value);
        Button button = convertView.findViewById(R.id.button_3d_view);

        sensorName.setText(item.sensorName);
        sensorValue.setText(item.sensorValue);
        if(item.sensorType== SensorsEnum.INTERNAL_GYROSCOPE.sensorType || item.sensorType== SensorsEnum.INTERNAL_ORIENTATION.sensorType){
            show3DBut = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DroidsorSettingsFramgent.SHOW_THREE_D_BUT,true);
            if(!show3DBut){
                button.setVisibility(View.GONE);
                return convertView;
            }
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), OpenGLActivity.class);
                    intent.putExtra(OpenGLActivity.SENSOR_TYPE,item.sensorType);
                    getContext().startActivity(intent);
                }
            });
        }else {
            button.setVisibility(View.GONE);
        }


        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
