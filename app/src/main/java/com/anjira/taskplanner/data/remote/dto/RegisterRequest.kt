package com.anjira.taskplanner.data.remote.dto

data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)
