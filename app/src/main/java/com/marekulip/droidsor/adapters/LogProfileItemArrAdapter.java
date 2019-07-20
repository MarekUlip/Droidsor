package com.marekulip.droidsor.adapters;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
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

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.SetExtMovSensorDialogFragment;
import com.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.List;

/**
 * Created by Marek Ulip on 28.10.2017.
 * Adapter used to display and edit Profile items.
 */

public class LogProfileItemArrAdapter extends ArrayAdapter<LogProfileItem>{

    private List<LogProfileItem> items;
    /**
     * Reference to a fragment using this Adapter
     */
    private FragmentActivity mActivity;
    /**
     * Sparse array that helps realise true nature of movement Sensor in SensorTag where movement sensor actually contains tree inner sensors
     * This array is used to remeber which sensors from movement sensor were selected
     */
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
        //Disable listener so setting checked state does not trigger it
        viewHolder.enableChB.setOnCheckedChangeListener(null);
        viewHolder.enableChB.setChecked(profile.isEnabled);
        //Again enable listener on appropriate sensor type
        viewHolder.enableChB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(profile.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){
                    //This is sensor containing more sensors show dialog with contained sensors
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
        viewHolder.frequencyEditText.setText(String.valueOf(profile.scanFrequency));
        viewHolder.frequencySeekBar.setProgress(profile.scanFrequency-minimumValue);
        viewHolder.itemName.setText(SensorsEnum.resolveEnum(profile.sensorType).getSensorName(getContext()));// "Sensor "+profile.getSensorType());
        return convertView;
    }

    public void setItems(List<LogProfileItem> items){
        this.items = items;
    }

    /**
     * Get items of this array adapter. Note that items of movement sensor must be obtained with {@link #getExtBluetoothMovSensorStates()} method
     * @return Items of this array adapter
     */
    public List<LogProfileItem> getItems(){
        return items;
    }

    /**
     * Get selected items of external movement sensor
     * @return Items of external movement sensor
     */
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

    /**
     * Minimal value of sensor logging frequency in ms
     */
    private final int minimumValue = 200;
    /**
     * Maximum value of sensor logging frequency in ms
     */
    private final int maxValue = 10000;
    private SeekBar.OnSeekBarChangeListener createSeekBarChangeListener(final ViewHolder v){

        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // If user did the change set text of EditText View and also add
                // minimum value so the final value is correct
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
                    // Keep value in bounds
                    if(value>maxValue)value = maxValue;
                    else if(value<minimumValue)value = minimumValue;
                    // Set real value
                    items.get(v.position).setScanFrequency(value);
                    // Set programmatic value so it reflects behaviour of seek bar
                    v.frequencySeekBar.setProgress(value-minimumValue);
                }
            }
        };
    }

    private final int minDelta = 300;           // threshold in ms
    private long focusTime = 0;                 // time of last touch
    private View focusTarget = null;

    /**
     * Focus change listener used so that number keyboard is displayed correctly
     * and focus is obtained when user tries to write frequency via EditText
     */
    private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
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
