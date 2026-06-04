package com.anjira.taskplanner.domain.model

data class Meeting(
    val id: Int,
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

data class MeetingParticipant(
    val userId: Int,
    val username: String,
    val email: String,
    val status: String // going, maybe, declined, pending
)
