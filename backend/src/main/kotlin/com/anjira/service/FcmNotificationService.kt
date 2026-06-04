package com.anjira.service

import com.anjira.db.*
import com.google.auth.oauth2.GoogleCredentials
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

object FcmNotificationService {
    private val logger = LoggerFactory.getLogger("FcmNotificationService")
    private var initialized = false

    fun init() {
        val firebaseConfigJson = System.getenv("FIREBASE_CONFIG_JSON")
        if (firebaseConfigJson.isNullOrBlank()) {
            logger.warn("FIREBASE_CONFIG_JSON not set — push notifications disabled")
            return
        }
        try {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(firebaseConfigJson.toByteArray())))
                .build()
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            initialized = true
            logger.info("Firebase initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase: ${e.message}")
        }
    }

    fun sendPushNotification(userId: Int, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (!initialized) return
        val tokens = transaction {
            FcmTokenTable.select { FcmTokenTable.userId eq userId }
                .map { it[FcmTokenTable.token] }
                .toList()
        }
        if (tokens.isEmpty()) return

        val messageBuilder = MulticastMessage.builder()
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .addAllTokens(tokens)

        try {
            val response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build())
            if (response.failureCount > 0) {
                response.responses.forEachIndexed { index, resp ->
                    if (!resp.isSuccessful) {
                        logger.warn("FCM send failed for token $index: ${resp.exception?.message}")
                        resp.exception?.messagingErrorCode?.let { code ->
                            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                                transaction {
                                    FcmTokenTable.deleteWhere { FcmTokenTable.token eq tokens[index] }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: FirebaseMessagingException) {
            logger.error("FCM send error: ${e.message}")
        }
    }

    fun registerToken(userId: Int, token: String, deviceName: String?) {
        val now = LocalDateTime.now().toString()
        transaction {
            val existing = FcmTokenTable.select {
                (FcmTokenTable.userId eq userId) and (FcmTokenTable.token eq token)
            }.firstOrNull()
            if (existing == null) {
                FcmTokenTable.insert {
                    it[FcmTokenTable.userId] = userId
                    it[FcmTokenTable.token] = token
                    it[FcmTokenTable.deviceName] = deviceName
                    it[FcmTokenTable.createdAt] = now
                    it[FcmTokenTable.updatedAt] = now
                }
            } else {
                FcmTokenTable.update({ (FcmTokenTable.userId eq userId) and (FcmTokenTable.token eq token) }) {
                    it[FcmTokenTable.updatedAt] = now
                    it[FcmTokenTable.deviceName] = deviceName
                }
            }
        }
    }

    fun unregisterToken(userId: Int, token: String) {
        transaction {
            FcmTokenTable.deleteWhere { (FcmTokenTable.userId eq userId) and (FcmTokenTable.token eq token) }
        }
    }

    fun shouldNotify(userId: Int, groupId: Int, notificationType: String): Boolean {
        return transaction {
            val pref = NotificationPreferenceTable.select {
                (NotificationPreferenceTable.userId eq userId) and (NotificationPreferenceTable.groupId eq groupId)
            }.firstOrNull()
            if (pref == null) true
            else when (notificationType) {
                "task_assigned" -> pref[NotificationPreferenceTable.taskAssigned]
                "task_status_changed" -> pref[NotificationPreferenceTable.taskStatusChanged]
                "meeting_created" -> pref[NotificationPreferenceTable.meetingCreated]
                "meeting_reminder" -> pref[NotificationPreferenceTable.meetingReminder]
                "announcement_posted" -> pref[NotificationPreferenceTable.announcementPosted]
                "group_invite" -> pref[NotificationPreferenceTable.groupInvite]
                else -> true
            }
        }
    }

    fun getMeetingReminderMinutes(userId: Int, groupId: Int): Int {
        return transaction {
            NotificationPreferenceTable.select {
                (NotificationPreferenceTable.userId eq userId) and (NotificationPreferenceTable.groupId eq groupId)
            }.firstOrNull()?.get(NotificationPreferenceTable.meetingReminderMinutes) ?: 30
        }
    }
}
