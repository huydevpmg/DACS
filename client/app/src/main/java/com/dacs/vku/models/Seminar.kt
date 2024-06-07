package com.dacs.vku.models

import java.io.Serializable

data class Seminar(
    val seminarId: String,
    val userId: String?,
    val dayOfWeek: String,
    val date: String,
    val time: String,
    val room: String,
    val subject: String
) : Serializable