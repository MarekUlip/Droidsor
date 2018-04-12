package com.marekulip.droidsor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Activity for settings
 */
public class DroidsorSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new DroidsorSettingsFramgent()).commit();
    }
}
