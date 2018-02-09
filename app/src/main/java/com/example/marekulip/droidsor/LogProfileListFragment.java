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

import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.LogProfileItemsTable;
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
        if(item.getItemId()==Menu.FIRST) deleteItem(info.id);
        return super.onContextItemSelected(item);
    }

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
        Cursor c = getContext().getContentResolver().query(DroidsorProvider.LOG_PROFILE_URI,new String[]{LogProfilesTable._ID,LogProfilesTable.PROFILE_NAME},null,null,null);
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
            exitPickingMode();
            ((LogProfileListFragmentListener)getActivity()).profilePicked(id);
        }
    }

    private void deleteItem(long id){
        getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI, LogProfileItemsTable.PROFILE_ID + " = ?",new String[]{String.valueOf(id)});
        getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_URI,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)});
        destroyCursorAdapter();
        initCursorAdapter();
    }

    public boolean isPickingModeOn(){
        return isPickingModeOn;
    }

    public interface LogProfileListFragmentListener{
        void changePickingMode(boolean on);
        void profilePicked(long id);
    }


}
