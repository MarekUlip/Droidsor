package com.example.marekulip.droidsor.logs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.marekulip.droidsor.R;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailActivity extends AppCompatActivity {
    private LogsDetailFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        fragment = new LogsDetailFragment();
        Bundle b = new Bundle();
        b.putInt("id",getIntent().getIntExtra("id",0));
        fragment.setArguments(b);
        getSupportFragmentManager().beginTransaction().replace(R.id.log_fragment, fragment).commit();
    }
}
