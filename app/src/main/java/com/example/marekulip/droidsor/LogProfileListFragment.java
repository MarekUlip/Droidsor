package com.example.marekulip.droidsor;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;

import java.util.ArrayList;
import java.util.List;


public class LogProfileListFragment extends ListFragment {
    //private OnFragmentInteractionListener mListener;

    public LogProfileListFragment() {
    }

    public static LogProfileListFragment newInstance() {
        LogProfileListFragment fragment = new LogProfileListFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout,container,false);
    }

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
        setListAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,loadProfiles()));
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

    private List<String> loadProfiles(){//TODO refresh
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(LogProfilesTable.TABLE_NAME,new String[]{LogProfilesTable._ID,LogProfilesTable.PROFILE_NAME},null,null,null,null,null);
        List<String> items = new ArrayList<>();
        if(c!=null && c.moveToFirst()){
            items.add(c.getString(c.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME)));
            while (c.moveToNext()){
                items.add(c.getString(c.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME)));
            }
            c.close();
        }
        database.close();
        dbHelper.close();
        return items;
    }

    /*public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/
}
