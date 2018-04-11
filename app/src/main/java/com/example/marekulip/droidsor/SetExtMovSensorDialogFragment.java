package com.example.marekulip.droidsor;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

/**
 * Created by Marek Ulip on 14-Feb-18.
 * DialogFragment that is used to set items from external movement sensor
 */

public class SetExtMovSensorDialogFragment extends DialogFragment {
    public static final String ACC_SENSOR = "acc_sensor";
    public static final String GYR_SENSOR = "gyr_sensor";
    public static final String MAG_SENSOR = "mag_sensor";
    private Bundle args;

    private SetExtMovSensorIface mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (SetExtMovSensorIface) context;
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
        final View view  = inflater.inflate(R.layout.dialog_profile_ext_mov_set, null);
        final CheckBox acCbx = view.findViewById(R.id.check_acc);
        final CheckBox gyrCbx = view.findViewById(R.id.check_gyr);
        final CheckBox magCbx = view.findViewById(R.id.check_mag);
        acCbx.setChecked(args.getBoolean(ACC_SENSOR,false));
        gyrCbx.setChecked(args.getBoolean(GYR_SENSOR,false));
        magCbx.setChecked(args.getBoolean(MAG_SENSOR,false));
        builder.setView(view).setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.extMovSensorsSet(acCbx.isChecked(),gyrCbx.isChecked(),magCbx.isChecked());
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.extMovSensorsSet(acCbx.isChecked(),gyrCbx.isChecked(),magCbx.isChecked());
                dismiss();
            }
        });
        return builder.create();
    }

    public interface SetExtMovSensorIface{
        void extMovSensorsSet(boolean acc, boolean gyr, boolean mag);
    }
}
