package com.dacs.vku.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.models.Notification
import okhttp3.internal.http2.Http2Connection

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationTitle: TextView = itemView.findViewById(R.id.notificationTitle)
        val notificationDateTime: TextView = itemView.findViewById(R.id.notificationDateTime)
    }

    private val differCallBack = object : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.href == newItem.href
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallBack)

    private var onItemClickListener: ((Notification) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_notifiations, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = differ.currentList[position]

        holder.notificationTitle.text = notification.title
        holder.notificationDateTime.text = notification.spanText
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(notification)
        }
    }
    fun setOnItemClickListener(listener: (Notification) -> Unit) {
        onItemClickListener = listener
    }
}