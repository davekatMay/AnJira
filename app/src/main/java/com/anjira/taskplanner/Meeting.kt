package com.anjira.taskplanner.domain.model

data class Meeting(
    val id: Int,
    val title: String,
    val description: String?,
    val dateTime: String,
    val location: String?,
    val createdBy: Int,
    val createdAt: String,
    val updatedAt: String
)
