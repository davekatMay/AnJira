package com.anjira.taskplanner.data.remote.dto

data class UserProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val avatar: String?,
    val description: String?,
    val contacts: String?,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
