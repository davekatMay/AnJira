package com.anjira.taskplanner.data.remote.dto

data class TaskCreateRequest(
    val title: String,
    val description: String? = null,
    val assignedTo: Int? = null
)