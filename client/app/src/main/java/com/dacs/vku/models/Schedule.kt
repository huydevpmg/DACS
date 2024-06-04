package com.dacs.vku.models

import java.io.Serializable

data class Schedule(
    val scheduleId: String,
    val userId: String?,
    val dayOfWeek: String,
    val time: String,
    val room: String,
    val subject: String
) : Serializable
