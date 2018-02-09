package com.example.marekulip.droidsor;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class DroidsorSettingsFramgent extends PreferenceFragment{
    public static final String DISCONNECT_FROM_BT_PREF = "disconnect_from_bt";
    public static final String WAIT_FOR_GPS_PREF = "wait_for_gps";
    public static final String START_LOG_BUT_BEHAVIOUR_PREF = "start_log_but_behaviour";
    public static final String ONE_CLICK_NOTIFICATION_EXIT = "one_click_notification_exit";
    public static final String COUNT_OF_POINTS = "count_of_points";

    public DroidsorSettingsFramgent() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
