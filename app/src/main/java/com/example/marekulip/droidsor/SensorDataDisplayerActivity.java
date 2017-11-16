package com.example.marekulip.droidsor;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.marekulip.droidsor.database.LogProfileItemsTable;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.logs.LogsActivity;
import com.example.marekulip.droidsor.positionmanager.PositionManager;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;

public class SensorDataDisplayerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final int BT_DEVICE_REQUEST = 2;
    private SensorDataDispListFragment fragment;
    private SensorService mSensorService;
    private PositionManager positionManager;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data_displ);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragment = new SensorDataDispListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment, fragment).commit();

        Intent intent = new Intent(this,SensorService.class);
        if(!isMyServiceRunning(SensorService.class)){
            Log.d("NtRn", "onCreate: NotRunning");startService(intent);
        }
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(mSensorService.isLogging()) {
                Toast.makeText(this,"Logging on. Let Service Run",Toast.LENGTH_SHORT).show();
                super.onBackPressed();
            }
            else {
                //Intent intent = new Intent(this,SensorService.class);
                mSensorService.stop(true);
                //unbindService(mServiceConnection);
                //disconnectFromService();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
        connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectFromService();
        //unregisterReceiver(mSensorServiceUpdateReceiver);
    }

    private void connectToService(){
        Intent intent = new Intent(this,SensorService.class);
        if(!isMyServiceRunning(SensorService.class)){
            Log.d("NtRn", "onCreate: NotRunning");startService(intent);
        }
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
        registerReceiver(mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    private void disconnectFromService(){
        if(!mSensorService.isLogging())mSensorService.stopListeningSensors();
        unregisterReceiver(mSensorServiceUpdateReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if(mSensorService.isLogging()){
            menu.findItem(R.id.action_start_log).setVisible(false);
            menu.findItem(R.id.action_stop_log).setVisible(true);
        }else {
            menu.findItem(R.id.action_start_log).setVisible(true);
            menu.findItem(R.id.action_stop_log).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_start_log) {
            positionManager = new PositionManager(this);
            positionManager.initPosManager(this);
            mSensorService.startLogging(getProfile());//TODO set up GPS
            isRecording = true;
            invalidateOptionsMenu();
            return true;
        }else if(id==R.id.action_stop_log){
            mSensorService.stopLogging();
            isRecording = false;
            invalidateOptionsMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BT_DEVICE_REQUEST){
            if(resultCode==RESULT_OK) {
                mSensorService.connectToBluetoothDevice(data.getStringExtra(DEVICE_ADDRESS));
                mSensorService.setMode(SensorService.BLUETOOTH_SENSORS_MODE);
                fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            }else {
                mSensorService.setMode(SensorService.MOBILE_SENSORS_MODE);
                fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            }
        } else if(requestCode == PositionManager.REQUEST_CHECK_SETTINGS){
            if(resultCode==RESULT_OK) {
                positionManager.initPosManager(this);
            }
        } else if(requestCode == PositionManager.MY_PERMISSIONS_REQUEST_LOCATION_FINE){
            if(resultCode==RESULT_OK) {
                positionManager.initPosManager(this);
            }
        }
    }

    private LogProfile getProfile(){
        LogProfile profile = new LogProfile();
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(this);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        /*Cursor c = database.query(LogProfilesTable.TABLE_NAME,
                new String[]{LogProfilesTable._ID,LogProfilesTable.PROFILE_NAME},
                LogProfilesTable._ID+" = ?",
                new String[]{"1"},null,null,null);*/
        Cursor c = database.query(LogProfileItemsTable.TABLE_NAME,new String[]{LogProfileItemsTable.SCAN_PERIOD,LogProfileItemsTable.SENSOR_TYPE},LogProfileItemsTable.PROFILE_ID+" = ?",new String[]{"1"},null,null,null);
        if(c!=null&&c.moveToFirst()){
            Log.d("gotSomething", "getProfile: ");
            LogProfileItem item;
            item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)),false);
            profile.getLogItems().add(item);
            while (c.moveToNext()){
                Log.d("gotSomething", "getProfile: ");
                item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)),false);
                profile.getLogItems().add(item);
            }
            c.close();
        }
        dbHelper.close();
        database.close();
        return profile;
    }

    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorService.ACTION_DATA_AVAILABLE.equals(action)) {
                if(mSensorService.getDataPackages().isEmpty())return;
                fragment.setNewData(mSensorService.getDataPackages());
                //Log.d("Displ", "onReceive: Displaying data");
                //displayData();
            }
        }
    };

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.mobile_sensors) {
            mSensorService.setMode(SensorService.MOBILE_SENSORS_MODE);
            fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
        } else if (id == R.id.ble_sensors) {
            if(mSensorService.isBluetoothDeviceOn()){
                mSensorService.setMode(SensorService.BLUETOOTH_SENSORS_MODE);
                fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            }
            else startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
        } else if (id == R.id.all_sensors) {
            mSensorService.setMode(SensorService.ALL_SENSORS_MODE);
            fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this,LogsActivity.class));
        } else if(id == R.id.nav_log_profiles_settings){
            startActivity(new Intent(this,LogProfileActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            mSensorService.startListeningSensors();//TODO check if it is not already listening
            fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };

    private static IntentFilter makeUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SensorService.ACTION_DATA_AVAILABLE);
        return intentFilter;
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
}
