package com.anjira.taskplanner.data.remote.dto

data class AuthResponse(
    val token: String,
    val userId: Int,
    val username: String
)