package com.marekulip.droidsor.droidsorservice;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Class that helps with connecting and disconnecting from DroidsorService. Created to reduce duplicity and make connection stuff easier to maintain.
 */
public class ServiceConnectionHelper {

    /**
     * Checks whether provided service is running. Note that this method is not reliable if the service has run before and app has not been turned off. Android system may still run the service although no connection to it is possible so it is necessary to start new one.
     * @param serviceClass Service to be found
     * @return true if service was found otherwise false.
     */
    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts Droidsor service or connects to it if it already runs.
     * @param context Context of an activity that connects
     * @param mServiceConnection {@link ServiceConnection} to be binded
     * @param mSensorServiceUpdateReceiver {@link BroadcastReceiver} to be registered
     * @param intentFilter {@link IntentFilter} of items that activity is ready to receive
     */
    public static void connectToService(Context context, ServiceConnection mServiceConnection, BroadcastReceiver mSensorServiceUpdateReceiver, IntentFilter intentFilter){
        Intent intent = new Intent(context,DroidsorService.class);
        if(!isMyServiceRunning(DroidsorService.class,context) || DroidsorService.isServiceOff()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            }else{
                context.startService(intent);
            }
        }
        context.bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
        context.registerReceiver(mSensorServiceUpdateReceiver,intentFilter);
    }

    /**
     * Disconnects from service but does not stop it
     * @param context Context of an activity that disconnects
     * @param mDroidsorService Service that is to be disconnected
     * @param mSensorServiceUpdateReceiver {@link BroadcastReceiver} to be unregistered
     * @param mServiceConnection {@link ServiceConnection} to be unbinded
     */
    public static void disconnectFromService(Context context, DroidsorService mDroidsorService, BroadcastReceiver mSensorServiceUpdateReceiver,ServiceConnection mServiceConnection){
        if(mDroidsorService ==null)return;
        if(!mDroidsorService.isLogging()) mDroidsorService.stopListeningSensors();
        context.unregisterReceiver(mSensorServiceUpdateReceiver);
        context.unbindService(mServiceConnection);
    }
}
