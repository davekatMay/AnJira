package com.anjira.taskplanner.data.remote.dto

data class GroupResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val createdBy: Int,
    val createdAt: String,
    val updatedAt: String
)