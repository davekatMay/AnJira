package com.anjira.taskplanner.data.remote.dto

data class TaskResponse(
    val id: Int,
    val groupId: Int,
    val title: String,
    val description: String?,
    val deadline: String?,
    val status: String,
    val createdBy: Int,
    val assignedTo: Int?,
    val createdAt: String,
    val updatedAt: String
)
