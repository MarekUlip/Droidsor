package com.example.marekulip.droidsor;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import static com.example.marekulip.droidsor.SensorDataDisplayerActivity.BT_DEVICE_REQUEST;
import static com.example.marekulip.droidsor.SensorDataDisplayerActivity.DEVICE_ADDRESS;

public class LogProfileSettingActivity extends AppCompatActivity implements SaveProfileDialogFragment.SaveProfileDialogListener{

    LogProfileSettingFragment fragment;
    private SensorService mSensorService;
    public static final String LOG_PROFILE_ID = "log_id";
    public static final String IS_NEW = "is_new";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_sensor_d_displ);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        /*Intent intent = new Intent(this,SensorService.class);
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);*/

        fragment = LogProfileSettingFragment.newInstance(getIntent().getBooleanExtra(IS_NEW,true),getIntent().getIntExtra(LOG_PROFILE_ID,0));
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment,fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_profile_items_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save:
                fragment.saveProfile();
                break;
            case R.id.action_connect_device:
                startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BT_DEVICE_REQUEST){
            if(resultCode==RESULT_OK) {
                mSensorService.connectToBluetoothDevice(data.getStringExtra(DEVICE_ADDRESS));
                fragment.restartFragment();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToService();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(mServiceConnection); //TODO solve leaking service problem
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            fragment.setSensorService(mSensorService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };

    @Override
    public void saveProfile(String name, int frequency, boolean scanGPS) {
        fragment.finishSaving(name, frequency, scanGPS);
        finish();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void connectToService(){
        Intent intent = new Intent(this,SensorService.class);
        if(!isMyServiceRunning(SensorService.class)){
            Log.d("NtRn", "onCreate: NotRunning");startService(intent);
        }
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
    }
}
