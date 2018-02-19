package com.example.marekulip.droidsor;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.marekulip.droidsor.adapters.LogProfileItemArrAdapter;
import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.LogProfileItemsTable;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfile;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;
import com.example.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfileSettingFragment extends ListFragment implements SetExtMovSensorDialogFragment.SetExtMovSensorIface{


    private boolean isNew = true;
    private int profileId = 0;
    private SensorService mSensorService;
    private LogProfileItemArrAdapter mAdapter;
    private List<LogProfileItem> items;
    private SparseBooleanArray extBluetoothMovSensorStates;
    private String profileName;
    private int gpsFrequency;
    private boolean scanGPS;

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

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }*/

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }*/

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
        initAdapter();

    }

    /**
     * Loads profile name and frequency and its items. There are two queries required to do this.
     * @return
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
                if(item.sensorType>=100 && item.sensorType <= 102){ //TODO make boolean isStandalone
                    extBluetoothMovSensorStates.put(item.sensorType,true);
                    if(!extMovSet){
                        items.add(new LogProfileItem(true,SensorsEnum.EXT_MOVEMENT.sensorType,item.scanFrequency));
                        extMovSet = true;
                    }
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
        if(isNew){
            items = new ArrayList<>();
        }else{
            items = loadProfile();
        }
        mAdapter = new LogProfileItemArrAdapter(getContext(),R.layout.profile_list_item,items,getActivity());
        mAdapter.setExtBluetoothMovSensorStates(extBluetoothMovSensorStates);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_create_new:
                startActivity(new Intent(getContext(),LogProfileSettingActivity.class));
                break;
        }
        return true;
    }



    private void createLogProfileItemList(){
        for(Integer i: mSensorService.getSensorTypesForProfile()){
            if(!isNew){
                int pos;
                for(pos = 0; pos<items.size();pos++){
                    if(items.get(pos).getSensorType() == i)break;
                }
                if(pos == items.size())items.add(new LogProfileItem(i));
            }else items.add(new LogProfileItem(i));
        }
    }

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

   /* public void finishSaving(String name, int frequency, boolean scanGPS){
        ContentValues cv = new ContentValues();
        if(name == null || name.length()==0)name = getString(R.string.untitled_profile);
        cv.put(LogProfilesTable.PROFILE_NAME,name);
        cv.put(LogProfilesTable.GPS_FREQUENCY,frequency);
        cv.put(LogProfilesTable.SAVE_LOCATION,scanGPS?1:0);
        int movScanFrequency = 1000;
        if(isNew){

            long id = Integer.parseInt(getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_URI,cv).getLastPathSegment());
            for(LogProfileItem item: items){
                if(item.isEnabled()) {
                    if(item.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){//All sensors associated to this should be placed after
                        movScanFrequency = item.scanFrequency;
                        continue;
                    }
                    cv = new ContentValues();
                    cv.put(LogProfileItemsTable.PROFILE_ID, id);
                    if(item.sensorType>=100 && item.sensorType <=102){
                        cv.put(LogProfileItemsTable.SCAN_PERIOD, movScanFrequency);
                    }
                    else cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                    cv.put(LogProfileItemsTable.SENSOR_TYPE, item.getSensorType());
                    getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv);
                }
            }
            Toast.makeText(getContext(),getString(R.string.saved),Toast.LENGTH_SHORT).show();
        } else {
            getContext().getContentResolver().update(DroidsorProvider.LOG_PROFILE_URI,cv,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(profileId)});
            for(LogProfileItem item: items){
                if(item.isEnabled()) {
                    if(item.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){//All sensors associated to this should be placed after
                        movScanFrequency = item.scanFrequency;
                        continue;
                    }
                    cv = new ContentValues();
                    if(item.sensorType==SensorsEnum.EXT_MOVEMENT.sensorType){//TODO try to generalize it

                        for(int i = 0, size = extBluetoothMovSensorStates.size(),key; i< size;i++){
                            key =extBluetoothMovSensorStates.keyAt(i);

                        }
                        continue;
                    }
                    else cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                    //cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                    getContext().getContentResolver().update(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(item.getSensorType())});
                    //database.insert(LogProfileItemsTable.TABLE_NAME,null, cv);
                }else {
                    getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(item.getSensorType())});
                }
            }
            Toast.makeText(getContext(),getString(R.string.updated),Toast.LENGTH_SHORT).show();
        }
    }*/

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

    private void insertOrUpdate(ContentValues cv,int sensorType){
        if(isNew) getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv);
        else {
            if(getContext().getContentResolver().update(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(sensorType)}) == 0){
                getContext().getContentResolver().insert(DroidsorProvider.LOG_PROFILE_ITEMS_URI,cv);
            }
        }
    }

    public void restartFragment(){
       initAdapter();
       createLogProfileItemList();
       mAdapter.notifyDataSetChanged();
    }

    public void setSensorService(SensorService sensorService){
        mSensorService = sensorService;
        initAdapter();
        createLogProfileItemList();
        mAdapter.notifyDataSetChanged();
    }

    public LogProfile getTempLogProfile(String name, int frequency, boolean scanGPS){
        LogProfile tempLogProfile = new LogProfile();
        List<LogProfileItem> usedSensors = new ArrayList<>();
        for(LogProfileItem item: items) {
            if (item.isEnabled()) {
                usedSensors.add(item);
            }
        }
        tempLogProfile.setLogItems(usedSensors);
        tempLogProfile.setSaveGPS(scanGPS);
        tempLogProfile.setGPSFrequency(frequency);
        tempLogProfile.setProfileName(name);
        return tempLogProfile;
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
