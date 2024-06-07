package com.dacs.vku.ui.fragments.Authentication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.vku.R
import com.dacs.vku.R.id.etEditSubject
import com.dacs.vku.adapters.SeminarAdapter
import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.api.UserData
import com.dacs.vku.databinding.FragmentSeminarBinding
import com.dacs.vku.models.Seminar
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class SeminarFragment : Fragment(R.layout.fragment_seminar) {
    private lateinit var binding: FragmentSeminarBinding
    private lateinit var seminarAdapter: SeminarAdapter
    private val seminarList = mutableListOf<Seminar>()
    private var userData: UserData? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userData = it.getSerializable("userData") as? UserData
            uid = userData?.userId
        }
        Log.e("haha", userData.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSeminarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()

        getAllSeminars()

        binding.fabAddSeminar.setOnClickListener {
            addSeminar()
        }

        // Set up RecyclerView
        seminarAdapter = SeminarAdapter(seminarList,
            onEditClick = {
                    seminar -> editSeminar(seminar)
            },
            onDeleteClick = { seminar -> deleteSeminar(seminar) }
        )

        binding.rvSeminars.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = seminarAdapter
        }
    }

    private fun addSeminar() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_seminar, null)
        val etDate = dialogView.findViewById<EditText>(R.id.seDate)
        val etTime = dialogView.findViewById<EditText>(R.id.seTime)
        val etDayOfWeek = dialogView.findViewById<EditText>(R.id.seDayOfWeek)
        val etSubject = dialogView.findViewById<EditText>(R.id.seEditSubject)
        val etRoom = dialogView.findViewById<EditText>(R.id.seEditRoom)
        val btnAddSeminar = dialogView.findViewById<Button>(R.id.btnAddSeminar)

        var dayOfWeek: Int = -1

        etDate.setOnClickListener {
            showDatePickerDialog(etDate, etDayOfWeek) { selectedDayOfWeek ->
                dayOfWeek = selectedDayOfWeek
            }
        }

        etTime.setOnClickListener {
            showTimePickerDialog(etTime)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Seminar")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnAddSeminar.setOnClickListener {
            val subject = etSubject.text.toString()
            val room = etRoom.text.toString()
            val date = etDate.text.toString()
            val time = etTime.text.toString()

            if (subject.isEmpty() || date.isEmpty() || time.isEmpty() || room.isEmpty() || dayOfWeek == -1) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val seminarId = UUID.randomUUID().toString()
                val userId = uid
                val dayOfWeekString = getDayOfWeekString(dayOfWeek)
                val seminarData = Seminar(seminarId, userId, dayOfWeekString, date, time, room, subject)

                seminarList.add(seminarData)
                val sortedList = seminarList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                seminarAdapter.updateSeminarList(sortedList.toMutableList())

                sendUserDataToServer(seminarData)
                insertEventIntoCalendar(seminarData)
                Toast.makeText(requireContext(), "Seminar added successfully", Toast.LENGTH_SHORT).show()
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

            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)

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

    @SuppressLint("InlinedApi")
    private fun insertEventIntoCalendar(seminar: Seminar) {
        val cal = Calendar.getInstance()
        val dateParts = seminar.date.split("/")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1
        val year = dateParts[2].toInt()
        val timeParts = seminar.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        cal.set(year, month, day, hour, minute)

        val startMillis: Long = cal.timeInMillis
        val endMillis: Long = cal.apply { add(Calendar.HOUR, 1) }.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            val description = "Subject: ${seminar.subject} Room: ${seminar.room}"
            putExtra(CalendarContract.Events.TITLE, description)
            Log.e("VKUUU", description)
            putExtra(CalendarContract.Events.DESCRIPTION, "Subject: $description")
            putExtra(CalendarContract.Events.EVENT_LOCATION, seminar.room)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.RRULE, "FREQ=ONCE")
        }
        startActivity(intent)
    }

    private fun editSeminar(seminar: Seminar) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_seminar, null)
        val etEditSubject = dialogView.findViewById<EditText>(etEditSubject)
        val etEditDate = dialogView.findViewById<EditText>(R.id.etEditDate)
        val etEditTime = dialogView.findViewById<EditText>(R.id.etEditTime)
        val etEditRoom = dialogView.findViewById<EditText>(R.id.etEditRoom)
        val etDayOfWeek = dialogView.findViewById<EditText>(R.id.etDayOfWeek)
        val btnUpdateSeminar = dialogView.findViewById<Button>(R.id.btnUpdateSeminar)

        etEditSubject.setText(seminar.subject)
        etEditDate.setText(seminar.date)
        etEditTime.setText(seminar.time)
        etEditRoom.setText(seminar.room)
        etDayOfWeek.setText(seminar.dayOfWeek)

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
            .setTitle("Edit Seminar")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnUpdateSeminar.setOnClickListener {
            val updatedSubject = etEditSubject.text.toString()
            val updatedDate = etEditDate.text.toString()
            val updatedTime = etEditTime.text.toString()
            val updatedRoom = etEditRoom.text.toString()

            val updatedDayOfWeek = if (dayOfWeek == -1) seminar.dayOfWeek else getDayOfWeekString(dayOfWeek)

            if (updatedSubject.isEmpty() || updatedDate.isEmpty() || updatedTime.isEmpty() || updatedRoom.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val updatedSeminar = seminar.copy(
                    subject = updatedSubject,
                    dayOfWeek = updatedDayOfWeek,
                    date = updatedDate,
                    time = updatedTime,
                    room = updatedRoom
                )

                updateSeminarOnServer(updatedSeminar) { success ->
                    if (success) {
                        dialog.dismiss()
                        seminarList[seminarList.indexOfFirst { it.seminarId == seminar.seminarId }] = updatedSeminar
                        seminarList.sortWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        seminarAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Seminar updated successfully", Toast.LENGTH_SHORT).show()
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

    private fun updateSeminarOnServer(seminar: Seminar, onComplete: (Boolean) -> Unit) {
        val apiService = RetrofitInstance.api
        val call = apiService.updateSeminar(seminar)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("VKUU", "Seminar updated successfully")
                    Toast.makeText(requireContext(), "Seminar updated successfully", Toast.LENGTH_SHORT).show()
                    val index = seminarList.indexOfFirst { it.seminarId == seminar.seminarId }
                    if (index >= 0) {
                        seminarList[index] = seminar
                        seminarList.sortWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        seminarAdapter.notifyDataSetChanged()
                    }
                    onComplete(true)
                } else {
                    Log.e("VKUU", "Failed to update seminar: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to update seminar", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("Vkuu", "Error updating seminar", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        })
    }

    private fun sendUserDataToServer(seminar: Seminar) {
        val apiService = RetrofitInstance.api
        val call = apiService.addSeminar(seminar)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "User data sent successfully")
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Failed to send user data: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to send user data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Error sending user data", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteSeminar(seminar: Seminar) {
        val apiService = RetrofitInstance.api
        val deleteParams = mapOf(
            "seminarId" to seminar.seminarId,
            "userId" to uid.orEmpty()
        )

        val call = apiService.deleteSeminar(deleteParams)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Seminar deleted successfully")
                    Toast.makeText(requireContext(), "Seminar deleted successfully", Toast.LENGTH_SHORT).show()
                    seminarList.remove(seminar)
                    val sortedList = seminarList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                    seminarAdapter.updateSeminarList(sortedList.toMutableList())
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Failed to delete seminar: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to delete seminar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Error deleting seminar", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getAllSeminars() {
        val apiService = RetrofitInstance.api
        val call = apiService.getAllSeminar(uid ?: "")
        call.enqueue(object : Callback<List<Seminar>> {
            override fun onResponse(call: Call<List<Seminar>>, response: Response<List<Seminar>>) {
                if (response.isSuccessful) {
                    val seminars = response.body()
                    seminars?.let {
                        seminarList.clear()
                        seminarList.addAll(it)
                        val sortedList = seminarList.sortedWith(compareBy({ parseDate(it.date) }, { parseTime(it.time) }))
                        seminarAdapter.updateSeminarList(sortedList.toMutableList())
                    }
                } else {
                    Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Failed to get seminars: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to get seminars", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Seminar>>, t: Throwable) {
                Log.e("com.dacs.vku.ui.fragments.Authentication.SeminarFragment", "Error getting seminars", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}