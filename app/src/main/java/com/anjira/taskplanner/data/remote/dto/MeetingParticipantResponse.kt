package com.anjira.taskplanner.data.remote.dto

data class MeetingParticipantResponse(
    val userId: Int,
    val username: String,
    val email: String,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
