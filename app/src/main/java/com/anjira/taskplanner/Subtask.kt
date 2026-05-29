package com.anjira.taskplanner.domain.model

data class Subtask(
    val id: Int,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)