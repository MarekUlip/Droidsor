package com.example.marekulip.droidsor;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ServiceStopperService extends Service {

    private SensorService mSensorService;

    public ServiceStopperService() {
    }

    @Override
    public void onCreate(){
        Intent intent = new Intent(this,SensorService.class);
        //TODO check if service is still running
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            mSensorService.stop(false);
            unbindService(mServiceConnection);
            ServiceStopperService.this.stopSelf();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };
}
