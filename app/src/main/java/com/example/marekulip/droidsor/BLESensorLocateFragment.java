package com.example.marekulip.droidsor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Fragment used to display and select BLE devices.
 *
 */
public class BLESensorLocateFragment extends ListFragment {

    /**
     * List adapter that shows found devices
     */
    private LeDeviceListAdapter mLeDeviceListAdapter;
    /**
     * Bluetooth adapter from Android system
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Indicates whether the device is searching for BLE devices
     */
    private boolean isScanning;
    /**
     * Callback interface for found devices
     */
    private OnFragmentInteractionListener mListener;

    public BLESensorLocateFragment() {
        // Required empty public constructor
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(isScanning){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            isScanning = false;
        }

        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if(device == null) return;
        mListener.onDeviceSelected(device);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
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
    }

    public boolean isScanning(){
        return isScanning;
    }

    public void scanLeDevice(boolean enable){
        if(enable){
            isScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void clearAdapter(){
        mLeDeviceListAdapter.clear();
    }

    public void initAdapter(){
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    private class LeDeviceListAdapter extends BaseAdapter {

        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter(){
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }

        BluetoothDevice getDevice(int postion){
            return mLeDevices.get(postion);
        }

        void clear(){
            mLeDevices.clear();
        }


        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if(view == null){
                view = mInflator.inflate(R.layout.listitem_sensor_locate,null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAdress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if(deviceName != null && deviceName.length()>0){
                viewHolder.deviceName.setText(deviceName);
            }else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAdress.setText(device.getAddress());

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(bluetoothDevice);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder{
        TextView deviceName;
        TextView deviceAdress;
    }

    /**
     * Interface used for notifying activity that device has been selected
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onDeviceSelected(BluetoothDevice device);
    }
}
