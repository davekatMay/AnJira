package com.anjira.taskplanner.domain.model

data class Group(
    val id: Int,
    val name: String,
    val description: String?,
    val createdBy: Int,
    val createdAt: String,
    val updatedAt: String
)