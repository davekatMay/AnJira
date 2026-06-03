package com.anjira.taskplanner.data.repository

import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.ApiService
import com.anjira.taskplanner.data.remote.dto.RefreshRequest
import com.anjira.taskplanner.data.remote.dto.RegisterRequest
import com.anjira.taskplanner.domain.model.User
import com.anjira.taskplanner.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override suspend fun register(email: String, password: String): User {
        return withContext(Dispatchers.IO) {
            val request = RegisterRequest(email, password)
            val response = apiService.register(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Registration failed: ${response.errorBody()?.string()}")
            }
            val authResponse = response.body() ?: throw Exception("Empty response body")
            dataStoreManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
            dataStoreManager.saveUserInfo(authResponse.userId, authResponse.username)
            User(authResponse.userId, authResponse.username, email)
        }
    }

    override suspend fun login(email: String, password: String): User {
        return withContext(Dispatchers.IO) {
            val request = RegisterRequest(email, password)
            val response = apiService.login(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Login failed: ${response.errorBody()?.string()}")
            }
            val authResponse = response.body() ?: throw Exception("Empty response body")
            dataStoreManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
            dataStoreManager.saveUserInfo(authResponse.userId, authResponse.username)
            User(authResponse.userId, authResponse.username, email)
        }
    }

    override suspend fun refreshToken(): Boolean {
        return withContext(Dispatchers.IO) {
            val refreshToken = dataStoreManager.getRefreshToken() ?: return@withContext false
            val request = RefreshRequest(refreshToken)
            val response = apiService.refreshToken(request).execute()
            if (response.isSuccessful) {
                val authResponse = response.body() ?: return@withContext false
                dataStoreManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                dataStoreManager.saveUserInfo(authResponse.userId, authResponse.username)
                true
            } else {
                dataStoreManager.clearAll()
                false
            }
        }
    }

    override suspend fun logout() {
        dataStoreManager.clearAll()
    }
}
