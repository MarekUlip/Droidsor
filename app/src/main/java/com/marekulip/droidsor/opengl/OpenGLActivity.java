package com.marekulip.droidsor.opengl;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.DroidsorService;
import com.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

/**
 * Acitivty used to display 3D cube to show how data from sensors look in space.
 */
public class OpenGLActivity extends AppCompatActivity {
    /**
     * Surface view from Android
     */
    private GLSurfaceView mGLSurfaceView;
    /**
     * Renderer that does actual drawing
     */
    private DroidsorRenderer renderer;
    /**
     * Service used to retrieve data from sensors
     */
    private DroidsorService mDroidsorService;
    /**
     * Type of a sensor to get data from
     */
    private int sensorType;
    /**
     * Connection to service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mDroidsorService = ((DroidsorService.LocalBinder)service).getService();
            mDroidsorService.startOpenGLMode();
            mDroidsorService.setMode(DroidsorService.ALL_SENSORS_MODE);
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDroidsorService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorType = SensorsEnum.INTERNAL_ACCELEROMETER.sensorType;
        mGLSurfaceView = new GLSurfaceView(this);

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;


        if (supportsEs2)
        {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below.
            renderer = new DroidsorRenderer();
            mGLSurfaceView.setRenderer(renderer);
        }
        else
        {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }

        setContentView(mGLSurfaceView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.opel_gl_menu,menu);
        //Check if gyroscope is present. Accelerometer should always be present so no need to check.
        if(mDroidsorService!= null){
            menu.findItem(R.id.action_gyroscope_internal).setVisible(mDroidsorService.isSensorPresent(SensorsEnum.INTERNAL_GYROSCOPE.sensorType));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_accelerometer_internal: sensorType = SensorsEnum.INTERNAL_ACCELEROMETER.sensorType;
                break;
            case R.id.action_gyroscope_internal: sensorType = SensorsEnum.INTERNAL_GYROSCOPE.sensorType;
                break;
            case R.id.action_accelerometer_external: sensorType = SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType;
                break;
            case R.id.action_gyroscope_external: sensorType = SensorsEnum.EXT_MOV_GYROSCOPE.sensorType;
                break;
        }
        return true;
    }

    @Override
    protected void onResume()
    {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        connectToService();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause()
    {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        disconnectFromService();
        mGLSurfaceView.onPause();
    }

    /**
     * Determines whether specified service is running. Not 100% reliable. Usable at approximately 80% use cases.
     * @param serviceClass Service to check
     * @return true if service runs otherwise false
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connects to service or truns service on if it is not running
     */
    private void connectToService(){
        Intent intent = new Intent(this,DroidsorService.class);
        if(!isMyServiceRunning(DroidsorService.class) || DroidsorService.isServiceOff()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            }else{
                startService(intent);
            }
        }
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
        registerReceiver(mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    private static IntentFilter makeUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DroidsorService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void disconnectFromService(){
        if(mDroidsorService ==null)return;
        mDroidsorService.stopOpenGLMode();
        unregisterReceiver(mSensorServiceUpdateReceiver);
        unbindService(mServiceConnection);
    }
    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DroidsorService.ACTION_DATA_AVAILABLE.equals(action)) {
                renderer.setData(mDroidsorService.getSensorDataSparseArray().get(sensorType));
            }else if (BluetoothSensorManager.ACTION_GATT_CONNECTED.equals(action) ||BluetoothSensorManager.ACTION_GATT_DISCONNECTED.equals(action) ) {
                invalidateOptionsMenu();
            }
        }
    };
}
