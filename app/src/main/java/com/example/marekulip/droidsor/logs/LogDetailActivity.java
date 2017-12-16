package com.example.marekulip.droidsor.logs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.marekulip.droidsor.R;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailActivity extends AppCompatActivity {
    private LogsDetailFragment fragment;
    private boolean isSelectionModeOn = false;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_detail_menu, menu);
        if(isSelectionModeOn){
            menu.findItem(R.id.action_mark_more).setVisible(false);
            menu.findItem(R.id.action_export_selected).setVisible(true);
            menu.findItem(R.id.action_cancel).setVisible(true);
        }else {
            menu.findItem(R.id.action_mark_more).setVisible(true);
            menu.findItem(R.id.action_export_selected).setVisible(false);
            menu.findItem(R.id.action_cancel).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_mark_more) {
            isSelectionModeOn = true;
            fragment.setSelectionMode(true);
        } else if(id == R.id.action_export_selected){
            fragment.exportSelected();
            isSelectionModeOn = false;
            fragment.setSelectionMode(false);
        } else if (id == R.id.action_cancel){
            isSelectionModeOn = false;
            fragment.setSelectionMode(false);
        }
        invalidateOptionsMenu();
        return true;
    }
}
