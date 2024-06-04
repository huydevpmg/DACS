package com.dacs.vku.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.models.Alarm

class AlarmAdapter(private val alarms: MutableList<Alarm>) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val buttonToggle: Button = itemView.findViewById(R.id.buttonToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = alarms[position]
        holder.textViewTitle.text = notification.title

        // Set button text based on notification state
        holder.buttonToggle.text = if (notification.isEnabled) "On" else "Off"

        // Toggle notification state when button is clicked
        holder.buttonToggle.setOnClickListener {
            notification.isEnabled = !notification.isEnabled
            notifyItemChanged(position) // Update button text
        }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }

    // Add function to add item to RecyclerView
    fun addItem(alarm: Alarm) {
        alarms.add(alarm)
        notifyItemInserted(alarms.size - 1)
    }

    // Add function to remove item from RecyclerView
    fun removeItem(position: Int) {
        alarms.removeAt(position)
        notifyItemRemoved(position)
    }
}