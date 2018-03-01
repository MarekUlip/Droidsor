package com.example.marekulip.droidsor;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;

import static com.example.marekulip.droidsor.SensorDataDisplayerActivity.BT_DEVICE_REQUEST;
import static com.example.marekulip.droidsor.SensorDataDisplayerActivity.DEVICE_ADDRESS;

public class LogProfileSettingActivity extends AppCompatActivity implements SaveProfileDialogFragment.SaveProfileDialogListener, SetExtMovSensorDialogFragment.SetExtMovSensorIface{

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

        FloatingActionButton fab = findViewById(R.id.sens_disp_fab);
        fab.setImageResource(R.drawable.ic_action_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.saveProfile();
            }
        });

        /*Intent intent = new Intent(this,SensorService.class);
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);*/

        fragment = LogProfileSettingFragment.newInstance(getIntent().getBooleanExtra(IS_NEW,true),getIntent().getIntExtra(LOG_PROFILE_ID,0));
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment,fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_conn,menu);
        if(mSensorService!=null){
            if(mSensorService.isBluetoothDeviceOn()){
                menu.findItem(R.id.action_bluetooth_connect).setVisible(false);
                menu.findItem(R.id.action_bluetooth_disconnect).setVisible(true);
            }else {
                menu.findItem(R.id.action_bluetooth_connect).setVisible(true);
                menu.findItem(R.id.action_bluetooth_disconnect).setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_bluetooth_connect:
                startActivityForResult(new Intent(LogProfileSettingActivity.this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
                break;
            case R.id.action_bluetooth_disconnect:
                mSensorService.disconnectFromBluetoothDevice();
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
        invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        disconnectFromService();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            fragment.setSensorService(mSensorService);
            invalidateOptionsMenu();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);//startService(intent);
            }else{
                startService(intent);
            }
        }
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
        registerReceiver(mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    private void disconnectFromService(){
        if(!mSensorService.isLogging())mSensorService.stopListeningSensors();//TODO stop bluetooth sensors too
        unregisterReceiver(mSensorServiceUpdateReceiver);
        unbindService(mServiceConnection);
    }

    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothSensorManager.ACTION_GATT_CONNECTED.equals(action) || BluetoothSensorManager.ACTION_GATT_DISCONNECTED.equals(action)) {
                fragment.restartFragment();
                invalidateOptionsMenu();
            }
        }
    };

    private static IntentFilter makeUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothSensorManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothSensorManager.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    @Override
    public void extMovSensorsSet(boolean acc, boolean gyr, boolean mag) {
        fragment.extMovSensorsSet(acc,gyr,mag);
    }
}
