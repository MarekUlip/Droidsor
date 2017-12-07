package com.example.marekulip.droidsor.positionmanager;

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

import com.example.marekulip.droidsor.sensorlogmanager.PermissionHandlerIFace;
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
 * Created by Fredred on 13.09.2017.
 */

public class PositionManager {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION_FINE = 1;
    public static final int REQUEST_CHECK_SETTINGS = 3;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mLastLocation;
    private Context context;

    private static boolean isPositionObtainable = false;
    private boolean isTryingToGetFirstPosition = false;
    private boolean isGettingUpdates = false;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private long interval;
    private long fastestInterval;

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

    public PositionManager(Context context){
        this(UPDATE_INTERVAL_IN_MILLISECONDS,FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,context);
    }


    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                if(isTryingToGetFirstPosition) {
                    stopUpdates();
                    isTryingToGetFirstPosition = false;
                }
                //mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        };
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public boolean isObtainable(){
        return isPositionObtainable;
    }

    public void initPosManager(final Activity activity){
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            //noinspection MissingPermission
                           // mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                  //  mLocationCallback, Looper.myLooper());
                            isPositionObtainable = true;
                        }
                    })
                    .addOnFailureListener(activity, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            isPositionObtainable = false;
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        //TODO resolve
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
            isPositionObtainable = false;
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_LOCATION_FINE);
            ((PermissionHandlerIFace)activity).requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    public void tryInitPosManager(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            //noinspection MissingPermission
                            // mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            //  mLocationCallback, Looper.myLooper());
                            isPositionObtainable = true;
                            isTryingToGetFirstPosition = true;
                            startUpdates();
                        }
                    });
        }
    }

    public void setIntervals(int interval){
        mLocationRequest.setInterval(interval);
        if(interval>2000){
            mLocationRequest.setFastestInterval(interval / 2000);
        }
        else{
            mLocationRequest.setFastestInterval(interval);
        }
    }
    @SuppressWarnings("MissingPermission")
    public void startUpdates(){
        if(isPositionObtainable){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
            isGettingUpdates = true;
        }
    }

    public void stopUpdates(){
        if(isPositionObtainable && isGettingUpdates) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            isGettingUpdates = false;
        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }
    @SuppressWarnings("MissingPermission")
    public Location getLocation(){
        if(isPositionObtainable){
            return mLastLocation;
        }
        return null;
    }
}
