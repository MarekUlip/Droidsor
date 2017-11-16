package com.example.marekulip.droidsor;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

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


    private boolean isNew = false;
    private SensorService mSensorService;
    private LogProfileItemArrAdapter mAdapter;
    private List<LogProfileItem> items; //= new ArrayList<>();

    public LogProfileSettingFragment() {

    }

    public static LogProfileSettingFragment newInstance() {
        LogProfileSettingFragment fragment = new LogProfileSettingFragment();
        Bundle args = new Bundle();
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());

        items = new ArrayList<>();
        mAdapter = new LogProfileItemArrAdapter(getContext(),R.layout.profile_list_item,items);
        setListAdapter(mAdapter);

        Intent intent = new Intent(getContext(),SensorService.class);
        getActivity().bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
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
        getActivity().unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSensorService = ((SensorService.LocalBinder)service).getService();
            createLogProfileItemList();
           // mAdapter.setItems(items);
            //Log.d("Test", "onServiceConnected: "+mAdapter.getItems().size());//TODO Possible slowdown
            mAdapter.notifyDataSetChanged();
            //getListView().invalidateViews();
            //Log.d("Test", "onServiceConnected: ");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSensorService = null;
        }
    };

    private void createLogProfileItemList(){
        for(Integer i: mSensorService.getMonitoredSensorsTypes(true)){
            items.add(new LogProfileItem(i));
        }
        //return logProfileItems;
    }

    public void saveProfile(){//TODO Toast
        if(!isNew){
            SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(LogProfilesTable.SAVE_LOCATION,0);
            cv.put(LogProfilesTable.PROFILE_NAME,"ProfileTest");
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
            database.close();
            dbHelper.close();
        }
    }
    /*public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/
}
