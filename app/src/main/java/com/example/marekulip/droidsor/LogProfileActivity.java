package com.example.marekulip.droidsor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class LogProfileActivity extends AppCompatActivity implements LogProfileListFragment.LogProfileListFragmentListener{

    LogProfileListFragment fragment;
    private boolean isPickingModeOn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_sensor_d_displ);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fragment = LogProfileListFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment,fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

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
            case R.id.action_create_new:
                startActivity(new Intent(this,LogProfileSettingActivity.class));
                break;
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
        if(fragment.isPickingModeOn()){
            fragment.exitPickingMode();
        }else finish();
    }

    @Override
    public void changePickingMode(boolean on) {
        isPickingModeOn = on;
        invalidateOptionsMenu();
    }
}
