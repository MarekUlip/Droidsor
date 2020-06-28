package com.marekulip.droidsor;

import androidx.lifecycle.Lifecycle;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.DialogFragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;
import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.LogProfileItemsTable;
import com.marekulip.droidsor.database.LogProfilesTable;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.droidsorservice.ServiceConnectionHelper;
import com.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.marekulip.droidsor.logs.LogsActivity;
import com.marekulip.droidsor.opengl.OpenGLActivity;
import com.marekulip.droidsor.positionmanager.PositionManager;
import com.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.marekulip.droidsor.sensorlogmanager.LogProfileItem;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Main page activity used to display data from sensors.
 */
public class SensorDataDisplayerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PositionManager.OnRecievedPositionListener, WaitForGPSDialog.WaitForGPSIFace {

    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String SHARED_PREFS_NAME = "droidsor_prefs";
    public static final String FAVORITE_LOG = "favorite_log";
    public static final int BT_DEVICE_REQUEST = 2;
    private SensorDataDispListFragment fragment;
    private FloatingActionButton fab;
    private DroidsorService mDroidsorService;
    private PositionManager positionManager;
    /**
     * Holder used when log cannot be started right away but profile was already loaded. For example
     * when waiting for first GPS position.
     */
    private LogProfile profileHolder;
    private boolean isRecording = false;
    private boolean recievedLocation = false;
    /**
     * Indicates that dialog could not be shown and wants to be shown as soon as possible
     */
    private boolean isRequestingDialog = false;
    /**
     * Semaphore used for waiting till connection to service has been created or restored
     */
    private final Semaphore serviceSemaphore = new Semaphore(0,true);
    /**
     * Semaphore used for waiting till dialog can be shown
     */
    private final Semaphore dialogSemaphore = new Semaphore(1,true);
    /**
     * Dialog to inform user that app waits for first GPS position till it will start logging and allows
     * user to start logging without this position
     */
    private DialogFragment waitForGPSDialog;
    private NavigationView drawerNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data_displ);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //drawer.setDrawerListener(toggle);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerNavigationView= findViewById(R.id.nav_view);
        drawerNavigationView.setNavigationItemSelectedListener(this);
        drawerNavigationView.setCheckedItem(R.id.all_sensors);
        boolean legacyBT = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.BT_LEGACY,false);
        if (!legacyBT){
            Menu navigationViewMenu = drawerNavigationView.getMenu();
            navigationViewMenu.findItem(R.id.ble_sensors).setVisible(false);
            navigationViewMenu.findItem(R.id.all_sensors).setVisible(false);
            drawerNavigationView.setCheckedItem(R.id.mobile_sensors);
            //drawerNavigationView.getHeaderView(0).findViewById(R.id.ble_sensors).setVisibility(View.GONE);
        }
        fab = findViewById(R.id.sens_disp_fab);

        fragment = new SensorDataDispListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment, fragment).commit();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !PermissionsActivity.hasAllPermissions(this)){
            Snackbar.make(findViewById(R.id.linearLayout),R.string.permissions_required,Snackbar.LENGTH_LONG)
            .setAction(R.string.action_settings, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(SensorDataDisplayerActivity.this,PermissionsActivity.class));
                }
            })
            .setActionTextColor(Color.BLUE)
            .show();
        }
        //Toast.makeText(this,"9",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(mDroidsorService.isLogging()) {
                Toast.makeText(this,getString(R.string.logging_on),Toast.LENGTH_SHORT).show();
                super.onBackPressed();
            }
            else {
                mDroidsorService.stop(true);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToService();
        if(isRequestingDialog){
            dialogSemaphore.release();
            isRequestingDialog = false;
        }
        invalidateOptionsMenu();
        setFabClickListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectFromService();
    }

    /**
     * Starts Droidsor service or connects to it if it already runs.
     */
    private void connectToService(){
        ServiceConnectionHelper.connectToService(this,mServiceConnection,mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    /**
     * Disconnects from Droidsor service but does not stop it.
     */
    private void disconnectFromService(){
        ServiceConnectionHelper.disconnectFromService(this,mDroidsorService,mSensorServiceUpdateReceiver,mServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_conn,menu);
        if(mDroidsorService !=null){
            boolean legacyBT = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.BT_LEGACY,false); //TODO add to DS fragment
            if(legacyBT) {
                if (mDroidsorService.isBluetoothDeviceOn()) {
                    menu.findItem(R.id.action_bluetooth_connect).setVisible(false);
                    menu.findItem(R.id.action_bluetooth_disconnect).setVisible(true);
                } else {
                    menu.findItem(R.id.action_bluetooth_connect).setVisible(true);
                    menu.findItem(R.id.action_bluetooth_disconnect).setVisible(false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_bluetooth_connect:
                if(DroidsorService.isServiceOff()){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            connectToService();
                            try {
                                serviceSemaphore.acquire();
                                //isServiceOff = false;
                                startActivityForResult(new Intent(SensorDataDisplayerActivity.this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    return true;
                }
                startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
                break;
            case R.id.action_bluetooth_disconnect:
                mDroidsorService.disconnectFromBluetoothDevice();
                break;
        }
        return true;
    }

    /**
     * Sets correct icon and action for Floating Action Button.
     */
    private void setFabClickListener(){
        if(mDroidsorService !=null && mDroidsorService.isLogging()){
            fab.setImageResource(R.drawable.ic_action_stop);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDroidsorService.stopLogging();
                    isRecording = false;
                    //invalidateOptionsMenu();
                    setFabClickListener();
                    setActualDisplayMode();
                    fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
                }
            });
        }else{
            fab.setImageResource(R.drawable.ic_action_record);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    prepareForLogging();
                }
            });
        }
    }

    /**
     * Attempt to get one position from GPS. Also used to get correct permissions.
     */
    private void tryToInitPosManager(){
        positionManager = new PositionManager(this);
        positionManager.setOnRecievedPositionListener(this);
        positionManager.initPosManager(this);
    }

    /**
     * Prepares for logging and ensures that logging will start correctly.
     */
    private void prepareForLogging(){
        if(DroidsorService.isServiceOff()){
            Toast.makeText(this,R.string.service_is_off_resetting,Toast.LENGTH_SHORT).show();
            disconnectFromService();
            connectToService();
            return;
        }
        if(mDroidsorService.isLoggingSafe())return;
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
            startLogging(p);
        } else if(pref.equals("2")){
            Intent intent = new Intent(this,LogProfileActivity.class);
            intent.putExtra(LogProfileActivity.IS_PICKING_NEXT_TO_LOG,true);
            Toast.makeText(this,R.string.pick_profile_to_log,Toast.LENGTH_SHORT).show();
            startActivityForResult(intent,LogProfileActivity.SET_NEXT_TO_LOG);
        }
    }

    /**
     * Starts the logging
     * @param profile Profile with which the logging will be done
     */
    private void startLogging(LogProfile profile){
        if(mDroidsorService ==null || mDroidsorService.isLogging())return;
        if(profile.isSaveGPS()&&!recievedLocation){
            tryToInitPosManager();
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.WAIT_FOR_GPS_PREF,false)){
                profileHolder = profile;
                waitForGPSDialog = new WaitForGPSDialog();
                waitForGPSDialog.setCancelable(false);
                waitForGPSDialog.show(getSupportFragmentManager(),"WaitForGPSDialog");
                return;
            }
        }

        mDroidsorService.startLogging(profile);
        isRecording = true;
        recievedLocation = false;
        //invalidateOptionsMenu();
        setFabClickListener();
        setActualDisplayMode();
        fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(true));
    }

    /**
     * Method designed to handle service unavailability issues. Should be called only from {@link #onActivityResult(int, int, Intent)} method.
     * @param requestCode
     * @param resultCode
     * @param data
     * @param waitedForService indicates whether semaphore for service has been used. If not and dialog is going to be used it is necessary to use dialog semaphore
     */
    private void delayedOnActivityResult(final int requestCode, final int resultCode, final Intent data,boolean waitedForService){
        if(requestCode == BT_DEVICE_REQUEST){
            if(resultCode==RESULT_OK) {
                mDroidsorService.connectToBluetoothDevice(data.getStringExtra(DEVICE_ADDRESS));
                mDroidsorService.setMode(DroidsorService.BLUETOOTH_SENSORS_MODE);
                fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            }else {
                mDroidsorService.setMode(DroidsorService.MOBILE_SENSORS_MODE);
                fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            }
        } else if(requestCode == PositionManager.REQUEST_CHECK_SETTINGS){
            if(resultCode==RESULT_OK) {
                if(positionManager==null) tryToInitPosManager();
                else positionManager.initPosManager(this);
            }
        } else if(requestCode == PositionManager.MY_PERMISSIONS_REQUEST_LOCATION_FINE){
            if(resultCode==RESULT_OK) {
                if(positionManager==null) tryToInitPosManager();
                else positionManager.initPosManager(this);
            }
        }  else if(requestCode == LogProfileActivity.SET_FIRST_FAVORITE_PROFILE){
            if(resultCode==RESULT_OK){
                if(!waitedForService){
                    requestDialog(getSharedPreferences(SHARED_PREFS_NAME, 0).getLong(FAVORITE_LOG, 0));
                }else prepareForLogging();
            }
        } else if (requestCode == LogProfileActivity.SET_NEXT_TO_LOG){
            if(resultCode == RESULT_OK){
                if(!waitedForService){
                    requestDialog(data.getLongExtra(LogProfileActivity.NEXT_LOG_ID,0));
                }else startLogging(getProfile(data.getLongExtra(LogProfileActivity.NEXT_LOG_ID,0)));
            }
        }
    }

    /**
     * Wait till displaying of dialog is possible. Then start logging with specified profile.
     * @param id Profile id
     */
    private void requestDialog(final long id){
        requestDialog(getProfile(id));
    }

    /**
     * Wait till displaying of dialog is possible. Then start logging with specified profile. This is preferred method over requestDialog(int)
     * @param profile Profile to log with
     */
    private void requestDialog(final LogProfile profile){
        if(getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)){
            startLogging(profile);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    isRequestingDialog = true;
                    if(!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)){
                        dialogSemaphore.acquire();
                    }
                    SensorDataDisplayerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startLogging(profile);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mDroidsorService == null || DroidsorService.isServiceOff()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // if service was stop by user reference to it is still alive and reconnection is in progress
                        // so this is to make sure that lock release from that reconnection will be ignored
                        // otherwise it would make "ghost log".
                        if (mDroidsorService != null) serviceSemaphore.acquire();
                        serviceSemaphore.acquire();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                delayedOnActivityResult(requestCode, resultCode, data, true);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return;
        }
        delayedOnActivityResult(requestCode, resultCode, data, false);
    }

    /**
     * Load profile from database based on provided id.
     * @param id Id of log profile.
     * @return Loaded LogProfile object if log profile has been found. Otherwise returns null.
     */
    private LogProfile getProfile(long id){
        if(id<0)id = getSharedPreferences(SHARED_PREFS_NAME,0).getLong(FAVORITE_LOG,0);
        LogProfile profile = new LogProfile();
        Cursor c = getContentResolver().query(DroidsorProvider.LOG_PROFILE_URI,null,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)},null);
        if(c!=null&&c.moveToFirst()){
            profile.setId(c.getLong(c.getColumnIndexOrThrow(LogProfilesTable._ID)));
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
            do{
                item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)));
                profile.getLogItems().add(item);
            }while (c.moveToNext());
            c.close();
        }
        return profile;
    }

    /**
     * Receiver used to receive broadcasts from Droidsor service.
     */
    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mDroidsorService == null)return;
            final String action = intent.getAction();
            if (DroidsorService.ACTION_DATA_AVAILABLE.equals(action)) {
                List<Integer> sensorTypes = mDroidsorService.getSensorTypesOccured();
                if(sensorTypes==null)return;
                fragment.setNewData(sensorTypes, mDroidsorService.getSensorDataSparseArray());
            }else if (BluetoothSensorManager.ACTION_GATT_CONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if(BluetoothSensorManager.ACTION_GATT_DISCONNECTED.equals(action) ){
                invalidateOptionsMenu();
                if(!mDroidsorService.isLogging()){
                    mDroidsorService.setMode(DroidsorService.MOBILE_SENSORS_MODE);
                    fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
                }
            } else if(DroidsorService.SERVICE_IS_TURNING_OFF.equals(action)){
                setFabClickListener();
            } else if(DroidsorService.SCHEDULED_LOG_STOP.equals(action)){
                setFabClickListener();
                setActualDisplayMode();
            }
        }
    };

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean isCorrect = true;

        if (id == R.id.mobile_sensors) {
            if(mDroidsorService.isLogging()){
                setActualDisplayMode();
                isCorrect =false;
                Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
            }
            else {
                mDroidsorService.setMode(DroidsorService.MOBILE_SENSORS_MODE);
                fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            }
        } else if (id == R.id.ble_sensors) {
            if(BluetoothAdapter.getDefaultAdapter() == null){
                Toast.makeText(this,R.string.error_bluetooth_not_supported,Toast.LENGTH_SHORT).show();
                ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(R.id.all_sensors);
                return true;
            }
            if(mDroidsorService.isBluetoothDeviceOn()){
                if(mDroidsorService.isLogging()){
                    setActualDisplayMode();
                    isCorrect =false;
                    Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
                }
                else {
                    mDroidsorService.setMode(DroidsorService.BLUETOOTH_SENSORS_MODE);
                    fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
                }
            }
            else startActivityForResult(new Intent(this,BLESensorLocateActivity.class),BT_DEVICE_REQUEST);
        } else if (id == R.id.all_sensors) {
            if(mDroidsorService.isLogging()){
                setActualDisplayMode();
                isCorrect = false;
                Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
            }
            else {
                mDroidsorService.setMode(DroidsorService.ALL_SENSORS_MODE);
                fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            }
        } else if(id == R.id.three_d){
            if(mDroidsorService.isLogging()){
                setActualDisplayMode();
                isCorrect = false;
                Toast.makeText(this,getString(R.string.unavailable_when_logging),Toast.LENGTH_LONG).show();
            } else{
                startActivity(new Intent(this, OpenGLActivity.class));
            }
        } else if(id == R.id.logged_sensors){
            if(mDroidsorService.isLogging())fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            else {
                    setActualDisplayMode();
                    isCorrect = false;
                    Toast.makeText(this,getString(R.string.unavailable_when_not_logging),Toast.LENGTH_LONG).show();
                }
        }
        else if (id == R.id.nav_logs) {
            startActivity(new Intent(this,LogsActivity.class));
        } else if(id == R.id.nav_log_profiles_settings){
            startActivity(new Intent(this,LogProfileActivity.class));
        } else if (id == R.id.nav_settings){
            startActivity(new Intent(this,DroidsorSettingsActivity.class));
        } else if (id == R.id.nav_notif_settings){
            startActivity(new Intent(this, SettingsNotificationsActivity.class));
        } else if(id == R.id.nav_permissions){
            startActivity(new Intent(this,PermissionsActivity.class));
        }

        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        return isCorrect;
    }

    /**
     * Highlights correct menu item in drawer menu.
     */
    private void setActualDisplayMode(){
        int mode = mDroidsorService.getMode();
        if(mDroidsorService.isLogging()){
            drawerNavigationView.setCheckedItem(R.id.logged_sensors);
        } else if(mode == DroidsorService.MOBILE_SENSORS_MODE){
            drawerNavigationView.setCheckedItem(R.id.mobile_sensors);
        } else if(mode == DroidsorService.BLUETOOTH_SENSORS_MODE){
            drawerNavigationView.setCheckedItem(R.id.ble_sensors);
        } else if(mode == DroidsorService.ALL_SENSORS_MODE){
            drawerNavigationView.setCheckedItem(R.id.all_sensors);
        }
    }

    /**
     * Connection to Droidsor service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mDroidsorService = ((DroidsorService.LocalBinder)service).getService();
            mDroidsorService.startListeningSensors();
            setFabClickListener();
            fragment.setSensorsToShow(mDroidsorService.getMonitoredSensorsTypes(false));
            invalidateOptionsMenu();
            setActualDisplayMode();
            serviceSemaphore.release();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDroidsorService = null;
        }
    };

    private static IntentFilter makeUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DroidsorService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothSensorManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothSensorManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(DroidsorService.SERVICE_IS_TURNING_OFF);
        intentFilter.addAction(DroidsorService.SCHEDULED_LOG_STOP);
        return intentFilter;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(profileHolder != null){
            outState.putLong("log_id",profileHolder.getId());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long id = savedInstanceState.getLong("log_id",-1);
        if(id > 0)profileHolder = getProfile(id);
    }

    @Override
    public void positionRecieved() {
        if(waitForGPSDialog !=null){
            waitForGPSDialog.dismiss();
            waitForGPSDialog = null;
        }
        if(positionManager!=null) {
            positionManager.cancelOnRecievedPositionListener();
            if (positionManager.getLocation() != null) {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putFloat(GPXExporter.GPX_LATITUDE, (float) positionManager.getLocation().getLatitude())
                        .putFloat(GPXExporter.GPX_LONGITUDE, (float) positionManager.getLocation().getLongitude()).apply();
            }
        }
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.WAIT_FOR_GPS_PREF,false)){
            if(profileHolder!=null) {
                recievedLocation = true;
                startLogging(profileHolder);
            }
        }
    }

    @Override
    public void startWithNoGPS() {
        positionRecieved();
    }

    @Override
    public void cancelLog() {
        if(positionManager!=null)positionManager.cancelOnRecievedPositionListener();
    }
}
