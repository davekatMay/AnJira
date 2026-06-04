package com.anjira.taskplanner.data.remote.dto

data class PlaylistResponse(
    val id: Int,
    val groupId: Int,
    val name: String,
    val type: String,
    val meetingId: Int?,
    val createdBy: Int,
    val createdByUsername: String,
    val createdAt: String,
    val updatedAt: String
)
