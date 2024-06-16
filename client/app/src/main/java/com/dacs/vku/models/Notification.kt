package com.dacs.vku.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "Notification"
)
data class Notification(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    val href: String,
    val spanText: String,
    val title: String,
    var note: String? = null // Add this field
):Serializable
