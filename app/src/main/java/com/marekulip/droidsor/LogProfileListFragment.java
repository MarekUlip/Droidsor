package com.marekulip.droidsor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.LogProfileItemsTable;
import com.marekulip.droidsor.database.LogProfilesTable;
import com.marekulip.droidsor.database.PlaceholderMaker;
import com.marekulip.droidsor.database.SensorLogsTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to show list of profiles
 */
public class LogProfileListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private LogProfilesCursorAdapter mAdapter;
    private List<Long> items = new ArrayList<>();
    private boolean isPickingModeOn = false;
    private boolean isSelectionModeOn = false;
    private LogProfileListFragmentListener mListener;

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
        if(item.getItemId()==Menu.FIRST) {
            deleteItemDialog(info.id);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LogProfileListFragmentListener) {
            mListener = (LogProfileListFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        initCursorAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    /**
     * Enters or exits from picking mode.
     * @param enable
     */
    public void setPickingMode(boolean enable){
        isPickingModeOn = enable;
        mListener.changePickingMode(isPickingModeOn);
    }

    /**
     * Enables or disables mark more feature
     * @param mode true for enable otherwise false
     */
    public void setSelectionMode(boolean mode){
        isSelectionModeOn = mode;
        if(!mode){
            // if disabling feature clear all selected items
            cancelSelection();
        }else {
            mAdapter.setItemsList(items);
        }
        mListener.changeSelectionMode(isSelectionModeOn);
    }

    /**
     * Deletes all items selected with mark more option
     */
    public void deleteMarked(){
        deleteItemDialog(-1);
    }

    /**
     * Deletes selected items
     */
    private void deleteMore(){
        if(items.isEmpty()){
            setSelectionMode(false);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Context appContext = getContext().getApplicationContext();
                String placeholders = PlaceholderMaker.makePlaceholders(items.size());
                String where = LogProfilesTable._ID+ " IN ("+placeholders+")";
                String[] params = new String[items.size()];
                PlaceholderMaker.makeParameters(params,items);

                appContext.getContentResolver().delete(DroidsorProvider.LOG_PROFILE_URI, where,params);
                where = LogProfileItemsTable.PROFILE_ID + " IN ("+placeholders+")";
                appContext.getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI,where,params);

                if(getActivity()!=null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(appContext,R.string.deleted, Toast.LENGTH_SHORT).show();
                            setSelectionMode(false);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Deletes specified log
     * @param id Id of the log to be deleted
     */
    private void deleteItemDialog(final long id){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.confirm_delete).setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(isSelectionModeOn) deleteMore();
                else {
                    deleteItem(id);
                    initCursorAdapter();
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Clears all items from the {@link #items} list selected with mark more feature
     */
    private void cancelSelection(){
        items.clear();
        mAdapter.setItemsList(items);
        initCursorAdapter();
    }

    /**
     * Initialize or resets cursor adapter responsible for showing logs
     */
    private void initCursorAdapter(){
        getLoaderManager().initLoader(0,null,this);
        mAdapter = new LogProfilesCursorAdapter(getContext(),android.R.layout.simple_list_item_1,null,new String[]{},new int[]{android.R.id.text1},0);
        getLoaderManager().restartLoader(0,null,this);
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(isPickingModeOn) {
            setPickingMode(false);
            mListener.profilePicked(id);
        } else if(isSelectionModeOn){
            if(items.contains(id)){
                items.remove(id);
                v.setBackgroundColor(Color.TRANSPARENT);
            }else{
                items.add(id);
                v.setBackgroundColor(Color.GRAY);
            }
            mAdapter.setItemsList(items);
        } else {
            Intent intent = new Intent(getContext(), LogProfileSettingActivity.class);
            intent.putExtra(LogProfileSettingActivity.LOG_PROFILE_ID, (int) id);
            intent.putExtra(LogProfileSettingActivity.IS_NEW, false);
            startActivity(intent);

        }
    }

    /**
     * Deletes specified profile and restarts cursor adapter
     * @param id - id of a profile to be deleted
     */
    private void deleteItem(long id){
        getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_ITEMS_URI, LogProfileItemsTable.PROFILE_ID + " = ?",new String[]{String.valueOf(id)});
        getContext().getContentResolver().delete(DroidsorProvider.LOG_PROFILE_URI,LogProfilesTable._ID+" = ?",new String[]{String.valueOf(id)});
        initCursorAdapter();
    }

    /**
     * Indicates whether picking mode is on
     * @return true if mode is on otherwise false
     */
    public boolean isPickingModeOn(){
        return isPickingModeOn;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),DroidsorProvider.LOG_PROFILE_URI,null,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * Adapter used for displaying log profiles
     */
    private class LogProfilesCursorAdapter extends SimpleCursorAdapter {
        /**
         * List of profile ids selected with mark more feature
         */
        private List<Long> items = new ArrayList<>();

        public LogProfilesCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(LogProfilesTable.PROFILE_NAME));
            ((TextView) view.findViewById(android.R.id.text1)).setText(name);
            if (items.contains(cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable._ID)))) {
                view.setBackgroundColor(Color.GRAY);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        /**
         * Set list of ids for mark more feature
         * @param ids list of ids
         */
        private void setItemsList(List<Long> ids){
            items = ids;
        }
    }

    public interface LogProfileListFragmentListener{
        /**
         * Notifies about enabling or disabling
         * @param on true enable otherwise false
         */
        void changePickingMode(boolean on);

        /**
         * Notifies about selected profile
         * @param id id of a profile that was selected
         */
        void profilePicked(long id);

        /**
         * Notifies about enabling or disabling mark more feature.
         * @param on true enable otherwise false
         */
        void changeSelectionMode(boolean on);
    }


}
