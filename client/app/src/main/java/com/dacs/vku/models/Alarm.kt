package com.dacs.vku.models

data class Alarm(
    val id: String, // Add an ID field for easier management
    val title: String,
    val dateTime: Long,
    var isEnabled: Boolean = true
)

