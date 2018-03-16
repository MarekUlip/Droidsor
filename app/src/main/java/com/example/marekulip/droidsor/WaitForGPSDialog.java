package com.example.marekulip.droidsor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by Marek Ulip on 09-Feb-18.
 */

public class WaitForGPSDialog extends DialogFragment {

    public interface WaitForGPSIFace{
        void startWithNoGPS();
        void cancelLog();
    }

    private WaitForGPSIFace mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (WaitForGPSIFace) context;
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
        builder.setTitle(R.string.wait_for_gps).setMessage(R.string.waiting_for_gps_message).setPositiveButton(R.string.start_without_gps, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.startWithNoGPS();
                dismiss();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.cancelLog();
                dismiss();
            }
        }).setCancelable(false);
        return builder.create();
    }
}
