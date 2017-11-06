package com.example.marekulip.droidsor.logs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.marekulip.droidsor.R;

public class LogsActivity extends AppCompatActivity {

    private LogsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        fragment = new LogsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.log_fragment, fragment).commit();
    }
}
