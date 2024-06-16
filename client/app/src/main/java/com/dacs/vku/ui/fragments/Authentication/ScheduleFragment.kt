package com.dacs.vku.ui.fragments.Authentication

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.vku.R
import com.dacs.vku.adapters.ScheduleAdapter
import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.models.UserData
import com.dacs.vku.databinding.FragmentScheduleBinding
import com.dacs.vku.models.Schedule
import com.dacs.vku.util.Constants
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

        binding.btnPreviewPdf.setOnClickListener {
            val pdfFile = createPdfFile()
            if (pdfFile != null) {
                showPdfPreviewDialog(pdfFile)
            } else {
                Toast.makeText(requireContext(), "Failed to create PDF", Toast.LENGTH_SHORT).show()
            }
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
            val description = "Subject: ${schedule.subject} Room: ${schedule.room}"
            putExtra(CalendarContract.Events.TITLE, description)
            Log.e("VKUUU", description)
            putExtra(CalendarContract.Events.DESCRIPTION, "Subject: $description")
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
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(),
                Constants.PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun renderPdfToImageView(pdfFile: File, imageView: ImageView, scaleFactor: Float = 1.5f) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(0)

            val bitmap = Bitmap.createBitmap((page.width * scaleFactor ).toInt(), (page.height * scaleFactor).toInt(), Bitmap.Config.ARGB_8888)
            val matrix = Matrix()
            matrix.setScale(scaleFactor, scaleFactor)
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            imageView.setImageBitmap(bitmap)

            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to render PDF", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showPdfPreviewDialog(pdfFile: File) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pdf_preview, null)
        val ivPdfPreview = dialogView.findViewById<ImageView>(R.id.ivPdfPreview)
        val btnSavePdf = dialogView.findViewById<Button>(R.id.btnSavePdf)

        // Set ImageView to be as large as possible
        val layoutParams = ivPdfPreview.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        ivPdfPreview.layoutParams = layoutParams

        renderPdfToImageView(pdfFile, ivPdfPreview)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("PDF Preview")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        val window = dialog.window
        if (window != null) {
            // Make the dialog fill the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = layoutParams
        }

        btnSavePdf.setOnClickListener {
            savePdfFile(pdfFile)
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun savePdfFile(pdfFile: File) {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        val destinationFile = File(directoryPath, "Subject.pdf")

        try {
            pdfFile.copyTo(destinationFile, overwrite = true)
            Toast.makeText(requireContext(), "PDF saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPdfFile(): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        paint.textSize = 12f
        paint.color = Color.BLACK
        titlePaint.textSize = 20f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        titlePaint.color = Color.BLUE

        val marginLeft = 50f
        var yPosition = 60

        canvas.drawText("Schedule Preview", marginLeft, yPosition.toFloat(), titlePaint)
        yPosition += 40

        scheduleList.forEach { seminar ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Subject: ${seminar.subject}", marginLeft, yPosition.toFloat(), paint)
            yPosition += 20

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Date: ${seminar.date}", marginLeft, yPosition.toFloat(), paint)
            yPosition += 20

            canvas.drawText("Time: ${seminar.time}", marginLeft, yPosition.toFloat(), paint)
            yPosition += 20

            canvas.drawText("Room: ${seminar.room}", marginLeft, yPosition.toFloat(), paint)
            yPosition += 30

            // Adding a separator line for better readability
            val linePaint = Paint()
            linePaint.color = Color.GRAY
            linePaint.strokeWidth = 1f
            canvas.drawLine(marginLeft, yPosition.toFloat(), pageInfo.pageWidth - marginLeft, yPosition.toFloat(), linePaint)
            yPosition += 20
        }

        pdfDocument.finishPage(page)

        val directoryPath = requireContext().cacheDir.toString()
        val file = File(directoryPath, "SeminarsPreview.pdf")

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
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