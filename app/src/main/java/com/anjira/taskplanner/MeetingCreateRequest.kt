package com.anjira.taskplanner.data.remote.dto

data class MeetingCreateRequest(
    val title: String,
    val description: String? = null,
    val dateTime: String, // ISO format
    val location: String? = null
)