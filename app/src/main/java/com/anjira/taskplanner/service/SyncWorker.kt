package com.anjira.taskplanner.service

import android.content.Context
import androidx.work.*
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.local.room.AppDatabase
import com.anjira.taskplanner.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dataStoreManager = DataStoreManager(applicationContext)
            RetrofitInstance.init { dataStoreManager.getAccessToken() }
            val db = AppDatabase.getInstance(applicationContext)

            val lastSync = dataStoreManager.getLastSyncTime()
            val response = RetrofitInstance.apiService.syncData(since = lastSync).execute()
            if (!response.isSuccessful) return@withContext Result.retry()

            val sync = response.body() ?: return@withContext Result.retry()

            db.replaceAll(sync)

            dataStoreManager.setLastSyncTime(sync.serverTime)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "anjira_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
