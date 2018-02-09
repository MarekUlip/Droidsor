package com.example.marekulip.droidsor.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfileItemArrAdapter extends ArrayAdapter<LogProfileItem>{

    private List<LogProfileItem> items;
    private int accelPosition;
    private int magnetPosition;
    private int orientPosition;
    private ViewHolder accellViewHolder;
    private ViewHolder magnetViewHolder;
    private ViewHolder orientViewHolder;


    public LogProfileItemArrAdapter(@NonNull Context context, int resource, List<LogProfileItem> items) {
        super(context, resource, items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        final LogProfileItem profile = getItem(position);
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.profile_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.position = position;
            viewHolder.enableChB = convertView.findViewById(R.id.profile_item_enabled_cb);
            viewHolder.itemName = convertView.findViewById(R.id.profile_item_name_tv);
            viewHolder.frequencySeekBar = convertView.findViewById(R.id.profile_item_freq_sb);
            viewHolder.frequencySeekBar.setMax(maxValue-minimumValue);
            viewHolder.frequencySeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(viewHolder));
            viewHolder.frequencyEditText = convertView.findViewById(R.id.profile_item_freq_et);
            viewHolder.frequencyEditText.addTextChangedListener(createTextWatcher(viewHolder));
            convertView.setTag(viewHolder);
        } else{
            viewHolder = (ViewHolder)convertView.getTag();
            viewHolder.position = position;
        }

        viewHolder.enableChB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                profile.setEnabled(b);
                if(profile.getSensorType() == SensorsEnum.INTERNAL_ORIENTATION.sensorType){
                    for(LogProfileItem p: items){
                        if(p.getSensorType()==SensorsEnum.INTERNAL_ACCELEROMETER.sensorType){
                            p.setEnabled(true);
                        }
                        else if(p.getSensorType()==SensorsEnum.INTERNAL_MAGNETOMETER.sensorType){
                            p.setEnabled(true);
                        }
                    }
                }else if(profile.getSensorType() == SensorsEnum.INTERNAL_ACCELEROMETER.sensorType){
                    for(LogProfileItem p: items){
                        if(p.getSensorType()==SensorsEnum.INTERNAL_ORIENTATION.sensorType){
                            p.setEnabled(false);
                            break;
                        }
                    }
                }else if (profile.getSensorType() == SensorsEnum.INTERNAL_MAGNETOMETER.sensorType){
                    for(LogProfileItem p: items){
                        if(p.getSensorType()==SensorsEnum.INTERNAL_ORIENTATION.sensorType){
                            p.setEnabled(false);
                            break;
                        }
                    }
                }
            }
        });
        viewHolder.enableChB.setChecked(profile.isEnabled());
        viewHolder.frequencyEditText.setText(String.valueOf(profile.getScanFrequency()+minimumValue));
        viewHolder.frequencySeekBar.setProgress(profile.getScanFrequency());
        viewHolder.itemName.setText(SensorsEnum.resolveEnum(profile.getSensorType()).getSensorName(getContext()));// "Sensor "+profile.getSensorType());
        return convertView;
    }



    public void setItems(List<LogProfileItem> items){
        this.items = items;
    }

    public List<LogProfileItem> getItems(){
        return items;
    }

    static class ViewHolder{
        int position;
        CheckBox enableChB;
        TextView itemName;
        SeekBar frequencySeekBar;
        EditText frequencyEditText;
    }
    private final int minimumValue = 200;
    private final int maxValue = 10000;
    private SeekBar.OnSeekBarChangeListener createSeekBarChangeListener(final ViewHolder v){

        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)v.frequencyEditText.setText(String.valueOf(i+minimumValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }

    private TextWatcher createTextWatcher(final ViewHolder v){
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.d("sd", "afterTextChanged: ");
                if(editable.toString().length() != 0) {
                    int value = Integer.parseInt(editable.toString());
                    if(value>maxValue)value = maxValue;
                    items.get(v.position).setScanFrequency(value-minimumValue>=minimumValue?value-minimumValue:minimumValue);
                    v.frequencySeekBar.setProgress(value-minimumValue);
                }
            }
        };
    }
}
