package com.dacs.vku.ui.fragments.Authentication

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.vku.R
import com.dacs.vku.adapters.ScheduleAdapter
import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.api.UserData
import com.dacs.vku.databinding.FragmentScheduleBinding
import com.dacs.vku.models.Schedule
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private lateinit var binding: FragmentScheduleBinding
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val scheduleList = mutableListOf<Schedule>()
    private var userData: UserData? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userData = it.getSerializable("userData") as? UserData
            uid = userData?.userId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()

        getAllSchedules()

        binding.fabAddSchedule.setOnClickListener {
            addSchedule()
        }

        // Set up RecyclerView
        scheduleAdapter = ScheduleAdapter(scheduleList,
            onEditClick = {
                schedule -> editSchedule(schedule)
                          },
            onDeleteClick = { schedule -> deleteSchedule(schedule) }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun addSchedule() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etTime = dialogView.findViewById<EditText>(R.id.etTime)
        val etDayOfWeek = dialogView.findViewById<EditText>(R.id.etDayOfWeek)
        val etSubject = dialogView.findViewById<EditText>(R.id.etEditSubject)
        val etRoom = dialogView.findViewById<EditText>(R.id.etEditRoom)
        val btnAddSchedule = dialogView.findViewById<Button>(R.id.btnAddSchedule)

        // Placeholder to store the captured day of the week
        var dayOfWeek: Int = -1

        // Set up listeners for date and time EditTexts
        etDate.setOnClickListener {
            showDatePickerDialog(etDate, etDayOfWeek) { selectedDayOfWeek ->
                dayOfWeek = selectedDayOfWeek
            }
        }

        etTime.setOnClickListener {
            showTimePickerDialog(etTime)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Schedule")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnAddSchedule.setOnClickListener {
            val subject = etSubject.text.toString()
            val room = etRoom.text.toString()
            val date = etDate.text.toString()
            val time = etTime.text.toString()

            if (subject.isEmpty() || date.isEmpty() || time.isEmpty() || room.isEmpty() || dayOfWeek == -1) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val scheduleId = UUID.randomUUID().toString()
                val userId = uid
                val dayOfWeekString = getDayOfWeekString(dayOfWeek)
                val scheduleData = Schedule(scheduleId, userId, dayOfWeekString, date, time, room, subject)

                // Add schedule to the list
                scheduleList.add(scheduleData)

                // Sort scheduleList by date and time
                val sortedList = scheduleList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                scheduleAdapter.updateScheduleList(sortedList.toMutableList())

                sendUserDataToServer(scheduleData)
                insertEventIntoCalendar(scheduleData)
                Toast.makeText(requireContext(), "Schedule added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
    private fun showDatePickerDialog(editText: EditText, dayOfWeekEditText: EditText, onDateSelected: (Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            editText.setText(formattedDate)

            // Set calendar to the selected date to get the day of the week
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)

            // Set the day of the week in the edit text
            dayOfWeekEditText.setText(getDayOfWeekString(dayOfWeek))

            onDateSelected(dayOfWeek)
        }, year, month, day)

        datePickerDialog.show()
    }
    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            editText.setText(formattedTime)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun getDayOfWeekString(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Chủ Nhật"
            Calendar.MONDAY -> "Thứ 2"
            Calendar.TUESDAY -> "Thứ 3"
            Calendar.WEDNESDAY -> "Thứ 4"
            Calendar.THURSDAY -> "Thứ 5"
            Calendar.FRIDAY -> "Thứ 6"
            Calendar.SATURDAY -> "Thứ 7"
            else -> ""
        }
    }

    private fun parseDate(date: String): Long {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.parse(date)?.time ?: 0L
    }

    private fun parseTime(time: String): Long {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.parse(time)?.time ?: 0L
    }


    // Function to insert Event and Save Event Id
    @SuppressLint("InlinedApi")
    private fun insertEventIntoCalendar(schedule: Schedule) {
        val cal = Calendar.getInstance()
        val dateParts = schedule.date.split("/")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Calendar.MONTH is 0-based
        val year = dateParts[2].toInt()
        val timeParts = schedule.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        cal.set(year, month, day, hour, minute)

        val startMillis: Long = cal.timeInMillis
        val endMillis: Long = cal.apply { add(Calendar.HOUR, 1) }.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, schedule.subject)
            putExtra(CalendarContract.Events.DESCRIPTION, "Schedule ID: ${schedule.scheduleId}\nUser ID: ${schedule.userId}")
            putExtra(CalendarContract.Events.EVENT_LOCATION, schedule.room)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=10")
        }
        startActivity(intent)
    }

    // Function to edit Event
    private fun editEventInCalendar(eventId: Long) {
        val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = uri
        }
        startActivity(intent)
    }






    private fun editSchedule(schedule: Schedule) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_schedule, null)
        val etEditSubject = dialogView.findViewById<EditText>(R.id.etEditSubject)
        val etEditDate = dialogView.findViewById<EditText>(R.id.etEditDate)
        val etEditTime = dialogView.findViewById<EditText>(R.id.etEditTime)
        val etEditRoom = dialogView.findViewById<EditText>(R.id.etEditRoom)
        val etDayOfWeek = dialogView.findViewById<EditText>(R.id.etDayOfWeek)
        val btnUpdateSchedule = dialogView.findViewById<Button>(R.id.btnUpdateSchedule)

        etEditSubject.setText(schedule.subject)
        etEditDate.setText(schedule.date)
        etEditTime.setText(schedule.time)
        etEditRoom.setText(schedule.room)
        etDayOfWeek.setText(schedule.dayOfWeek)

        var dayOfWeek: Int = -1

        etEditDate.setOnClickListener {
            showDatePickerDialog(etEditDate, etDayOfWeek) { selectedDayOfWeek ->
                dayOfWeek = selectedDayOfWeek
            }
        }

        etEditTime.setOnClickListener {
            showTimePickerDialog(etEditTime)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Schedule")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnUpdateSchedule.setOnClickListener {
            val updatedSubject = etEditSubject.text.toString()
            val updatedDate = etEditDate.text.toString()
            val updatedTime = etEditTime.text.toString()
            val updatedRoom = etEditRoom.text.toString()

            // Use the existing dayOfWeek if not changed
            val updatedDayOfWeek = if (dayOfWeek == -1) schedule.dayOfWeek else getDayOfWeekString(dayOfWeek)

            if (updatedSubject.isEmpty() || updatedDate.isEmpty() || updatedTime.isEmpty() || updatedRoom.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val updatedSchedule = schedule.copy(
                    subject = updatedSubject,
                    dayOfWeek = updatedDayOfWeek,
                    date = updatedDate,
                    time = updatedTime,
                    room = updatedRoom
                )

                updateScheduleOnServer(updatedSchedule) { success ->
                    if (success) {
                        dialog.dismiss()
                        scheduleList[scheduleList.indexOfFirst { it.scheduleId == schedule.scheduleId }] = updatedSchedule
                        scheduleList.sortWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        scheduleAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Schedule updated successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                PERMISSIONS_REQUEST_WRITE_CALENDAR
            )
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_WRITE_CALENDAR = 100
    }

    private fun updateScheduleOnServer(schedule: Schedule, onComplete: (Boolean) -> Unit) {
        val apiService = RetrofitInstance.api
        val call = apiService.updateSchedule(schedule)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Schedule updated successfully")
                    Toast.makeText(requireContext(), "Schedule updated successfully", Toast.LENGTH_SHORT).show()

                    val index = scheduleList.indexOfFirst { it.scheduleId == schedule.scheduleId }
                    if (index >= 0) {
                        scheduleList[index] = schedule
                        scheduleList.sortWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        scheduleAdapter.notifyDataSetChanged()
                    }
                    onComplete(true)
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Failed to update schedule: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to update schedule", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Error updating schedule", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        })
    }

    private fun sendUserDataToServer(schedule: Schedule) {
        val apiService = RetrofitInstance.api
        val call = apiService.addCalendar(schedule)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "User data sent successfully")
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Failed to send user data: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to send user data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Error sending user data", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteSchedule(schedule: Schedule) {
        val apiService = RetrofitInstance.api

        val deleteParams = mapOf(
            "scheduleId" to schedule.scheduleId,
            "userId" to uid.orEmpty()
        )

        val call = apiService.deleteSchedule(deleteParams)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Schedule deleted successfully")
                    Toast.makeText(requireContext(), "Schedule deleted successfully", Toast.LENGTH_SHORT).show()

                    // Remove schedule from the list and update the recycler view
                    scheduleList.remove(schedule)
                    val sortedList = scheduleList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                    scheduleAdapter.updateScheduleList(sortedList.toMutableList())
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Failed to delete schedule: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to delete schedule", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Error deleting schedule", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getAllSchedules() {
        val apiService = RetrofitInstance.api
        val call = apiService.getAllSchedules(uid ?: "")
        call.enqueue(object : Callback<List<Schedule>> {
            override fun onResponse(call: Call<List<Schedule>>, response: Response<List<Schedule>>) {
                if (response.isSuccessful) {
                    val schedules = response.body()
                    schedules?.let {
                        scheduleList.clear()
                        scheduleList.addAll(it)
                        val sortedList = scheduleList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        scheduleAdapter.updateScheduleList(sortedList.toMutableList())
                    }
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Failed to get schedules: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to get schedules", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Schedule>>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.ScheduleFragment", "Error getting schedules", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}