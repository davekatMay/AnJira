package com.anjira.taskplanner.data.remote.dto

data class MeetingCreateRequest(
    val title: String,
    val description: String? = null,
    val dateTime: String,
    val endDateTime: String? = null,
    val location: String? = null
)
