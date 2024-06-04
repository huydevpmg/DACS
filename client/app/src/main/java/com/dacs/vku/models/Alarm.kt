package com.dacs.vku.models

data class Alarm(
    val title: String,
    val dateTime: Long,
    var isEnabled: Boolean = true // Mặc định là true

)
