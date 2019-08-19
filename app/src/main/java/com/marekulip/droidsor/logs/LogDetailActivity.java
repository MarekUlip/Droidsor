package com.marekulip.droidsor.logs;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.marekulip.droidsor.R;

/**
 * Activity for displaying detail of a log
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogDetailActivity extends AppCompatActivity {
    /**
     * Fragment in which actual detail will be shown.
     */
    private LogsDetailFragment fragment;
    /**
     * Indicates whether selection of more items feature is enabled.
     */
    //private boolean isSelectionModeOn = false;

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
        if(fragment!=null && fragment.isSelectionModeOn()){
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
        switch (id) {
            case R.id.action_mark_more:
                fragment.setSelectionMode(true);
                break;
            case R.id.action_export_selected:
                fragment.exportSelectedItems();
                cancelSelection();
                break;
            case R.id.action_cancel:
                cancelSelection();
                break;
        }
        invalidateOptionsMenu();
        return true;
    }

    /**
     * Disables mark more feature
     */
    private void cancelSelection(){
        fragment.setSelectionMode(false);
    }

    @Override
    public void onBackPressed() {
        //If mark more feature is enable just disable it and stay in this activity
        if(fragment!=null && fragment.isSelectionModeOn()){
            cancelSelection();
            invalidateOptionsMenu();
        }else{
            finish();
        }

    }
}
