package com.marekulip.droidsor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import androidx.fragment.app.ListFragment;
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

    /**
     * Constructor
     */
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
        // Tell the activity that BLE device has been selected
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

    /**
     * Indicates whether scanning for BLE device is active
     * @return True if scanning is active otherwise false
     */
    public boolean isScanning(){
        return isScanning;
    }

    /**
     * Starts or stops BLE device scan. Should be called after {@link #initAdapter()} method otherwise
     * items won't have a place to display at.
     * @param enable true for start false for stop
     */
    public void scanLeDevice(boolean enable){
        if(enable){
            isScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * Clears items from list adapter
     */
    public void clearAdapter(){
        mLeDeviceListAdapter.clear();
    }

    /**
     * Initializes or restarts adapter call {@link #scanLeDevice(boolean)} after this method to start scan.
     */
    public void initAdapter(){
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
    }

    /**
     * Adapter used for displaying found BLE devices.
     */
    private class LeDeviceListAdapter extends BaseAdapter {

        /**
         * ArrayList of found BLE devices
         */
        private ArrayList<BluetoothDevice> mLeDevices;
        /**
         * Inflater used to inflate list view items
         */
        private LayoutInflater mInflator;

        /**
         * Simple constructor. Takes inflater from wrapping Activity or Fragment.
         */
        LeDeviceListAdapter(){
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getLayoutInflater();
        }

        /**
         * Adds found device into the list adapter
         * @param device Device to be added
         */
        public void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }

        /**
         * Gets device from specified position. Careful for {@link ArrayIndexOutOfBoundsException}.
         * @param position Position from which BT device should be acquired
         * @return BT device from position
         */
        BluetoothDevice getDevice(int position){
            return mLeDevices.get(position);
        }

        /**
         * Clears array list of this adapter.
         */
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
            // If device has a name use it
            if(deviceName != null && deviceName.length()>0){
                viewHolder.deviceName.setText(deviceName);
            }else {
                // Otherwise tell user that you don't know what that is
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAdress.setText(device.getAddress());

            return view;
        }
    }

    /**
     * Callback called after each found BT device.
     */
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
        /**
         * Method for notifying that BT device was selected
         * @param device Device that was selected.
         */
        void onDeviceSelected(BluetoothDevice device);
    }
}
