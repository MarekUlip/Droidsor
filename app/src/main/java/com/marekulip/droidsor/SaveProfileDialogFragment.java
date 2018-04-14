package com.marekulip.droidsor;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/**
 * Created by Marek Ulip on 18.11.2017.
 */

public class SaveProfileDialogFragment extends DialogFragment {

    public final static String PROFILE_NAME = "profile_name";
    public final static String GPS_FREQ = "gps_freq";
    public final static String SCAN_GPS = "scan_gps";

    private Bundle args;


    public interface SaveProfileDialogListener{
        void saveProfile(String name, int frequency, boolean scanGPS);
    }
    private int gpsFrequency = 1000;
    private final int minScan = 1000;
    private final int maxScan = 600000-minScan;
    private SaveProfileDialogListener mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments()==null?new Bundle():getArguments();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (SaveProfileDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view  = inflater.inflate(R.layout.dialog_save_profile_layout, null);
        EditText freq = view.findViewById(R.id.gps_freq_edit);
        final SeekBar freqBar = view.findViewById(R.id.gps_freq);
        final EditText profNameEdit = view.findViewById(R.id.profile_name);
        freqBar.setMax(maxScan);
        gpsFrequency = args.getInt(GPS_FREQ,minScan);
        freqBar.setProgress(gpsFrequency);
        freq.setText(String.valueOf(args.getInt(GPS_FREQ,minScan)));
        String freqText = args.getString(PROFILE_NAME,"");
        final CheckBox checkBox = view.findViewById(R.id.scan_gps_chbox);
        final LinearLayout freqWrapper = view.findViewById(R.id.gps_freq_wrapper);
        if(freqText.length() ==0){
            profNameEdit.setHint(getString(R.string.profile_name));
        }
        else {
            boolean scanGPS = (args.getBoolean(SCAN_GPS,false));
            checkBox.setChecked(scanGPS);
            if(scanGPS){
                freqWrapper.setVisibility(View.VISIBLE);

            }
            profNameEdit.setText(freqText);
        }

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    freqWrapper.setVisibility(View.VISIBLE);
                }else freqWrapper.setVisibility(View.GONE);
            }
        });

        freq.addTextChangedListener(createTextWatcher(freqBar));
        freqBar.setOnSeekBarChangeListener(createSeekBarChangeListener(freq));

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mListener.saveProfile(profNameEdit.getText().toString(),gpsFrequency,checkBox.isChecked());
            }
        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SaveProfileDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarChangeListener(final EditText v){

        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)v.setText(String.valueOf(i+minScan));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }

    /**
     * TextWatcher that set progress of provided SeekBar on any EditText change.
     * @param v SeekBar that should be changed
     * @return TextWatcher that can be used
     */
    private TextWatcher createTextWatcher(final SeekBar v){
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().length() != 0) {
                    int value = Integer.parseInt(editable.toString());
                    // Keep value in bounds
                    if(value>maxScan)value = maxScan;
                    else if(value<minScan)value = minScan;
                    // Set real value
                    gpsFrequency = value;
                    // Set programmatic value so it reflects behaviour of seek bar
                    v.setProgress(value-minScan);
                }
            }
        };
    }


}
