package com.anjira.taskplanner.data.remote.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Int,
    val username: String,
    val email: String
)
