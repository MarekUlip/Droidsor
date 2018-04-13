package com.marekulip.droidsor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.marekulip.droidsor.positionmanager.PositionManager;


/**
 * Activity used for finding BLE devices.
 */
public class BLESensorLocateActivity extends AppCompatActivity implements BLESensorLocateFragment.OnFragmentInteractionListener{

    /**
     * Fragment that displays list of found devices
     */
    private BLESensorLocateFragment fragment;
    /**
     * Android BT adapter used to check whether BT is available.
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Handler used for delayed stop of BLE device scan
     */
    private Handler mHanlder;

    /**
     * Id used for requesting permission to enable BT chip.
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Period after which scanning for BLE devices will stop
     */
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHanlder = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Check if BT is available
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.error_bluetooth_not_supported), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if BLE is available
        // It seems unnecessary but Android examples show it this way and one can never be too sure
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //It is required to search for BLE devices
        if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PositionManager.MY_PERMISSIONS_REQUEST_LOCATION_FINE);
        }

        fragment = new BLESensorLocateFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,fragment,"BLESensorLocateFragment").commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PositionManager.MY_PERMISSIONS_REQUEST_LOCATION_FINE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScan();
                } else {
                    Toast.makeText(this,R.string.permission_fine_location_not_granted,Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensor_locate, menu);
        if (!fragment.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.menu_refresh).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                fragment.clearAdapter();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        startScan();
    }

    /**
     * Checks if BT is enabled and if so starts scanning fo BLE devices
     */
    private void startScan(){
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }
        fragment.initAdapter();
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
            finish();
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        fragment.clearAdapter();
    }


    /**
     * Starts or stops scanning for BLE devices. This method only tells fragment to do actual starting or stopping.
     * If starting scan then it also sets {@link #mHanlder} to stop scanning after 10 seconds
     * @param enable true for enable false for disable
     */
    private void scanLeDevice(boolean enable){
        if(enable){
            mHanlder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fragment.scanLeDevice(false);
                    invalidateOptionsMenu();
                }
            },SCAN_PERIOD);
            fragment.scanLeDevice(true);
        }else {
            fragment.scanLeDevice(false);
        }
        invalidateOptionsMenu();
    }


    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        final Intent intent = new Intent();
        intent.putExtra(SensorDataDisplayerActivity.DEVICE_ADDRESS, device.getAddress());
        setResult(RESULT_OK,intent);
        finish();
    }
}
