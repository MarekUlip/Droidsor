package com.marekulip.droidsor

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.marekulip.droidsor.adapters.SettingsNotificationListAdapter


private const val ARG_ID = "arg_id"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DialogFragmentNotificationsSettings.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DialogFragmentNotificationsSettings.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DialogFragmentNotificationsSettings : DialogFragment() {
    private var sensorListId: Int? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sensorListId = it.getInt(ARG_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val parentLayout = inflater.inflate(R.layout.dialog_settings_notification_layout, container, false)
        val layout = parentLayout.findViewById<TableLayout>(R.id.table_sensors)
        val item = listener?.getActiveItem(sensorListId!!)
        for (info in item?.getInformations(context!!)!!){
            val row = TableRow(context)
            row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            row.tag = info.getTag()
            val subview = inflater.inflate(R.layout.dialog_notif_sub_view,container,false)
            subview.findViewById<CheckBox>(R.id.check_disp_val).isChecked = info.isShowable
            subview.findViewById<CheckBox>(R.id.check_tresh_set).isChecked = info.isThreshSet
            subview.findViewById<EditText>(R.id.edit_tresh_val).setText(info.threshVal.toString())
            subview.findViewById<TextView>(R.id.textview_sensor_name).setText(info.name)
            row.addView(subview)
            layout.addView(row)
        }
        parentLayout.findViewById<Button>(R.id.button_confirm).setOnClickListener {
            for(info in item.getInformations(context!!)){
                val row = layout.findViewWithTag<TableRow>(info.getTag()).getChildAt(0)
                info.isShowable = row.findViewById<CheckBox>(R.id.check_disp_val).isChecked
                info.isThreshSet = row.findViewById<CheckBox>(R.id.check_tresh_set).isChecked
                info.threshVal = row.findViewById<EditText>(R.id.edit_tresh_val).text.toString().toFloat()
            }
            listener?.saveActiveSensorInfo()
            dismiss()
        }
        parentLayout.findViewById<Button>(R.id.button_cancel).setOnClickListener { dismiss() }
        return parentLayout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun saveActiveSensorInfo()
        fun getActiveItem(id:Int):SettingsNotificationListAdapter.AdapterListItem
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment DialogFragmentNotificationsSettings.
         */
        @JvmStatic
        fun newInstance(param1: Int) =
                DialogFragmentNotificationsSettings().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_ID, param1)
                    }
                }
    }
}
