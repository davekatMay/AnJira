package com.anjira.taskplanner.domain.model

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val status: String,
    val createdBy: Int,
    val assignedTo: Int?,
    val createdAt: String,
    val updatedAt: String
)