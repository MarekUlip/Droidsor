package com.marekulip.droidsor.logs;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.marekulip.droidsor.R;

/**
 * Activity used do display list of all recorded logs
 */
public class LogsActivity extends AppCompatActivity {

    /**
     * Fragment which displays the logs
     */
    private LogsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        fragment = new LogsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.log_fragment, fragment).commit();
    }

    @Override
    public void onBackPressed() {
        //If mark more feature is enable simply disable it and stay in activity
        if(fragment.isSelectionModeOn()){
            fragment.setSelectionMode(false);
        }
        else {
            finish();
        }
    }
}
