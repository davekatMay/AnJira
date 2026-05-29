package com.anjira.taskplanner.data.remote.dto

data class MeetingUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val dateTime: String? = null,
    val location: String? = null
)