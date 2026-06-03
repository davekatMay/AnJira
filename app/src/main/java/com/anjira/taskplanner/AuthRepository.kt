package com.anjira.taskplanner.domain.repository

import com.anjira.taskplanner.domain.model.User

interface AuthRepository {
    suspend fun register(email: String, password: String): User
    suspend fun login(email: String, password: String): User
    suspend fun refreshToken(): Boolean
    suspend fun logout()
}
