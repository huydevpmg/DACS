package com.dacs.vku.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "notificationDaoTao"
)
data class Notification(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    val href: String,
    val spanText: String,
    val title: String
    ):Serializable
