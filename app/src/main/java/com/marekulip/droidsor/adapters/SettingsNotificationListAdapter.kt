package com.marekulip.droidsor.adapters

import android.content.ContentValues
import android.content.Context
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.marekulip.droidsor.DroidsorSettingsFramgent
import com.marekulip.droidsor.R
import com.marekulip.droidsor.contentprovider.DroidsorProvider
import com.marekulip.droidsor.database.NotificationsSettingsTable
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum

class SettingsNotificationListAdapter(diffCallback:DiffUtil.ItemCallback<AdapterListItem>, val context:Context, val adapterCallbacks: AdapterCallbacks) : ListAdapter<SettingsNotificationListAdapter.AdapterListItem,SettingsNotificationListAdapter.ItemViewHolder>(diffCallback) {
    var activeItem:AdapterListItem? = null

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.checkIsActive.text = item.sensorName
        holder.checkIsActive.isChecked = item.isActive
        holder.checkIsActive.setOnCheckedChangeListener { _, b ->
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(DroidsorSettingsFramgent.NOTIFICATION_DISPLAY + item.sensorId, b).apply()
        }
        holder.buttonSettings.setOnClickListener {
            openDialogForSensor(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_notifications_settings, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        return ItemViewHolder(view)
    }

    fun openDialogForSensor(item:AdapterListItem){
        activeItem = item
        adapterCallbacks.showDialog()
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var checkIsActive:CheckBox
        var buttonSettings:Button
        init {
            checkIsActive = itemView.findViewById(R.id.checkbox_show_in_notificaitons)
            buttonSettings = itemView.findViewById(R.id.button_set_threshold)
        }
    }

    class AdapterListItem(var sensorName:String, var sensorId: Int, var isActive: Boolean){
        var isFullyLoaded = false
        private val sensorInformation = ArrayList<ItemDetails>()

        fun getInformations(context: Context):List<ItemDetails>{
            if (!isFullyLoaded){
                loadInformations(context)
            }
            return sensorInformation
        }

        fun loadInformations(context: Context){
            val whereClause = NotificationsSettingsTable.SENSOR_ID + " = ?"
            val whereArgs = arrayOf(sensorId.toString())
            val cursor = context.contentResolver.query(DroidsorProvider.NOTIFICATIONS_SETTINGS_URI,null, whereClause, whereArgs, null)
            if(cursor != null){

                if (cursor.count == 0){
                    loadBasicInformations(context)
                    isFullyLoaded = true
                    return
                }
                cursor.moveToFirst()
                do {
                    sensorInformation.add(ItemDetails(cursor.getInt(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.SENSOR_ID)),cursor.getInt(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.VALUE_NUMBER)),cursor.getInt(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.IS_DISPLAY_VALUE)) == 1,
                                cursor.getInt(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.IS_THRESHOLD)) == 1,
                                    cursor.getFloat(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.TRESHOLD_VAL)),cursor.getString(cursor.getColumnIndexOrThrow(NotificationsSettingsTable.DATA_NAME))))
                } while (cursor.moveToNext())
                cursor.close()
                isFullyLoaded = true
            }
        }

        private fun loadBasicInformations(context: Context){
            val sensorDesc = SensorsEnum.resolveEnum(sensorId)
            for (i in 0 until sensorDesc.itemCount){
                val itemDetails = ItemDetails(sensorId, i,true,false,0.0.toFloat(), sensorDesc.getDataDescriptions(context)[i])
                context.contentResolver.insert(DroidsorProvider.NOTIFICATIONS_SETTINGS_URI,itemDetails.getContentValues())
                sensorInformation.add(itemDetails)
            }
        }



        class ItemDetails(val sensorId:Int, val num:Int, var isShowable:Boolean, var isThreshSet:Boolean,var threshVal:Float, val name:String){
            fun getTag():String{
                return name+num
            }

            fun getContentValues():ContentValues{
                val cv = ContentValues()
                cv.put(NotificationsSettingsTable.DATA_NAME,name)
                cv.put(NotificationsSettingsTable.VALUE_NUMBER,num)
                cv.put(NotificationsSettingsTable.IS_DISPLAY_VALUE,if(isShowable) 1 else 0)
                cv.put(NotificationsSettingsTable.IS_THRESHOLD, if(isThreshSet) 1 else 0)
                cv.put(NotificationsSettingsTable.SENSOR_ID,sensorId)
                cv.put(NotificationsSettingsTable.TRESHOLD_VAL, threshVal)
                return cv
            }
        }
    }
    interface AdapterCallbacks{
        fun showDialog()
    }

    class NotifDiffCallback :DiffUtil.ItemCallback<AdapterListItem>(){
        override fun areItemsTheSame(oldItem: AdapterListItem, newItem: AdapterListItem): Boolean {
            return oldItem.sensorId == newItem.sensorId
        }

        override fun areContentsTheSame(oldItem: AdapterListItem, newItem: AdapterListItem): Boolean {
            return oldItem.sensorId == newItem.sensorId
        }

    }
}