package com.example.marekulip.droidsor.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
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
import com.example.marekulip.droidsor.SetExtMovSensorDialogFragment;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfileItemArrAdapter extends ArrayAdapter<LogProfileItem>{

    private List<LogProfileItem> items;
    private FragmentActivity mActivity;
    private SparseBooleanArray extBluetoothMovSensorStates = new SparseBooleanArray();


    public LogProfileItemArrAdapter(@NonNull Context context, int resource, List<LogProfileItem> items, FragmentActivity activity) {
        super(context, resource, items);
        this.items = items;
        mActivity = activity;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
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
            viewHolder.frequencyEditText.setOnFocusChangeListener(mOnFocusChangeListener);

            convertView.setTag(viewHolder);
        } else{
            viewHolder = (ViewHolder)convertView.getTag();
            viewHolder.position = position;
        }
        viewHolder.enableChB.setOnCheckedChangeListener(null);
        viewHolder.enableChB.setChecked(profile.isEnabled);
        viewHolder.enableChB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(profile.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){
                    /*if(isCheckProgrammatical){
                        isCheckProgrammatical = false;
                        return;
                    }*/
                    DialogFragment dialogFragment = new SetExtMovSensorDialogFragment();
                    Bundle args = new Bundle();
                    args.putBoolean(SetExtMovSensorDialogFragment.MAG_SENSOR,extBluetoothMovSensorStates.get(SensorsEnum.EXT_MOV_MAGNETIC.sensorType,false));
                    args.putBoolean(SetExtMovSensorDialogFragment.ACC_SENSOR,extBluetoothMovSensorStates.get(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType,false));
                    args.putBoolean(SetExtMovSensorDialogFragment.GYR_SENSOR,extBluetoothMovSensorStates.get(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType,false));
                    dialogFragment.setArguments(args);
                    dialogFragment.show(mActivity.getSupportFragmentManager(),"SetExtMovSensor");
                }else profile.setEnabled(b);

            }
        });
        //if(profile.getSensorType())
        viewHolder.frequencyEditText.setText(String.valueOf(profile.scanFrequency+minimumValue));
        viewHolder.frequencySeekBar.setProgress(profile.scanFrequency-minimumValue);
        viewHolder.itemName.setText(SensorsEnum.resolveEnum(profile.sensorType).getSensorName(getContext()));// "Sensor "+profile.getSensorType());
        return convertView;
    }

    public void setItems(List<LogProfileItem> items){
        this.items = items;
    }

    public List<LogProfileItem> getItems(){
        return items;
    }

    public SparseBooleanArray getExtBluetoothMovSensorStates(){
        return extBluetoothMovSensorStates;
    }
    public void setExtBluetoothMovSensorStates(SparseBooleanArray extBluetoothMovSensorStates){
        this.extBluetoothMovSensorStates = extBluetoothMovSensorStates;
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
                //Log.d("sd", "afterTextChanged: ");
                if(editable.toString().length() != 0) {
                    int value = Integer.parseInt(editable.toString());
                    if(value>maxValue)value = maxValue;
                    else if(value<minimumValue)value = minimumValue;
                    items.get(v.position).setScanFrequency(value-minimumValue>=minimumValue?value-minimumValue:minimumValue);
                    v.frequencySeekBar.setProgress(value-minimumValue);
                }
            }
        };
    }

    private final int minDelta = 300;           // threshold in ms
    private long focusTime = 0;                 // time of last touch
    private View focusTarget = null;

    View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            long t = System.currentTimeMillis();
            long delta = t - focusTime;
            if (hasFocus) {     // gained focus
                if (delta > minDelta) {
                    focusTime = t;
                    focusTarget = view;
                }
            }
            else {              // lost focus
                if (delta <= minDelta  &&  view == focusTarget) {
                    focusTarget.post(new Runnable() {   // reset focus to target
                        public void run() {
                            focusTarget.requestFocus();
                            if(focusTarget instanceof EditText){
                                ((EditText) focusTarget).setSelection(((EditText) focusTarget).getText().toString().length());
                            }
                        }
                    });
                }
            }
        }
    };
}
