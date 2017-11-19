package com.example.marekulip.droidsor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;


public class LogProfileListFragment extends ListFragment {
    private SimpleCursorAdapter cursorAdapter;
    private boolean isPickingModeOn = false;

    public LogProfileListFragment() {
    }

    public static LogProfileListFragment newInstance() {
        LogProfileListFragment fragment = new LogProfileListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout,container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, Menu.FIRST,0,getString(R.string.delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(item.getItemId()==Menu.FIRST) deleteItem((int)info.id);
        return super.onContextItemSelected(item);
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_create_new:
                startActivity(new Intent(getContext(),LogProfileSettingActivity.class));
                break;
            case R.id.action_pick_favorite:
                enterPickingMode();
                break;
            case R.id.action_cancel:
                exitPickingMode();
                break;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(isPickingModeOn)inflater.inflate(R.menu.log_profile_menu,menu);
        else inflater.inflate(R.menu.cancel_menu,menu);
    }*/

    @Override
    public void onResume() {
        super.onResume();
        initCursorAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
        destroyCursorAdapter();
    }

    public void enterPickingMode(){
        isPickingModeOn = true;
        ((LogProfileListFragmentListener)getActivity()).changePickingMode(isPickingModeOn);
    }

    public void exitPickingMode(){
        isPickingModeOn = false;
        ((LogProfileListFragmentListener)getActivity()).changePickingMode(isPickingModeOn);
    }


    private void initCursorAdapter(){
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(LogProfilesTable.TABLE_NAME,new String[]{LogProfilesTable._ID,LogProfilesTable.PROFILE_NAME},null,null,null,null,null);
        cursorAdapter = new SimpleCursorAdapter(getContext(),android.R.layout.simple_list_item_1,c,new String[]{LogProfilesTable.PROFILE_NAME},new int[]{android.R.id.text1},0);
        setListAdapter(cursorAdapter);
    }

    private void destroyCursorAdapter(){
        cursorAdapter.getCursor().close();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(!isPickingModeOn) {
            Intent intent = new Intent(getContext(), LogProfileSettingActivity.class);
            intent.putExtra(LogProfileSettingActivity.LOG_PROFILE_ID, (int) id);
            intent.putExtra(LogProfileSettingActivity.IS_NEW, false);
            startActivity(intent);
        } else {
            SharedPreferences settings = getActivity().getSharedPreferences(SensorDataDisplayerActivity.SHARED_PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(SensorDataDisplayerActivity.FAVORITE_LOG, (int)id);
            editor.apply();
            Toast.makeText(getContext(),R.string.favorite_log_picked,Toast.LENGTH_SHORT).show();
            exitPickingMode();
        }
    }

    private void deleteItem(int id){
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        database.delete(LogProfilesTable.TABLE_NAME,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)});
        database.close();
        dbHelper.close();
        destroyCursorAdapter();
        initCursorAdapter();
    }

    public boolean isPickingModeOn(){
        return isPickingModeOn;
    }

    public interface LogProfileListFragmentListener{
        void changePickingMode(boolean on);
    }


}
