package com.marekulip.droidsor

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.marekulip.droidsor.adapters.SettingsNotificationListAdapter
import com.marekulip.droidsor.contentprovider.DroidsorProvider
import com.marekulip.droidsor.database.NotificationsSettingsTable
import com.marekulip.droidsor.droidsorservice.DroidsorService
import com.marekulip.droidsor.droidsorservice.ServiceConnectionHelper
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum

import kotlinx.android.synthetic.main.activity_settings_notifications.*

class SettingsNotificationsActivity : AppCompatActivity(),SettingsNotificationListAdapter.AdapterCallbacks, DialogFragmentNotificationsSettings.OnFragmentInteractionListener {
    var adapter:SettingsNotificationListAdapter? = null
    /**
     * Service which provides available sensors
     */
    private var mDroidsorService: DroidsorService? = null

    override fun getActiveItem(id: Int): SettingsNotificationListAdapter.AdapterListItem {
        return adapter?.activeItem!!
    }

    override fun saveActiveSensorInfo() {
        val info = adapter?.activeItem?.getInformations(this)
        for (item: SettingsNotificationListAdapter.AdapterListItem.ItemDetails in info!!.iterator()){
            val whereClause = NotificationsSettingsTable.SENSOR_ID + " = ? AND "+NotificationsSettingsTable.VALUE_NUMBER + " = ?"
            val whereArgs = arrayOf(item.sensorId.toString(),item.num.toString())
            contentResolver.update(DroidsorProvider.NOTIFICATIONS_SETTINGS_URI,item.getContentValues(),whereClause,whereArgs)
        }
        mDroidsorService?.invalidateNotificationsSettings()
    }

    override fun showDialog() {
        val dialog = DialogFragmentNotificationsSettings.newInstance(adapter?.activeItem!!.sensorId)
        dialog.show(supportFragmentManager,"setSensorNotificationDialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_notifications)
        setSupportActionBar(toolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =LinearLayoutManager(this)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        adapter = SettingsNotificationListAdapter(SettingsNotificationListAdapter.NotifDiffCallback(),this, this)
        recyclerView.adapter = adapter

    }

    private fun initAdapter(){
        val availableSensors = mDroidsorService?.sensorTypesForProfile
        val adapterList = ArrayList<SettingsNotificationListAdapter.AdapterListItem>()
        for(sensorID:Int in availableSensors!!){
            val sensorEnum = SensorsEnum.resolveEnum(sensorID)
            adapterList.add(SettingsNotificationListAdapter.AdapterListItem(sensorEnum.getSensorName(this),sensorID,PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DroidsorSettingsFramgent.NOTIFICATION_DISPLAY + sensorID, false)))
        }
        adapter?.submitList(adapterList)
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mDroidsorService = (service as DroidsorService.LocalBinder).service
            initAdapter()
            invalidateOptionsMenu()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mDroidsorService = null
        }
    }

    override fun onResume() {
        super.onResume()
        connectToService()
        invalidateOptionsMenu()
    }

    public override fun onPause() {
        super.onPause()
        mDroidsorService?.invalidateNotificationsSettings()
        disconnectFromService()
    }

    private fun connectToService() {
        ServiceConnectionHelper.connectToService(this, mServiceConnection, null, null)
    }

    private fun disconnectFromService() {
        ServiceConnectionHelper.disconnectFromService(this, mDroidsorService, null, mServiceConnection)
    }


}
