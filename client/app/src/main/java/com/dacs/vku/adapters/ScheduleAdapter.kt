package com.dacs.vku.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.models.Schedule

class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.tvDayOfWeek.text = schedule.dayOfWeek
        holder.tvTime.text = schedule.time
        holder.tvRoom.text = schedule.room
        holder.tvSubject.text = schedule.subject

        holder.editButton.setOnClickListener { onEditClick(schedule) }
        holder.deleteButton.setOnClickListener { onDeleteClick(schedule) }
    }

    override fun getItemCount(): Int = schedules.size

    fun updateScheduleList(newSchedules: MutableList<Schedule>) {
        schedules.clear()
        schedules.addAll(newSchedules)
        notifyDataSetChanged()
    }

    fun removeSchedule(schedule: Schedule) {
        val position = schedules.indexOf(schedule)
        if (position >= 0) {
            schedules.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}