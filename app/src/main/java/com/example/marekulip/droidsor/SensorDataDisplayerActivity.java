package com.example.marekulip.droidsor;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.LogProfileItemsTable;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.logs.LogsActivity;
import com.example.marekulip.droidsor.positionmanager.PositionManager;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;

import java.util.List;

public class SensorDataDisplayerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PositionManager.OnRecievedPositionListener, WaitForGPSDialog.WaitForGPSIFace {

    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String SHARED_PREFS_NAME = "droidsor_prefs";
    public static final String FAVORITE_LOG = "favorite_log";
    public static final int BT_DEVICE_REQUEST = 2;
    private SensorDataDispListFragment fragment;
    private FloatingActionButton fab;
    private SensorService mSensorService;
    private PositionManager positionManager;
    private LogProfile profileHolder;
    private boolean isRecording = false;
    private boolean recievedLocation = false;
    private DialogFragment waitForGPSDialog;

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
        navigationView.setCheckedItem(R.id.mobile_sensors);

        fab = findViewById(R.id.sens_disp_fab);
        setFabClickListener();


        fragment = new SensorDataDispListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment, fragment).commit();

        Intent intent = new Intent(this,SensorService.class);
        if(!isMyServiceRunning(SensorService.class)){
            startService(intent);
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
        if(mSensorService==null)return;//TODO solve service leakage on orientation change
        if(!mSensorService.isLogging())mSensorService.stopListeningSensors();
        unregisterReceiver(mSensorServiceUpdateReceiver);
        unbindService(mServiceConnection);
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
                startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
                break;
            case R.id.action_bluetooth_disconnect:
                mSensorService.disconnectFromBluetoothDevice();
                break;
        }
        return true;
    }

    private void setFabClickListener(){
        if(mSensorService!=null && mSensorService.isLogging()){
            fab.setImageResource(R.drawable.ic_action_stop);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSensorService.stopLogging();
                    isRecording = false;
                    //invalidateOptionsMenu();
                    setFabClickListener();
                    fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
                }
            });
        }else{
            fab.setImageResource(R.drawable.ic_action_record);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startLogging(false);
                }
            });
        }
    }

    private void tryToInitPosManager(){
        positionManager = new PositionManager(this);
        positionManager.setOnRecievedPositionListener(this);
        positionManager.initPosManager(this);//TODO edit is optainable at start of this method
        Log.d("PosManagerTest", "onOptionsItemSelected: "+PositionManager.isObtainable());
    }

    private void createTempLogProfile(){
        Intent intent = new Intent(this,LogProfileSettingActivity.class);
        intent.putExtra(LogProfileSettingActivity.IS_SETTING_TEMP_PROFILE,true);
        startActivityForResult(intent,LogProfileSettingActivity.CREATE_TEMP_PROFILE);
    }

    /**
     * Function to set up logging profile
     * @param isTemp
     */
    private void startLogging(boolean isTemp){
        if(mSensorService.isLogging())return;
        if(isTemp){
            mSensorService.startTempProfileLogging();
        }else {
            String pref = PreferenceManager.getDefaultSharedPreferences(this).getString(DroidsorSettingsFramgent.START_LOG_BUT_BEHAVIOUR_PREF,"1");
            if (pref.equals("1")) {
                if (getSharedPreferences(SHARED_PREFS_NAME, 0).getLong(FAVORITE_LOG, 0) == 0) {
                    Intent intent = new Intent(this,LogProfileActivity.class);
                    intent.putExtra(LogProfileActivity.IS_PICKING_FAVORITE_PROFILE,true);
                    Toast.makeText(this, R.string.pick_favorite_log, Toast.LENGTH_SHORT).show();
                    startActivityForResult(intent,LogProfileActivity.SET_FIRST_FAVORITE_PROFILE);
                    return;
                }
                LogProfile p = getProfile(-1);
                if(p==null){
                    Intent intent = new Intent(this,LogProfileActivity.class);
                    intent.putExtra(LogProfileActivity.IS_PICKING_FAVORITE_PROFILE,true);
                    Toast.makeText(this, R.string.favorite_profile_gone, Toast.LENGTH_SHORT).show();
                    startActivityForResult(intent,LogProfileActivity.SET_FIRST_FAVORITE_PROFILE);
                    return;
                }
                startLoggingWithPicked(p);
                /*if(p.isSaveGPS())tryToInitPosManager();
                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.WAIT_FOR_GPS_PREF,false)){
                    profileHolder = p;
                    return;
                }
                mSensorService.startLogging(p);//TODO set up GPS check if some sensor is from bluetooth if so try to connect to bluetooth*/
            } else if(pref.equals("2")){
                Intent intent = new Intent(this,LogProfileActivity.class);
                intent.putExtra(LogProfileActivity.IS_PICKING_NEXT_TO_LOG,true);
                Toast.makeText(this,R.string.pick_profile_to_log,Toast.LENGTH_SHORT).show();
                startActivityForResult(intent,LogProfileActivity.SET_NEXT_TO_LOG);
                return;
            }
            /*isRecording = true;
            setFabClickListener();
            fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(true));*/
        }
    }

    /**
     * Starts the logging
     * @param profile
     */
    private void startLoggingWithPicked(LogProfile profile){
        if(mSensorService==null || mSensorService.isLogging())return;
        if(profile.isSaveGPS()&&!recievedLocation){
            tryToInitPosManager();
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.WAIT_FOR_GPS_PREF,false)){
                profileHolder = profile;
                waitForGPSDialog = new WaitForGPSDialog();
                waitForGPSDialog.setCancelable(false);
                waitForGPSDialog.show(getSupportFragmentManager(),"WaitForGPSDialog");
                //TODO make dialogue about waiting for GPS
                return;
            }
        }

        mSensorService.startLogging(profile);
        isRecording = true;
        recievedLocation = false;
        //invalidateOptionsMenu();
        setFabClickListener();
        fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(true));
    }

    private void delayedOnActivityResult(int requestCode, int resultCode, Intent data){
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
        } else if(requestCode == LogProfileSettingActivity.CREATE_TEMP_PROFILE){
            if(resultCode==RESULT_OK){
                //if(mSensorService.getTempLogProfile().isSaveGPS())tryToInitPosManager();
                startLogging(true);
            }
        } else if(requestCode == LogProfileActivity.SET_FIRST_FAVORITE_PROFILE){
            if(resultCode==RESULT_OK){
                startLogging(false);
            }
        } else if (requestCode == LogProfileActivity.SET_NEXT_TO_LOG){
            if(resultCode == RESULT_OK){
                startLoggingWithPicked(getProfile(data.getLongExtra(LogProfileActivity.NEXT_LOG_ID,0)));
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(mSensorService==null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while(mSensorService==null)
                            Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            delayedOnActivityResult(requestCode,resultCode,data);
                        }
                    });

                }
            }).start();
            return;
        }
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
        } else if(requestCode == LogProfileSettingActivity.CREATE_TEMP_PROFILE){
            if(resultCode==RESULT_OK){
                //if(mSensorService.getTempLogProfile().isSaveGPS())tryToInitPosManager();
                startLogging(true);
            }
        } else if(requestCode == LogProfileActivity.SET_FIRST_FAVORITE_PROFILE){
            if(resultCode==RESULT_OK){
                startLogging(false);
            }
        } else if (requestCode == LogProfileActivity.SET_NEXT_TO_LOG){
            if(resultCode == RESULT_OK){
                startLoggingWithPicked(getProfile(data.getLongExtra(LogProfileActivity.NEXT_LOG_ID,0)));
            }
        }
    }

    private LogProfile getProfile(long id){
        if(id<0)id = getSharedPreferences(SHARED_PREFS_NAME,0).getLong(FAVORITE_LOG,0);
        LogProfile profile = new LogProfile();
        Cursor c = getContentResolver().query(DroidsorProvider.LOG_PROFILE_URI,null,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)},null);
        if(c!=null&&c.moveToFirst()){
            profile.setProfileName(c.getString(c.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME)));
            profile.setGPSFrequency(c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.GPS_FREQUENCY)));
            profile.setSaveGPS(c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.SAVE_LOCATION))!=0);
            c.close();
        } else{
            return null;
        }
        c = getContentResolver().query(DroidsorProvider.LOG_PROFILE_ITEMS_URI,new String[]{LogProfileItemsTable.SCAN_PERIOD,LogProfileItemsTable.SENSOR_TYPE},LogProfileItemsTable.PROFILE_ID+" = ?",new String[]{String.valueOf(id)},null);
        if(c!=null&&c.moveToFirst()){
            LogProfileItem item;
            item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)));
            profile.getLogItems().add(item);
            while (c.moveToNext()){
                item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)));
                profile.getLogItems().add(item);
            }
            c.close();
        }
        return profile;
    }

    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorService.ACTION_DATA_AVAILABLE.equals(action)) {
                /*if(mSensorService.getSensorDataQueue().isEmpty())return;
                fragment.setNewData(mSensorService.getSensorDataQueue());*/
                List<Integer> sensorTypes = mSensorService.getSensorTypesOccured();
                if(sensorTypes==null)return;
                fragment.setNewData(sensorTypes, mSensorService.getSensorDataSparseArray());
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
            if(mSensorService.isLogging())Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
            else {
                mSensorService.setMode(SensorService.MOBILE_SENSORS_MODE);
                fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            }
        } else if (id == R.id.ble_sensors) {
            if(BluetoothAdapter.getDefaultAdapter() == null){
                Toast.makeText(this,R.string.error_bluetooth_not_supported,Toast.LENGTH_SHORT).show();
                ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(R.id.all_sensors);
                return true;
            }
            if(mSensorService.isBluetoothDeviceOn()){
                if(mSensorService.isLogging())Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
                else {
                    mSensorService.setMode(SensorService.BLUETOOTH_SENSORS_MODE);
                    fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
                }
            }
            else startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
        } else if (id == R.id.all_sensors) {
            if(mSensorService.isLogging())Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
            else {
                mSensorService.setMode(SensorService.ALL_SENSORS_MODE);
                fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            }
        } else if(id == R.id.logged_sensors){
            if(mSensorService.isLogging())fragment.setSensorsToShow(mSensorService.getMonitoredSensorsTypes(false));
            else Toast.makeText(this,getString(R.string.unavailable_when_not_logging),Toast.LENGTH_LONG).show();
        }
        else if(id == R.id.nav_logging){
            createTempLogProfile();
        }
        else if (id == R.id.nav_logs) {
            startActivity(new Intent(this,LogsActivity.class));
        } else if(id == R.id.nav_log_profiles_settings){
            startActivity(new Intent(this,LogProfileActivity.class));
        } else if (id == R.id.nav_settings){
            startActivity(new Intent(this,DroidsorSettingsActivity.class));
        }

        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        return true;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            mSensorService.startListeningSensors();
            setFabClickListener();
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

    @Override
    public void positionRecieved() {
        Log.d("posTest", "positionRecieved: ");
        if(waitForGPSDialog !=null){
            waitForGPSDialog.dismiss();
            waitForGPSDialog = null;
        }
        positionManager.cancelOnRecievedPositionListener();
        recievedLocation = true;
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.WAIT_FOR_GPS_PREF,false)){
            startLoggingWithPicked(profileHolder);
        }


    }

    @Override
    public void startWithNoGPS() {
        positionRecieved();
    }

    @Override
    public void cancelLog() {
        positionManager.cancelOnRecievedPositionListener();
    }
}
