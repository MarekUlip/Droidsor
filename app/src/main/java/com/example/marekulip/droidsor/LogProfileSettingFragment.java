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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.marekulip.droidsor.adapters.LogProfileItemArrAdapter;
import com.example.marekulip.droidsor.database.LogProfileItemsTable;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.sensorlogmanager.LogProfileItem;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfileSettingFragment extends ListFragment {


    private boolean isNew = true;
    private int profileId = 0;
    private SensorService mSensorService;
    private LogProfileItemArrAdapter mAdapter;
    private List<LogProfileItem> items;
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

        Intent intent = new Intent(getContext(),SensorService.class);
        getActivity().bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
    }

    /**
     * Loads profile name and frequency and its items. There are two queries required to do this.
     * @return
     */
    private List<LogProfileItem> loadProfile(){
        List<LogProfileItem> items = new ArrayList<>();
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(LogProfileItemsTable.TABLE_NAME,null,LogProfileItemsTable.PROFILE_ID+" = ?",new String[]{String.valueOf(profileId)},null,null,null);
        if(c!=null&&c.moveToFirst()){
            LogProfileItem item;
            item = new LogProfileItem(true,c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)),false);
            items.add(item);
            while (c.moveToNext()){
                item = new LogProfileItem(true,
                        c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SENSOR_TYPE)),
                        c.getInt(c.getColumnIndexOrThrow(LogProfileItemsTable.SCAN_PERIOD)),
                        true);
                items.add(item);
            }
            c.close();
        }

        c = database.query(LogProfilesTable.TABLE_NAME,null,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(profileId)},null,null,null);
        if(c!=null&&c.moveToFirst()){
            profileName = c.getString(c.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME));
            gpsFrequency = c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.GPS_FREQUENCY));
            scanGPS = c.getInt(c.getColumnIndexOrThrow(LogProfilesTable.SAVE_LOCATION)) != 0;
            c.close();
        }

        dbHelper.close();
        database.close();
        return items;
    }

    private void initAdapter(){
        if(isNew){
            items = new ArrayList<>();
        }else{
            items = loadProfile();
        }
        mAdapter = new LogProfileItemArrAdapter(getContext(),R.layout.profile_list_item,items);
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

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(mServiceConnection); //TODO solve leaking service problem
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            createLogProfileItemList();
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };

    private void createLogProfileItemList(){
        for(Integer i: mSensorService.getMonitoredSensorsTypes(true)){
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

    public void finishSaving(String name, int frequency, boolean scanGPS){
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if(name == null || name.length()==0)name = getString(R.string.untitled_profile);
        cv.put(LogProfilesTable.PROFILE_NAME,name);
        cv.put(LogProfilesTable.GPS_FREQUENCY,frequency);
        cv.put(LogProfilesTable.SAVE_LOCATION,scanGPS?1:0);
        if(isNew){
            long id = database.insert(LogProfilesTable.TABLE_NAME,null, cv);
            for(LogProfileItem item: items){
                if(item.isEnabled()) {
                    cv = new ContentValues();
                    cv.put(LogProfileItemsTable.PROFILE_ID, id);
                    cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                    cv.put(LogProfileItemsTable.SENSOR_TYPE, item.getSensorType());
                    database.insert(LogProfileItemsTable.TABLE_NAME,null, cv);
                }
            }
            Toast.makeText(getContext(),getString(R.string.saved),Toast.LENGTH_SHORT).show();
        } else {
            database.update(LogProfilesTable.TABLE_NAME,cv,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(profileId)});
            for(LogProfileItem item: items){
                if(item.isEnabled()) {
                    cv = new ContentValues();
                    cv.put(LogProfileItemsTable.SCAN_PERIOD, item.getScanFrequency());
                    database.update(LogProfileItemsTable.TABLE_NAME,cv,LogProfileItemsTable.PROFILE_ID +" = ? AND "+LogProfileItemsTable.SENSOR_TYPE+ " = ?",new String[]{String.valueOf(profileId),String.valueOf(item.getSensorType())});
                    //database.insert(LogProfileItemsTable.TABLE_NAME,null, cv);
                }
            }
            Toast.makeText(getContext(),getString(R.string.updated),Toast.LENGTH_SHORT).show();
        }

        database.close();
        dbHelper.close();
    }

    public void restartFragment(){
       initAdapter();
       createLogProfileItemList();
       mAdapter.notifyDataSetChanged();
    }
}
