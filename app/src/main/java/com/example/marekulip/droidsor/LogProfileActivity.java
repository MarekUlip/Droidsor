package com.example.marekulip.droidsor;

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

public class LogProfileActivity extends AppCompatActivity implements LogProfileListFragment.LogProfileListFragmentListener{


    public static final String IS_PICKING_FAVORITE_PROFILE= "is_picking_favorite";
    public static final String IS_PICKING_NEXT_TO_LOG = "is_picking_next_to_log"; // Option for fab with show all logs settings
    public static final String NEXT_LOG_ID = "next_log_id";
    LogProfileListFragment fragment;
    private boolean isPickingModeOn = false;


    private boolean isPickingFirstFavoriteProfile = false;
    private boolean isPickingNextToLog = false;
    public static final int SET_FIRST_FAVORITE_PROFILE = 6;
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
        if(isPickingFirstFavoriteProfile||isPickingNextToLog)fragment.enterPickingMode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!isPickingModeOn)getMenuInflater().inflate(R.menu.log_profile_menu,menu);
        else getMenuInflater().inflate(R.menu.cancel_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            /*case R.id.action_create_new:
                startActivity(new Intent(this,LogProfileSettingActivity.class));
                break;*/
            case R.id.action_pick_favorite:
                fragment.enterPickingMode();
                break;
            case R.id.action_cancel:
                fragment.exitPickingMode();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(isPickingFirstFavoriteProfile || isPickingNextToLog){
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        if(fragment.isPickingModeOn()){
            fragment.exitPickingMode();
        }else finish();
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
}
