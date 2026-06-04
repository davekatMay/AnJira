package com.anjira.taskplanner.data.remote

import com.anjira.taskplanner.data.local.DataStoreManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    private var dataStoreManager: DataStoreManager? = null
    private var cachedAccessToken: String? = null

    lateinit var apiService: ApiService
        private set

    @Synchronized
    fun init(dataStoreManager: DataStoreManager) {
        this.dataStoreManager = dataStoreManager
        cachedAccessToken = null
        recreateApiService()
    }

    fun updateToken(token: String?) {
        cachedAccessToken = token
    }

    private fun recreateApiService() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            var request = chain.request()
            val token = cachedAccessToken ?: runBlocking { dataStoreManager?.getAccessToken() }
            if (token != null) {
                request = request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
