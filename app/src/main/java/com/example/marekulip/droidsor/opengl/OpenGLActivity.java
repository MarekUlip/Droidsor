package com.example.marekulip.droidsor.opengl;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ConfigurationInfo;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.marekulip.droidsor.SensorService;
import com.example.marekulip.droidsor.bluetoothsensormanager.BluetoothSensorManager;

import java.util.List;

public class OpenGLActivity extends AppCompatActivity {
    private GLSurfaceView mGLSurfaceView;
    private DroidsorRenderer renderer;
    private SensorService mSensorService;
    public final static String SENSOR_TYPE = "sensor_type";
    private int sensorType;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            mSensorService.startOpenGLMode();
            mSensorService.setMode(SensorService.ALL_SENSORS_MODE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorType = getIntent().getIntExtra(SENSOR_TYPE,0);
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
        registerReceiver(mSensorServiceUpdateReceiver,makeUpdateIntentFilter());
    }

    private static IntentFilter makeUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SensorService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void disconnectFromService(){
        if(mSensorService==null)return;
        mSensorService.stopOpenGLMode();
        unregisterReceiver(mSensorServiceUpdateReceiver);
        unbindService(mServiceConnection);
    }
    private final BroadcastReceiver mSensorServiceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorService.ACTION_DATA_AVAILABLE.equals(action)) {
                renderer.setData(mSensorService.getSensorDataSparseArray().get(sensorType));
            }else if (BluetoothSensorManager.ACTION_GATT_CONNECTED.equals(action) ||BluetoothSensorManager.ACTION_GATT_DISCONNECTED.equals(action) ) {
                invalidateOptionsMenu();
            }
        }
    };
}
