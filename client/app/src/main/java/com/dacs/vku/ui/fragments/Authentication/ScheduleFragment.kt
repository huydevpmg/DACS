package com.dacs.vku.ui.fragments.Authentication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        getAllSchedules()

        val daysOfWeek = resources.getStringArray(R.array.days_of_week)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDaysOfWeek.adapter = spinnerAdapter

        binding.btnAddSchedule.setOnClickListener {
            addSchedule()
        }

        // Set up RecyclerView
        scheduleAdapter = ScheduleAdapter(scheduleList,
            onEditClick = { schedule -> editSchedule(schedule) },
            onDeleteClick = { schedule -> deleteSchedule(schedule) }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun getDayOfWeekIndex(dayOfWeek: String): Int {
        return when (dayOfWeek) {
            "Thứ 2" -> 1
            "Thứ 3" -> 2
            "Thứ 4" -> 3
            "Thứ 5" -> 4
            "Thứ 6" -> 5
            "Thứ 7" -> 6
            "Chủ Nhật" -> 7
            else -> 0
        }
    }

    private fun addSchedule() {
        val userId = uid
        val subject = binding.etSubject.text.toString()
        val dayOfWeek = binding.spinnerDaysOfWeek.selectedItem.toString()
        val time = binding.etTime.text.toString()
        val room = binding.etRoom.text.toString()

        // Validate input
        if (subject.isEmpty() || time.isEmpty() || room.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a unique schedule ID
        val scheduleId = UUID.randomUUID().toString()

        // Prepare data to send to server
        val scheduleData = Schedule(scheduleId, userId, dayOfWeek, time, room, subject)

        // Add schedule to the list
        scheduleList.add(scheduleData)

        // Sort scheduleList by dayOfWeek
        val sortedList = scheduleList.sortedBy { getDayOfWeekIndex(it.dayOfWeek) }
        scheduleAdapter.updateScheduleList(sortedList.toMutableList())

        sendUserDataToServer(scheduleData)
    }

    private fun editSchedule(schedule: Schedule) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_schedule, null)
        val etEditSubject = dialogView.findViewById<EditText>(R.id.etEditSubject)
        val spinnerEditDaysOfWeek = dialogView.findViewById<Spinner>(R.id.spinnerEditDaysOfWeek)
        val etEditTime = dialogView.findViewById<EditText>(R.id.etEditTime)
        val etEditRoom = dialogView.findViewById<EditText>(R.id.etEditRoom)
        val btnUpdateSchedule = dialogView.findViewById<Button>(R.id.btnUpdateSchedule)

        etEditSubject.setText(schedule.subject)
        etEditTime.setText(schedule.time)
        etEditRoom.setText(schedule.room)

        val daysOfWeek = resources.getStringArray(R.array.days_of_week)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEditDaysOfWeek.adapter = spinnerAdapter

        val dayOfWeekIndex = daysOfWeek.indexOf(schedule.dayOfWeek)
        if (dayOfWeekIndex >= 0) {
            spinnerEditDaysOfWeek.setSelection(dayOfWeekIndex)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Schedule")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnUpdateSchedule.setOnClickListener {
            val updatedSubject = etEditSubject.text.toString()
            val updatedDayOfWeek = spinnerEditDaysOfWeek.selectedItem.toString()
            val updatedTime = etEditTime.text.toString()
            val updatedRoom = etEditRoom.text.toString()

            if (updatedSubject.isEmpty() || updatedTime.isEmpty() || updatedRoom.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val updatedSchedule = schedule.copy(
                    subject = updatedSubject,
                    dayOfWeek = updatedDayOfWeek,
                    time = updatedTime,
                    room = updatedRoom
                )

                updateScheduleOnServer(updatedSchedule) { success ->
                    if (success) {
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun updateScheduleOnServer(schedule: Schedule, onComplete: (Boolean) -> Unit) {
        val apiService = RetrofitInstance.api
        val call = apiService.updateSchedule(schedule)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("ScheduleFragment", "Schedule updated successfully")
                    Toast.makeText(requireContext(), "Schedule updated successfully", Toast.LENGTH_SHORT).show()

                    // Update local list
                    val index = scheduleList.indexOfFirst { it.scheduleId == schedule.scheduleId }
                    if (index >= 0) {
                        scheduleList[index] = schedule
                        // Sort the list by day of week
                        scheduleList.sortBy { getDayOfWeekIndex(it.dayOfWeek) }
                        scheduleAdapter.notifyDataSetChanged()
                    }
                    onComplete(true)
                } else {
                    Log.e("ScheduleFragment", "Failed to update schedule: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to update schedule", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("ScheduleFragment", "Error updating schedule", t)
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
                    Log.d("ScheduleFragment", "User data sent successfully")
                } else {
                    Log.e("ScheduleFragment", "Failed to send user data: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to send user data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("ScheduleFragment", "Error sending user data", t)
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
                    Log.d("ScheduleFragment", "Schedule deleted successfully")
                    Toast.makeText(requireContext(), "Schedule deleted successfully", Toast.LENGTH_SHORT).show()
                    scheduleList.remove(schedule)
                    scheduleAdapter.removeSchedule(schedule) // Remove from RecyclerView
                    val sortedList = scheduleList.sortedBy { getDayOfWeekIndex(it.dayOfWeek) }
                    scheduleAdapter.updateScheduleList(sortedList.toMutableList())
                } else {
                    Log.e("ScheduleFragment", "Failed to delete schedule: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to delete schedule", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("ScheduleFragment", "Error deleting schedule", t)
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
                        scheduleAdapter.notifyDataSetChanged()
                    }
                } else {
                    Log.e("ScheduleFragment", "Failed to get schedules: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to get schedules", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Schedule>>, t: Throwable) {
                Log.e("ScheduleFragment", "Error getting schedules", t)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
