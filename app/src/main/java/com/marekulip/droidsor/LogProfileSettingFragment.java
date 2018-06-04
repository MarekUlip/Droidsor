package com.marekulip.droidsor;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.marekulip.droidsor.adapters.LogProfileItemArrAdapter;
import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.LogProfileItemsTable;
import com.marekulip.droidsor.database.LogProfilesTable;
import com.marekulip.droidsor.droidsorservice.DroidsorService;
import com.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;
import com.marekulip.droidsor.viewmodels.LogProfileViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek Ulip on 28.10.2017.
 * Fragment that displays list view of log profile items
 */

public class LogProfileSettingFragment extends ListFragment implements SetExtMovSensorDialogFragment.SetExtMovSensorIface{


    /**
     * Indicates it this profile is new or was loaded from database
     */
    private boolean isNew = true;
    /**
     * Id of a profile. 0 if creating new profile
     */
    private int profileId = 0;
    /**
     * Service from which log profile items are obtained
     */
    private DroidsorService mDroidsorService;
    /**
     * Adapter that manages set log profile items
     */
    private LogProfileItemArrAdapter mAdapter;
    /**
     * Items of {@link #mAdapter}
     */
    private List<LogProfileItem> items;
    /**
     * Map containing information if sensors that are part of movement sensor are enabled or disabled
     */
    private SparseBooleanArray extBluetoothMovSensorStates;
    /**
     * Name of loaded profile
     */
    private String profileName;
    /**
     * GPS frequency of loaded profile
     */
    private int gpsFrequency;
    /**
     * Indicates whether this profile should save GPS frequency
     */
    private boolean scanGPS;

    private LogProfileViewModel logProfileViewModel;

    public LogProfileSettingFragment() {

    }

