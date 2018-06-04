package com.marekulip.droidsor.droidsorservice;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Service
 */
public class ServiceStopperService extends Service {

    private DroidsorService mDroidsorService;

    public ServiceStopperService() {
    }

    @Override
    public void onCreate(){
        Intent intent = new Intent(this,DroidsorService.class);
        if(isMyServiceRunning(DroidsorService.class) || !DroidsorService.isServiceOff()){
            bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
        }else {
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mDroidsorService = ((DroidsorService.LocalBinder)service).getService();
            mDroidsorService.stop(false);
            unbindService(mServiceConnection);
            ServiceStopperService.this.stopSelf();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDroidsorService = null;
        }
    };

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
