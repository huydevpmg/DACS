package com.dacs.vku.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.models.Schedule

class ScheduleAdapter(
    private val scheduleList: MutableList<Schedule>,
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(schedule: Schedule) {
            itemView.findViewById<TextView>(R.id.tvDayOfWeek).text = schedule.dayOfWeek
            itemView.findViewById<TextView>(R.id.tvSubject).text = schedule.subject
            itemView.findViewById<TextView>(R.id.tvTime).text = schedule.time
            itemView.findViewById<TextView>(R.id.tvRoom).text = schedule.room

            itemView.findViewById<Button>(R.id.editButton).setOnClickListener {
                onEditClick(schedule)
            }

            itemView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                onDeleteClick(schedule)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }

    override fun getItemCount(): Int = scheduleList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateScheduleList(newScheduleList: MutableList<Schedule>) {
        scheduleList.clear()
        scheduleList.addAll(newScheduleList)
        notifyDataSetChanged()
    }

    fun removeSchedule(schedule: Schedule) {
        val position = scheduleList.indexOf(schedule)
        if (position != -1) {
            scheduleList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}
