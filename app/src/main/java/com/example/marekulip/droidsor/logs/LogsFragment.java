package com.example.marekulip.droidsor.logs;

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

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.SenorDataItemsCountTable;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.database.SensorLogsTable;
import com.example.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.example.marekulip.droidsor.gpxfileexporter.LogExporter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = LogsFragment.class.toString();
    private LogsFragmentCursorAdapter mAdapter;
    private boolean isSelectionModeOn = false;
    private List<Long> items = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
        if(!isSelectionModeOn){
            Intent intent = new Intent(getActivity(),LogDetailActivity.class);
            intent.putExtra("id",(int)mAdapter.getItemId(position));
            startActivity(intent);}
        else {
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

    public boolean isSelectionModeOn(){
        return isSelectionModeOn;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_mark_more) {
            setSelectionMode(true);
        }
        else if (id == R.id.action_cancel){
            setSelectionMode(false);
        }
        else if(id==R.id.action_delete){
            deleteItemDialog(-1);//deleteMore();
            //setSelectionMode(false);
        }
        else if(id==R.id.action_export_selected){
            exportMore();
            setSelectionMode(false);
        }
        //getActivity().invalidateOptionsMenu();
        return true;
    }

    public void setSelectionMode(boolean mode){
        isSelectionModeOn = mode;
        if(!mode){
            cancelSelection();
        }else {
            mAdapter.setItemsList(items);
        }
        getActivity().invalidateOptionsMenu();
    }
    private void cancelSelection(){
        items.clear();
        mAdapter.setItemsList(items);
        initCursorAdapter();
    }

    private void deleteMore(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Context appContext = getContext().getApplicationContext();
                String placeholders = makePlaceholders(items.size());
                String where = SenorDataItemsCountTable.LOG_ID + " IN ("+placeholders+")";
                String[] params = new String[items.size()];
                makeParameters(params,items);

                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_COUNT_URI, where,params);
                where = SensorDataTable.LOG_ID + " IN ("+placeholders+")";
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_DATA_URI,where,params);
                where = SensorLogsTable._ID + " IN ("+placeholders+")";
                appContext.getContentResolver().delete(DroidsorProvider.SENSOR_LOGS_URI,where,params);

                if(getActivity()!=null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(appContext,R.string.deleted,Toast.LENGTH_SHORT).show();
                            setSelectionMode(false);
                        }
                    });
                }
            }
        }).start();
        /*for(Long item : items){
            deleteItem(item);
        }*/
        //setSelectionMode(false);
        //initCursorAdapter();
    }

    private void exportMore(){
        for(Long item:items){
            LogExporter.exportLog(getContext(),item,null);
        }
    }

    private String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
    private void makeParameters(String[] params,List<Long> idList) {
        if (params.length < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            for (int i = 0; i < idList.size(); i++) {
                params[i] = String.valueOf(idList.get(i));
            }
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
        if(item.getItemId()==Menu.FIRST) LogExporter.exportLog(getContext(),info.id,null);//exportLog(info.id);
        if(item.getItemId()==Menu.FIRST+1) {
            deleteItemDialog(info.id);
            //deleteItem(info.id);
            //initCursorAdapter();
        }
        if(item.getItemId() == Menu.FIRST+2) renameItem(info.id);
        return super.onContextItemSelected(item);
    }

    private void exportLog(final long id){
        Toast.makeText(getContext(),R.string.started_exporting,Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<SensorData> data = new ArrayList<>();
                Cursor c = getContext().getApplicationContext().getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,SensorDataTable.LOG_ID+ " = ?",new String[]{String.valueOf(id)},null);
                if(c!=null && c.moveToFirst()) {
                    data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                            ,new Point3D(
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                    ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE)),
                            c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SPEED)),
                            c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.ACCURACY))
                    ));
                    while (c.moveToNext()) {
                        data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                                ,new Point3D(
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                        ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE)),
                                c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.SPEED)),
                                c.getFloat(c.getColumnIndexOrThrow(SensorDataTable.ACCURACY))
                        ));
                    }
                    c.close();
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
                    df.setTimeZone(tz);
                    GPXExporter.exportLogItems(data, "Log "+ id + " Exported at "+df.format(new Date(System.currentTimeMillis())), getContext().getApplicationContext());
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext().getApplicationContext(), R.string.exporting_done, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void deleteItem(long id){
        getContext().getContentResolver().delete(DroidsorProvider.SENSOR_DATA_COUNT_URI, SenorDataItemsCountTable.LOG_ID + " = ?",new String[]{String.valueOf(id)});
        getContext().getContentResolver().delete(DroidsorProvider.SENSOR_DATA_URI,SensorDataTable.LOG_ID + " = ?",new String[]{String.valueOf(id)});
        getContext().getContentResolver().delete(DroidsorProvider.SENSOR_LOGS_URI,SensorLogsTable._ID+" = ?",new String[]{String.valueOf(id)});
        //initCursorAdapter();
    }

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


    private class LogsFragmentCursorAdapter extends SimpleCursorAdapter{
        private List<Long> items = new ArrayList<>();
        //private static boolean isSelectionModeOn

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

        private String prepText(Cursor cursor){
            String name =  cursor.getString(cursor.getColumnIndexOrThrow(SensorLogsTable.LOG_NAME));
            long start = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_START));
            long end = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_END));
            //DateFormat.getDateTimeInstance().format(new Date(start));
            name += System.lineSeparator()+"Time: "+getElapsedTime(start,end);//
            // DateFormat.getDateTimeInstance().format(new Date(start))+ " - " + DateFormat.getDateTimeInstance().format(new Date(end));;
            return name;
        }

        private void setItemsList(List<Long> ids){
            items = ids;
        }

        private String getElapsedTime(long start, long end){
            long different = end - start;
            if(different < 0) return "Unknown duration";
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;

            //long elapsedDays = different / daysInMilli;
            different = different % daysInMilli;

            long elapsedHours = different / hoursInMilli;
            different = different % hoursInMilli;

            long elapsedMinutes = different / minutesInMilli;
            different = different % minutesInMilli;

            long elapsedSeconds = different / secondsInMilli;

            return convertLessThanTen(elapsedHours)+":"+convertLessThanTen(elapsedMinutes)+":"+convertLessThanTen(elapsedSeconds);
        }

        private String convertLessThanTen(long num){
            if(num<10)return "0"+num;
            else return String.valueOf(num);
        }


    }
}
