package com.anjira.taskplanner.data.remote.dto

data class NotificationResponse(
    val id: Int,
    val type: String,
    val referenceId: Int,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
