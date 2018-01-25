package com.example.marekulip.droidsor.logs;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.SensorItem;
import com.example.marekulip.droidsor.adapters.SensorDataDispArrAdapter;
import com.example.marekulip.droidsor.contentprovider.DroidsorProvider;
import com.example.marekulip.droidsor.database.LogProfilesTable;
import com.example.marekulip.droidsor.database.SensorDataTable;
import com.example.marekulip.droidsor.database.SensorLogsTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;
import com.example.marekulip.droidsor.gpxfileexporter.GPXExporter;
import com.example.marekulip.droidsor.sensorlogmanager.Point3D;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = LogsFragment.class.toString();
    private SimpleCursorAdapter mAdapter;


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
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(getActivity(),LogDetailActivity.class);
        intent.putExtra("id",(int)mAdapter.getItemId(position));
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,Menu.FIRST,0,R.string.export_log);
        menu.add(0, Menu.FIRST+1,0,getString(R.string.delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(item.getItemId()==Menu.FIRST) exportLog((int)info.id);
        if(item.getItemId()==Menu.FIRST+1) deleteItem((int)info.id);
        return super.onContextItemSelected(item);
    }

    private void exportLog(final int id){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(),R.string.started_exporting,Toast.LENGTH_LONG).show();
                List<SensorData> data = new ArrayList<>();
                Cursor c = getContext().getContentResolver().query(DroidsorProvider.SENSOR_DATA_URI,null,SensorDataTable.LOG_ID+ " = ?",new String[]{String.valueOf(id)},null);
                if(c!=null && c.moveToFirst()) {
                    data.add(new SensorData(c.getInt(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_TYPE))
                            ,new Point3D(
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_X)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Y)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.SENSOR_VALUE_Z))
                    ), c.getLong(c.getColumnIndexOrThrow(SensorDataTable.TIME_OF_LOG)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LONGITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.LATITUDE)),
                            c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
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
                                c.getDouble(c.getColumnIndexOrThrow(SensorDataTable.ALTITUDE))
                        ));
                    }
                    c.close();
                    GPXExporter.exportLogItems(data, "Log "+ id + " Exported at " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()), getContext());
                    Toast.makeText(getContext(), R.string.exporting_done, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteItem(int id){
        getContext().getContentResolver().delete(DroidsorProvider.SENSOR_LOGS_URI,SensorLogsTable._ID+" = ?",new String[]{String.valueOf(id)});
        initCursorAdapter();
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


        public LogsFragmentCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = prepText(cursor);
            ((TextView)view.findViewById(android.R.id.text1)).setText(name);
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
