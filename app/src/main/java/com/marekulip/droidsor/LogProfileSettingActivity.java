package com.marekulip.droidsor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.droidsorservice.ServiceConnectionHelper;

import static com.marekulip.droidsor.SensorDataDisplayerActivity.BT_DEVICE_REQUEST;
import static com.marekulip.droidsor.SensorDataDisplayerActivity.DEVICE_ADDRESS;

/**
 * Activity used to create or edit log profile
 */
public class LogProfileSettingActivity extends AppCompatActivity implements SaveProfileDialogFragment.SaveProfileDialogListener, SetExtMovSensorDialogFragment.SetExtMovSensorIface{

    /**
     * Fragment containing list of log profile items
     */
    LogProfileSettingFragment fragment;
    /**
     * Service which provides available sensors
     */
    private DroidsorService mDroidsorService;
    /**
     * Indicator of log id that was sent via intent
     */
    public static final String LOG_PROFILE_ID = "log_id";
    /**
     * Indicator that log is new and not edited
     */
    public static final String IS_NEW = "is_new";

    private String storedAddress = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_sensor_d_displ);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.sens_disp_fab);
        fab.setImageResource(R.drawable.ic_action_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.saveProfile();
            }
        });


        fragment = LogProfileSettingFragment.newInstance(getIntent().getBooleanExtra(IS_NEW,true),getIntent().getIntExtra(LOG_PROFILE_ID,0));
        getSupportFragmentManager().beginTransaction().replace(R.id.sensor_list_fragment,fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_conn,menu);
        if(mDroidsorService !=null){
            if(mDroidsorService.isBluetoothDeviceOn()){
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
                mDroidsorService.disconnectFromBluetoothDevice();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BT_DEVICE_REQUEST){
            if(resultCode==RESULT_OK) {
                if(mDroidsorService == null)storedAddress = data.getStringExtra(DEVICE_ADDRESS);
                else {
                    mDroidsorService.connectToBluetoothDevice(data.getStringExtra(DEVICE_ADDRESS));
                    fragment.restartFragment();
                }
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
            mDroidsorService = ((DroidsorService.LocalBinder)service).getService();
            fragment.setSensorService(mDroidsorService);
            if(storedAddress != null){
                mDroidsorService.connectToBluetoothDevice(storedAddress);
                fragment.restartFragment();
            }
            storedAddress = null;
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDroidsorService = null;
        }
    };

    @Override
    public void saveProfile(String name, int frequency, boolean scanGPS) {
        fragment.finishSaving(name, frequency, scanGPS);
        finish();
    }

    private void connectToService(){
        ServiceConnectionHelper.connectToService(this,mServiceConnection,mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    private void disconnectFromService(){
        ServiceConnectionHelper.disconnectFromService(this,mDroidsorService,mSensorServiceUpdateReceiver,mServiceConnection);
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
