package com.dacs.vku.models

import java.io.Serializable

data class Schedule(
    val scheduleId: String,
    val userId: String?,
    val dayOfWeek: String,
    val date: String,
    val time: String,
    val room: String,
    val subject: String,
    var eventId: Long? = null // Add eventId with a default value of null

) : Serializable

