package com.dacs.vku.ui.fragments.Authentication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.CalendarContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.vku.R
import com.dacs.vku.R.id.etEditSubject
import com.dacs.vku.adapters.SeminarAdapter
import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.models.UserData
import com.dacs.vku.databinding.FragmentSeminarBinding
import com.dacs.vku.models.Seminar
import com.dacs.vku.util.Constants.Companion.PERMISSIONS_REQUEST_CODE
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

@Suppress("DEPRECATION", "NAME_SHADOWING")
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
    ): View {
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

        binding.btnPreviewPdf.setOnClickListener {
            val pdfFile = createPdfFile()
            if (pdfFile != null) {
                showPdfPreviewDialog(pdfFile)
            } else {
                Toast.makeText(requireContext(), "Failed to create PDF", Toast.LENGTH_SHORT).show()
            }
        }
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
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
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

        val dialog = AlertDialog.Builder(requireContext())
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
        val destinationFile = File(directoryPath, "seminarsList.pdf")

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

        canvas.drawText("Seminars Preview", marginLeft, yPosition.toFloat(), titlePaint)
        yPosition += 40

        seminarList.forEach { seminar ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Title: ${seminar.subject}", marginLeft, yPosition.toFloat(), paint)
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

    @SuppressLint("NotifyDataSetChanged")
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



    private fun updateSeminarOnServer(seminar: Seminar, onComplete: (Boolean) -> Unit) {
        val apiService = RetrofitInstance.api
        val call = apiService.updateSeminar(seminar)
        call.enqueue(object : Callback<ResponseBody> {
            @SuppressLint("NotifyDataSetChanged")
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