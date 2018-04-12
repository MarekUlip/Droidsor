package com.marekulip.droidsor.positionmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Class used to get GPS position with Fused Location Provider. Make sure you get first location from
 * activity before using it anywhere else as there might be permission problems if you don't.
 * Created by Marek Ulip on 13.09.2017.
 */

public class PositionManager {

    /**
     * Id for requesting fine location
     */
    public static final int MY_PERMISSIONS_REQUEST_LOCATION_FINE = 1;
    /**
     * Id for requesting of turning GPS on
     */
    public static final int REQUEST_CHECK_SETTINGS = 3;

    /**
     * From Android system. Its used to get GPS position.
     */
    private FusedLocationProviderClient mFusedLocationClient;
    /**
     * From Android system. Its used to check GPS settings and permission
     */
    private SettingsClient mSettingsClient;
    /**
     * From Android system. Its used to set GPS frequency and precession
     */
    private LocationRequest mLocationRequest;
    /**
     * From Android system. Its used in conjunction with {@link #mSettingsClient}
     */
    private LocationSettingsRequest mLocationSettingsRequest;
    /**
     * From Android system. Processes actual position changes
     */
    private LocationCallback mLocationCallback;
    /**
     * Last found location. Might not be accurate after a while without GPS
     */
    private Location mLastLocation;
    /**
     * Activity or service context
     */
    private Context context;
    /**
     * Interface for classes that wait till location is received
     */
    private OnRecievedPositionListener onRecievedPositionListener;

    /**
     * Indicates whether it is possible to obtain position based on settings. Note that it this indicator
     * does not tell that position is really obtainable it only tells that app has permission to try.
     */
    private static boolean isPositionObtainable = false;
    /**
     * Indicates if manager should stop receiving position after first acquired position. Its used to
     * prepare manager for providing position while logging so first log items also has position if not so accurate.
     */
    private boolean isTryingToGetFirstPosition = false;
    /**
     * Indicates whether the manager is receiving position
     */
    private boolean isGettingUpdates = false;
    /**
     * Base GPS update interval in milliseconds - position has to be delivered at least this fast
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * Fastest desired update interval in milliseconds - if position is acquired faster ie. from another app
     * then system notifies this manager at this interval.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Variable for base GPS update interval
     */
    private long interval;
    /**
     * Variable for fastest GPS update interval
     */
    private long fastestInterval;

    /**
     * Initializes PositionManager. Note that this manager should be first constructed within activity
     * with activity context before it can be used in Service. Otherwise permission errors might occur.
     * @param interval Desired GPS frequency - position is obtained at least this fast
     * @param fastestInterval Fastest GPS frequency - position may be obtained this fast
     * @param context Activity or Service.
     */
    public PositionManager(long interval, long fastestInterval, Context context){
        this.interval = interval;
        this.fastestInterval = fastestInterval;
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    /**
     * Basic constructor that initializes position manager to gain position at 10000 ms while fastest is 5000 ms.
     * Position gathering is stopped after first received position. To continue call {@link #startUpdates()} method.
     * @param context Activity or Service.
     */
    public PositionManager(Context context){
        this(UPDATE_INTERVAL_IN_MILLISECONDS,FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,context);
    }


    /**
     * Creates location callback so it can process one or more GPS callbacks depending on PositionManager mode.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                if(onRecievedPositionListener!=null)onRecievedPositionListener.positionRecieved();
                if(isTryingToGetFirstPosition) {
                    stopUpdates();
                    isTryingToGetFirstPosition = false;
                }
            }
        };
    }

    /**
     * Creates location request where it set GPS settings.
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Tells whether all setting and permissions are met to obtain position.
     * @return true if all requirements are met otherwise false
     */
    public static boolean isObtainable(){
        return isPositionObtainable;
    }

    /**
     * Initializes position manager so it can be able to get GPS position. This method makes sure that
     * all permissions and settings are in correct state. This method should only be called from activity
     * because service does not have the ability to request permissions.
     * @param activity Activity which will ask for permission and can process permission result.
     */
    public void initPosManager(final Activity activity){
        // First check if app can access fine location
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // if yes check if device meets required settings
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            // if yes try to acquire position
                            isPositionObtainable = true;
                            if(onRecievedPositionListener!=null){
                                isTryingToGetFirstPosition = true;
                                startUpdates();
                            }
                        }
                    })
                    .addOnFailureListener(activity, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // if not try to cope with it by asking user to activate GPS
                            isPositionObtainable = false;
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.e("InitError", "onFailure: "+sie.toString() );
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    Toast.makeText(activity,"Cannot get GPS location.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // if not ask for permission
            isPositionObtainable = false;
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_LOCATION_FINE);
        }
    }

    /**
     * This method tries to acquire position but if any permission or settings requirement is not met
     * it stops the attempts without trying to fix anything. It is suited to use from Service to acquire first position if possible.
     */
    public void tryInitPosManager(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            isPositionObtainable = true;
                            isTryingToGetFirstPosition = true;
                            startUpdates();
                        }
                    });
        }
    }

    /**
     * Set listener for acquired position
     * @param listener Listener which should be notified when position is acquired.
     */
    public void setOnRecievedPositionListener(OnRecievedPositionListener listener){
        onRecievedPositionListener = listener;
    }

    /**
     * Cancels listener so it cannot receive notification when position is acquired.
     */
    public void cancelOnRecievedPositionListener(){
        onRecievedPositionListener = null;
    }

    /**
     * Sets base GPS frequency and fastest frequency which is one half of base frequency.
     * @param interval frequency to be set
     */
    public void setIntervals(int interval){
        mLocationRequest.setInterval(interval);
        if(interval>2000){
            mLocationRequest.setFastestInterval(interval / 2);
        }
        else{
            mLocationRequest.setFastestInterval(interval);
        }
    }

    /**
     * Start obtaining GPS position. This method should only be called after successful {@link #initPosManager(Activity)} method call.
     * If it is called without that method it can cause permission not granted errors.
     */
    @SuppressWarnings("MissingPermission")
    public void startUpdates(){
        if(isPositionObtainable){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
            isGettingUpdates = true;
        }
    }

    /**
     * Stop obtaining GPS position. If no position is being obtained then this method has no effect.
     */
    public void stopUpdates(){
        if(isPositionObtainable && isGettingUpdates) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            isGettingUpdates = false;
        }
    }

    /**
     * Builds location settings request.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Get last acquired location.
     * @return Acquired location or null if no location has been acquired.
     */
    public Location getLocation(){
        if(isPositionObtainable){
            return mLastLocation;
        }
        return null;
    }


    /**
     * Interface for notifying when position has been acquired.
     */
    public interface OnRecievedPositionListener{
        /**
         * Notifies that manager has acquired position
         */
        void positionRecieved();
    }
}