    public static LogProfileSettingFragment newInstance(boolean isNew, int id) {
        LogProfileSettingFragment fragment = new LogProfileSettingFragment();
        Bundle args = new Bundle();
        args.putInt(LogProfileSettingActivity.LOG_PROFILE_ID,id);
        args.putBoolean(LogProfileSettingActivity.IS_NEW,isNew);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(getArguments() != null){
            if(!(isNew = getArguments().getBoolean(LogProfileSettingActivity.IS_NEW,true))){
                profileId = getArguments().getInt(LogProfileSettingActivity.LOG_PROFILE_ID);
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
        if(getActivity()!=null) {
            logProfileViewModel = ViewModelProviders.of(getActivity()).get(LogProfileViewModel.class);
        }
        initAdapter();

    }

    /**
     * Loads profile name and frequency and its items. There are two queries required to do this.
     * @return List of log profile items
     */
    private List<LogProfileItem> loadProfile(){
        List<LogProfileItem> items = new ArrayList<>();
        Cursor c = getContext().getContentResolver().query(DroidsorProvider.LOG_PROFILE_ITEMS_URI,null,LogProfileItemsTable.PROFILE_ID+" = ?",new String[]{String.valueOf(profileId)},null);
        if(c!=null&&c.moveToFirst()){
            LogProfileItem item;
            boolean extMovSet = false;
            do{
                item = new LogProfileItem(true,
                        c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),
                        c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)));
                if(item.sensorType>=100 && item.sensorType <= 102){
                    extBluetoothMovSensorStates.put(item.sensorType,true);
                    if(!extMovSet){
                        items.add(new LogProfileItem(true,SensorsEnum.EXT_MOVEMENT.sensorType,item.scanFrequency));
                        extMovSet = true;
                    }
                    continue;
                }
                items.add(item);
            }while (c.moveToNext());
            c.close();
        }
        c = getContext().getContentResolver().query(DroidsorProvider.LOG_PROFILE_URI,null,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(profileId)},null);
        if(c!=null&&c.moveToFirst()){
            profileName = c.getString(c.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME));
            gpsFrequency = c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.GPS_FREQUENCY));
            scanGPS = c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.SAVE_LOCATION)) != 0;
            c.close();
        }
        return items;
    }

    private void initAdapter(){
        extBluetoothMovSensorStates = new SparseBooleanArray();
        items = logProfileViewModel.getItems();
        if(!isNew){
            if(items.isEmpty()){
                items = loadProfile();
                logProfileViewModel.setItems(items);
            }
        }
        mAdapter = new LogProfileItemArrAdapter(getContext(),R.layout.profile_list_item,items,getActivity());
        mAdapter.setExtBluetoothMovSensorStates(extBluetoothMovSensorStates);
        //logProfileViewModel.setItems(items);
        setListAdapter(mAdapter);
    }

    private void createLogProfileItemList(){
        for(Integer i: mDroidsorService.getSensorTypesForProfile()){
            if(!isNew){
                int pos;
                for(pos = 0; pos<items.size();pos++){
                    if(items.get(pos).getSensorType() == i)break;
                }
                if(pos == items.size())items.add(new LogProfileItem(i));
            }else items.add(new LogProfileItem(i));
        }
    }

    /**
     * Shows dialog that allows to save this profile and set its name and gps options
     */
    public void saveProfile(){
        DialogFragment dialogFragment = new SaveProfileDialogFragment();
        if(!isNew){
            Bundle args = new Bundle();
            args.putString(SaveProfileDialogFragment.PROFILE_NAME,profileName);
            args.putInt(SaveProfileDialogFragment.GPS_FREQ,gpsFrequency);
            args.putBoolean(SaveProfileDialogFragment.SCAN_GPS,scanGPS);
            dialogFragment.setArguments(args);
        }
        dialogFragment.show(getActivity().getSupportFragmentManager(),"SaveProfileDialog");
    }

    /**
     * Finishes saving using items that were set in dialog
     * @param name Name of a profile
     * @param frequency GPS frequency
     * @param scanGPS true to scan otherwise false
     */
    public void finishSaving(String name, int frequency, boolean scanGPS){
        ContentValues cv = new ContentValues();
        if(name == null || name.length()==0)name = getString(R.string.untitled_profile);
        cv.put(LogProfilesTable.PROFILE_NAME,name);
        cv.put(LogProfilesTable.GPS_FREQUENCY,frequency);
        cv.put(LogProfilesTable.SAVE_LOCATION,scanGPS?1:0);
        int movScanFrequency = 1000;
        long id;
        if(isNew) {
            id = Integer.parseInt(getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_URI, cv).getLastPathSegment());
        } else {
            id = profileId;
            getContext().getContentResolver().update(DroidsorProvider.LOG_PROFILE_URI,cv,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)});
        }

        for(LogProfileItem item: items){
            if(item.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){//All sensors associated to this should be placed after
                movScanFrequency = item.scanFrequency;
                continue;
            }
            if(item.isEnabled()) {
                cv = new ContentValues();
                cv.put(LogProfileItemsTable.PROFILE_ID, id);
                cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                cv.put(LogProfileItemsTable.SENSOR_TYPE, item.getSensorType());
                insertOrUpdate(cv,item.sensorType);
            }else {
                getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(item.getSensorType())});
            }
        }
        for(int i = 0, size = extBluetoothMovSensorStates.size(),key; i< size;i++){
            key =extBluetoothMovSensorStates.keyAt(i);
            if(extBluetoothMovSensorStates.get(key,false)){
                cv = new ContentValues();
                cv.put(LogProfileItemsTable.PROFILE_ID, id);
                cv.put(LogProfileItemsTable.SCAN_PERIOD, movScanFrequency);
                cv.put(LogProfileItemsTable.SENSOR_TYPE, key);
                insertOrUpdate(cv,key);
            } else {
                getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(key)});
            }
        }
            Toast.makeText(getContext(),getString(R.string.saved),Toast.LENGTH_SHORT).show();
    }

    /**
     * Inserts provided values of one sensor of this log profile or updates them
     * @param cv items of a sensor to be inserted
     * @param sensorType type of a sensors that is being inserted
     */
    private void insertOrUpdate(ContentValues cv,int sensorType){
        if(isNew) getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv);
        else {
            if(getContext().getContentResolver().update(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(sensorType)}) == 0){
                getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv);
            }
        }
    }

    /**
     * Restarts adapter of this fragment
     */
    public void restartFragment(){
       initAdapter();
       createLogProfileItemList();
       mAdapter.notifyDataSetChanged();
    }

    public void setSensorService(DroidsorService droidsorService){
        mDroidsorService = droidsorService;
        initAdapter();
        createLogProfileItemList();
        mAdapter.notifyDataSetChanged();
    }

    public void setLogProfileViewModel(LogProfileViewModel model){
        logProfileViewModel = model;
    }

    @Override
    public void extMovSensorsSet(boolean acc, boolean gyr, boolean mag) {
        mAdapter.getExtBluetoothMovSensorStates().put(SensorsEnum.EXT_MOV_ACCELEROMETER.sensorType,acc);
        mAdapter.getExtBluetoothMovSensorStates().put(SensorsEnum.EXT_MOV_GYROSCOPE.sensorType,gyr);
        mAdapter.getExtBluetoothMovSensorStates().put(SensorsEnum.EXT_MOV_MAGNETIC.sensorType,mag);
        for(LogProfileItem item: mAdapter.getItems()){
            if(item.sensorType == SensorsEnum.EXT_MOVEMENT.sensorType){
                item.setEnabled(acc||gyr||mag);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
