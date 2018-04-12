package com.marekulip.droidsor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Activity used to manage log profiles. It allows to pick favorite, pick next profile that will be used to log and also allows to create new log.
 */
public class LogProfileActivity extends AppCompatActivity implements LogProfileListFragment.LogProfileListFragmentListener{

    /**
     * Identification that no favorite profile was set and it is required to run log.
     */
    public static final String IS_PICKING_FAVORITE_PROFILE= "is_picking_favorite";
    /**
     * Option for fab with show all logs settings
     */
    public static final String IS_PICKING_NEXT_TO_LOG = "is_picking_next_to_log";
    /**
     * Indicator for picked id
     */
    public static final String NEXT_LOG_ID = "next_log_id";
    /**
     * Fragment to contain profile list
     */
    LogProfileListFragment fragment;
    /**
     * Indicates whether pick next profile feature is on
     */
    private boolean isPickingModeOn = false;
    /**
     * Indicates whether mark more feature is on
     */
    private boolean isSelectionModeOn = false;


    /**
     * Indicates that no favorite profile was set and it is being selected now. Used only when log start
     * requires favorite but no favorite is present.
     */
    private boolean isPickingFirstFavoriteProfile = false;
    /**
     * Indicates that selected profile will be used as next log profile.
     */
    private boolean isPickingNextToLog = false;
    /**
     * Id which will be returned as result for this action
     */
    public static final int SET_FIRST_FAVORITE_PROFILE = 6;
    /**
     * Id which will be returned as result for this action
     */
    public static final int SET_NEXT_TO_LOG = 7;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_sensor_d_displ);

        FloatingActionButton fab = findViewById(R.id.sens_disp_fab);
        fab.setImageResource(R.drawable.ic_action_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LogProfileActivity.this,LogProfileSettingActivity.class));
            }
        });

        fragment = LogProfileListFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment,fragment).commit();

        isPickingFirstFavoriteProfile = getIntent().getBooleanExtra(IS_PICKING_FAVORITE_PROFILE,false);
        isPickingNextToLog = getIntent().getBooleanExtra(IS_PICKING_NEXT_TO_LOG,false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if(isPickingFirstFavoriteProfile||isPickingNextToLog){
            toolbar.setVisibility(View.GONE);
        }else{
            setSupportActionBar(toolbar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isPickingFirstFavoriteProfile||isPickingNextToLog)fragment.setPickingMode(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!isPickingModeOn && !isSelectionModeOn)getMenuInflater().inflate(R.menu.log_profile_menu,menu);
        else {
            getMenuInflater().inflate(R.menu.cancel_menu,menu);
            if(isSelectionModeOn)menu.findItem(R.id.action_delete).setVisible(true);
            else menu.findItem(R.id.action_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_pick_favorite:
                fragment.setPickingMode(true);
                break;
            case R.id.action_mark_more:
                fragment.setSelectionMode(true);
                invalidateOptionsMenu();
                break;
            case R.id.action_delete:
                fragment.deleteMarked();
                break;
            case R.id.action_cancel:
                if(isSelectionModeOn) {
                    fragment.setSelectionMode(false);
                    invalidateOptionsMenu();
                } else if(isPickingModeOn) fragment.setPickingMode(false);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(isPickingFirstFavoriteProfile || isPickingNextToLog){
            // Inform activity that no profile was selected
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        if(fragment.isPickingModeOn()){
            fragment.setPickingMode(false);
        } else if(isSelectionModeOn){
            isSelectionModeOn = false;
            fragment.setSelectionMode(false);
        } else finish();
    }

    @Override
    public void changePickingMode(boolean on) {
        if(isPickingFirstFavoriteProfile||isPickingNextToLog)return;
        else isPickingModeOn = on;
        invalidateOptionsMenu();
    }

    @Override
    public void profilePicked(long id) {
        if(isPickingNextToLog){
            Intent intent = new Intent();
            intent.putExtra(NEXT_LOG_ID,id);
            setResult(RESULT_OK,intent);
            finish();
            return;
        }
        SharedPreferences settings = getSharedPreferences(SensorDataDisplayerActivity.SHARED_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(SensorDataDisplayerActivity.FAVORITE_LOG, id);
        editor.apply();
        Toast.makeText(this,R.string.favorite_log_picked,Toast.LENGTH_SHORT).show();
        if(isPickingFirstFavoriteProfile){
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void changeSelectionMode(boolean on) {
        if(isPickingFirstFavoriteProfile||isPickingNextToLog)return;
        else isSelectionModeOn = on;
        invalidateOptionsMenu();
    }
}
