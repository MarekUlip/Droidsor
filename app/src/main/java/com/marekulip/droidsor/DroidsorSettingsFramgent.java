package com.marekulip.droidsor;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

/**
 * Fragment used for setting settings.
 */
public class DroidsorSettingsFramgent extends PreferenceFragment{
    /**
     * Setting indicating that app should disconnect from BT device if it does not need it at the moment
     */
    public static final String DISCONNECT_FROM_BT_PREF = "disconnect_from_bt";
    /**
     * Setting indicating that app should wait for GPS position before starting log
     */
    public static final String WAIT_FOR_GPS_PREF = "wait_for_gps";
    /**
     * Setting telling what should start log button do
     */
    public static final String START_LOG_BUT_BEHAVIOUR_PREF = "start_log_but_behaviour";
    /**
     * Setting indicating whether service should be stop after only one click on notification
     */
    public static final String ONE_CLICK_NOTIFICATION_EXIT = "one_click_notification_exit";
    /**
     * Setting indicating how many points should be displayed on chart.
     */
    public static final String COUNT_OF_POINTS = "count_of_points";
    /**
     * Setting indicating whether log should stop after specified amount of time
     */
    public static final String SCHEDULED_LOG_END = "scheduled_log_end";
    /**
     * Specified amount of time after which log should be stopped
     */
    public static final String SCHEDULED_LOG_END_TIME = "scheduled_log_end_time";
    /**
     * Determines whether app should show GPS data with other sensors. This setting may cause slightly bigger
     * battery consumption
     */
    public static final String SHOW_GPS_DATA = "show_gps_data";

    public DroidsorSettingsFramgent() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
