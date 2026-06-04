package com.anjira.taskplanner.data.remote.dto

data class MeetingResponse(
    val id: Int,
    val groupId: Int,
    val title: String,
    val description: String?,
    val dateTime: String,
    val endDateTime: String?,
    val location: String?,
    val createdBy: Int,
    val createdAt: String,
    val updatedAt: String,
    val myRsvp: String? = null
)
