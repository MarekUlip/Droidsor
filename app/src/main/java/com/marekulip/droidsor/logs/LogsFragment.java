package com.marekulip.droidsor.logs;

import android.content.ContentValues;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.marekulip.droidsor.database.PlaceholderMaker;
import com.marekulip.droidsor.database.SenorDataItemsCountTable;
import com.marekulip.droidsor.database.SensorDataTable;
import com.marekulip.droidsor.database.SensorLogsTable;
import com.marekulip.droidsor.gpxfileexporter.LogExporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment used to display list of logs
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = LogsFragment.class.toString();
    /**
     * Adapter used do display list of logs
     */
    private LogsFragmentCursorAdapter mAdapter;

    /**
     * Indicates whether mark more feature is enabled
     */
    private boolean isSelectionModeOn = false;
    /**
     * List of log ids used with mark more feature
     */
    private final List<Long> items = new ArrayList<>();

    /**
     * String used within adapter to display translated word "time"
     * It is made this way so that the adapter doesn't have to load it all the time the list is scrolled
     */
    private static String timeString = null;
    /**
     * String used within adapter to display translated words "Unknown duration"
     * It is made this way so that the adapter doesn't have to load it all the time the list is scrolled
     */
    private static String unknownDurationString = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(timeString == null){
            timeString = getContext().getString(R.string.time);
        }
        if(unknownDurationString == null){
            unknownDurationString = getContext().getString(R.string.unknown_duration);
        }

        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
        initCursorAdapter();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Mark more feature disabled open log detail
        if(!isSelectionModeOn){
            Intent intent = new Intent(getActivity(),LogDetailActivity.class);
            intent.putExtra("id",(int)mAdapter.getItemId(position));
            startActivity(intent);}
        else {
            // Mark more feature enabled add or remove item from the list
            if(items.contains(id)){
                items.remove(id);
                v.setBackgroundColor(Color.TRANSPARENT);
            }else{
                items.add(id);
                v.setBackgroundColor(Color.GRAY);
            }
            mAdapter.setItemsList(items);

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.mark_more_delete_export_menu,menu);
        if(isSelectionModeOn){
            menu.findItem(R.id.action_cancel).setVisible(true);
            menu.findItem(R.id.action_mark_more).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_export_selected).setVisible(true);
        }else{
            menu.findItem(R.id.action_cancel).setVisible(false);
            menu.findItem(R.id.action_mark_more).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_export_selected).setVisible(false);
        }
        super.onCreateOptionsMenu(menu,inflater);
    }

    /**
     * Tells whether mark more feature is enabled
     * @return true for enable otherwise false
     */
    public boolean isSelectionModeOn(){
        return isSelectionModeOn;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_mark_more:
                setSelectionMode(true);
                break;
            case R.id.action_cancel:
                setSelectionMode(false);
                break;
            case R.id.action_delete:
                deleteItemDialog(-1);
                break;
            case R.id.action_export_selected:
                exportMore();
                setSelectionMode(false);
                break;
        }
        return true;
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
        getActivity().invalidateOptionsMenu();
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
     * Deletes specified log
     * @param id Id of the log to be deleted
     */
    private void deleteItem(final long id){
        final Context appContext = getContext().getApplicationContext();
        // First delete log so list can be shown without it
        appContext.getContentResolver().delete(DroidsorProvider.SENSOR_LOGS_URI,SensorLogsTable._ID+" = ?",new String[]{String.valueOf(id)});
        Toast.makeText(appContext,R.string.deleted,Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Finish deleting the rest of the log on new thread so it does not block UI.
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_COUNT_URI, SenorDataItemsCountTable.LOG_ID + " = ?",new String[]{String.valueOf(id)});
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_URI,SensorDataTable.LOG_ID + " = ?",new String[]{String.valueOf(id)});

            }
        }).start();
    }

    /**
     * Deletes all logs selected with mark more feature
     */
    private void deleteMore(){
        if(items.isEmpty()){
            setSelectionMode(false);
            return;
        }
        final Context appContext = getContext().getApplicationContext();
        final String placeholders = PlaceholderMaker.makePlaceholders(items.size());
        final String[] params = new String[items.size()];
        PlaceholderMaker.makeParameters(params,items);
        // First delete logs so list can be shown without them
        appContext.getContentResolver().delete(DroidsorProvider.SENSOR_LOGS_URI,SensorLogsTable._ID + " IN ("+placeholders+")",params);
        Toast.makeText(appContext,R.string.deleted,Toast.LENGTH_SHORT).show(); //TODO consider better way of informing user that items are deleted - this is like lying.
        // Disable mark more feature and show refreshed list
        setSelectionMode(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Aaand finish deleting the rest of the log on new thread so it does not block UI.
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_COUNT_URI, SenorDataItemsCountTable.LOG_ID + " IN ("+placeholders+")",params);
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_URI,SensorDataTable.LOG_ID + " IN ("+placeholders+")",params);

                /*if(getActivity()!=null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }*/
            }
        }).start();
    }

    /**
     * Exports all logs selected with mark more feature
     */
    private void exportMore(){
        for(Long item:items){
            LogExporter.exportLog(getContext(),item,null);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,Menu.FIRST,0,R.string.export_log);
        menu.add(0, Menu.FIRST+1,0,getString(R.string.delete));
        menu.add(0,Menu.FIRST+2,0,R.string.rename);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(item.getItemId()==Menu.FIRST) LogExporter.exportLog(getContext(),info.id,null);
        if(item.getItemId()==Menu.FIRST+1) {
            deleteItemDialog(info.id);
        }
        if(item.getItemId() == Menu.FIRST+2) renameItem(info.id);
        return super.onContextItemSelected(item);
    }

    /**
     * Renames specified log
     * @param id id of the log to be renamed
     */
    private void renameItem(final long id){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final EditText editText = new EditText(getContext());
        builder.setTitle(R.string.choose_new_log_name).setView(editText).setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ContentValues cv = new ContentValues();
                cv.put(SensorLogsTable.LOG_NAME,editText.getText().toString());
                getContext().getContentResolver().update(DroidsorProvider.SENSOR_LOGS_URI,cv,SensorLogsTable._ID + " = ?",new String[]{String.valueOf(id)});
                initCursorAdapter();
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
     * Deletes specified log
     * @param id Id of the log to be deleted
     */
    private void deleteItemDialog(final long id){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.confirm_delete).setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(isSelectionModeOn)deleteMore();
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
     * Initialize or resets cursor adapter responsible for showing logs
     */
   private void initCursorAdapter(){
        getLoaderManager().initLoader(0,null,this);
        mAdapter = new LogsFragmentCursorAdapter(getContext(),android.R.layout.simple_list_item_1,null,new String[]{},new int[]{android.R.id.text1},0);
        getLoaderManager().restartLoader(0,null,this);
        getListView().setAdapter(mAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),DroidsorProvider.SENSOR_LOGS_URI,null,null,null,null);
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
     * Adapter used for displaying logs
     */
    private class LogsFragmentCursorAdapter extends SimpleCursorAdapter{
        /**
         * List of log ids selected with mark more feature
         */
        private List<Long> items = new ArrayList<>();

        public LogsFragmentCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = prepText(cursor);
            ((TextView)view.findViewById(android.R.id.text1)).setText(name);
            if(items.contains(cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable._ID)))){
                view.setBackgroundColor(Color.GRAY);
            }else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        /**
         * Prepares text to show in the list for the log contained in cursor
         * @param cursor Cursor from which data will be drawn
         * @return String to be shown
         */
        private String prepText(Cursor cursor){
            String name =  cursor.getString(cursor.getColumnIndexOrThrow(SensorLogsTable.LOG_NAME));
            long start = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_START));
            long end = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_END));
            name += System.lineSeparator()+timeString+getElapsedTime(start,end);
            return name;
        }

        /**
         * Set list of ids for mark more feature
         * @param ids list of ids
         */
        private void setItemsList(List<Long> ids){
            items = ids;
        }

        /**
         * Creates elapsed time of log string
         * @param start Time of log start
         * @param end Time of log end
         * @return Formatted time of log string
         */
        private String getElapsedTime(long start, long end){
            long different = end - start;
            if(different < 0) return unknownDurationString;
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;

            different = different % daysInMilli;

            long elapsedHours = different / hoursInMilli;
            different = different % hoursInMilli;

            long elapsedMinutes = different / minutesInMilli;
            different = different % minutesInMilli;

            long elapsedSeconds = different / secondsInMilli;

            return convertLessThanTen(elapsedHours)+":"+convertLessThanTen(elapsedMinutes)+":"+convertLessThanTen(elapsedSeconds);
        }

        /**
         * Makes sure that numbers lesser than ten have prepended zero.
         * @param num Number to be converted
         * @return Correct string
         */
        private String convertLessThanTen(long num){
            if(num<10)return "0"+num;
            else return String.valueOf(num);
        }


    }
}
