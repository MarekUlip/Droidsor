package com.example.marekulip.droidsor.logs;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.SensorItem;
import com.example.marekulip.droidsor.adapters.SensorDataDispArrAdapter;
import com.example.marekulip.droidsor.database.SensorLogsTable;
import com.example.marekulip.droidsor.database.SensorsDataDbHelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Marek Ulip on 24-Sep-17.
 */

public class LogsFragment extends ListFragment {
    private static final String TAG = LogsFragment.class.toString();
    private SensorDataDispArrAdapter adapter;
    private CursorAdapter mAdapter;
    private List<SensorItem> items = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.getListView().setDividerHeight(2);
        registerForContextMenu(getListView());
        //showLogs();
        //adapter = new SensorDataDispArrAdapter(getContext(), R.layout.sensor_data_displayer_list_item,items);
        SensorsDataDbHelper dbHelper = SensorsDataDbHelper.getInstance(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.rawQuery("SELECT * FROM "+SensorLogsTable.TABLE_NAME,null);
        mAdapter = new LogsFragmentCursorAdapter(getContext(),c,0);
        getListView().setAdapter(mAdapter);
        //Log.d(TAG, "onActivityCreated: ");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(getActivity(),LogDetailActivity.class);
        intent.putExtra("id",(int)mAdapter.getItemId(position));
        startActivity(intent);
    }

    private class LogsFragmentCursorAdapter extends CursorAdapter{

        public LogsFragmentCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            String name = prepText(cursor);
            TextView tv = new TextView(context);
            tv.setText(name);
            return tv;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = prepText(cursor);
            ((TextView)view).setText(name);
        }

        private String prepText(Cursor cursor){
            String name =  cursor.getString(cursor.getColumnIndexOrThrow(SensorLogsTable.LOG_NAME));
            long start = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_START));
            long end = cursor.getLong(cursor.getColumnIndexOrThrow(SensorLogsTable.DATE_OF_END));
            DateFormat.getDateTimeInstance().format(new Date(start));
            name += System.lineSeparator()+DateFormat.getDateTimeInstance().format(new Date(start))+ " - " + DateFormat.getDateTimeInstance().format(new Date(end));;
            return name;
        }


    }
}
