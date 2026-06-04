package com.anjira.taskplanner.data.remote.dto

data class SubtaskResponse(
    val id: Int,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)