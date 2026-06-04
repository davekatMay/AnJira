package com.anjira.taskplanner.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anjira.taskplanner.MainActivity
import com.anjira.taskplanner.R
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.remote.dto.FcmRegisterRequest
import kotlinx.coroutines.runBlocking

class FirebaseMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "anjira_notifications"
        const val NOTIFICATION_ID_BASE = 1000
        private var notifIdCounter = NOTIFICATION_ID_BASE

        fun getNextNotificationId(): Int = notifIdCounter++
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        val dataStoreManager = DataStoreManager(applicationContext)
        RetrofitInstance.init(dataStoreManager)
        runBlocking {
            val accessToken = dataStoreManager.getAccessToken()
            if (accessToken != null) {
                try {
                    RetrofitInstance.apiService.registerFcmToken(FcmRegisterRequest(token)).execute()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val title = message.notification?.title ?: message.data["title"] ?: "AnJira"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"]
        val groupId = message.data["groupId"]?.toIntOrNull()
        val taskId = message.data["taskId"]?.toIntOrNull()
        val meetingId = message.data["meetingId"]?.toIntOrNull()
        val announcementId = message.data["announcementId"]?.toIntOrNull()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            type?.let { putExtra("nav_type", it) }
            groupId?.let { putExtra("nav_groupId", it) }
            taskId?.let { putExtra("nav_taskId", it) }
            meetingId?.let { putExtra("nav_meetingId", it) }
            announcementId?.let { putExtra("nav_announcementId", it) }
        }

        val notifId = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(notifId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AnJira уведомления",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Уведомления о задачах, встречах и объявлениях" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
