package com.dacs.vku.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.models.Seminar

class SeminarAdapter(
    private val seminars: MutableList<Seminar>,
    private val onEditClick: (Seminar) -> Unit,
    private val onDeleteClick: (Seminar) -> Unit
) : RecyclerView.Adapter<SeminarAdapter.SeminarViewHolder>() {

    inner class SeminarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeminarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seminar, parent, false)
        return SeminarViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeminarViewHolder, position: Int) {
        val seminar = seminars[position]
        holder.tvDayOfWeek.text = seminar.dayOfWeek
        holder.tvTime.text = seminar.time
        holder.tvRoom.text = seminar.room
        holder.tvSubject.text = seminar.subject

        holder.editButton.setOnClickListener { onEditClick(seminar) }
        holder.deleteButton.setOnClickListener { onDeleteClick(seminar) }
    }

    override fun getItemCount(): Int = seminars.size

    fun updateSeminarList(newSeminar: MutableList<Seminar>) {
        seminars.clear()
        seminars.addAll(newSeminar)
        notifyDataSetChanged()
    }

    fun removeSeminar(seminar: Seminar) {
        val position = seminars.indexOf(seminar)
        if (position >= 0) {
            seminars.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}