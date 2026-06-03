package com.anjira.taskplanner.data.remote.dto

data class TaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val deadline: String? = null,
    val status: String? = null,
    val assignedTo: Int? = null
)
