import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.adapters.AlarmAdapter
import com.dacs.vku.adapters.AlarmReceiver
import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.api.RetrofitInstance.Companion.api
import com.dacs.vku.models.Alarm
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AlarmFragment : Fragment() {

    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmManager: AlarmManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        val alarms = loadAlarmsFromDatabase()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        alarmAdapter = AlarmAdapter(alarms)
        recyclerView.adapter = this.alarmAdapter

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Set up swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                alarmAdapter.removeItem(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Setup FloatingActionButton to add alarm
        val fabAddNotification = view.findViewById<FloatingActionButton>(R.id.fabAddNotification)
        fabAddNotification.setOnClickListener {
            showAddAlarmDialog()
        }

        return view
    }

    private fun showAddAlarmDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_alarm, null)
        val editTextTitle = dialogLayout.findViewById<EditText>(R.id.editTextTitle)
        val buttonPickDate = dialogLayout.findViewById<Button>(R.id.buttonPickDate)
        val buttonPickTime = dialogLayout.findViewById<Button>(R.id.buttonPickTime)

        val calendar = Calendar.getInstance()

        buttonPickDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, y, m, d ->
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
            }, year, month, day).show()
        }

        buttonPickTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), { _, h, m ->
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, m)
            }, hour, minute, true).show()
        }

        builder.setView(dialogLayout)
        builder.setPositiveButton("Set Alarm") { _, _ ->
            val title = editTextTitle.text.toString()
            if (title.isNotEmpty()) {
                val alarm = Alarm(title, calendar.timeInMillis, true)
                alarmAdapter.addItem(alarm)
                setAlarm(alarm.dateTime, title)
//                saveAlarmToServer(alarm)
            } else {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

//    private fun saveAlarmToServer(alarm: Alarm) {
//        val apiService = RetrofitInstance.api
//        val call = apiService.addAlarm(alarm)
//
//        call.enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                if (response.isSuccessful) {
//                    Log.d("ScheduleFragment", "User data sent successfully")
//                } else {
//                    Log.e("ScheduleFragment", "Failed to send user data: ${response.errorBody()?.string()}")
//                    Toast.makeText(requireContext(), "Failed to send user data", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e("ScheduleFragment", "Error sending user data", t)
//                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
//            }
//        })
//
//    }

    @SuppressLint("MissingPermission")
    private fun setAlarm(timeInMillis: Long, message: String) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        intent.action = "com.dacs.vku.ALARM_TRIGGERED"
        intent.putExtra("message", message)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            (System.currentTimeMillis() / 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
    private fun loadAlarmsFromDatabase(): MutableList<Alarm> {
        // Your logic to load alarms from local database or any storage
        return mutableListOf()
    }
}
