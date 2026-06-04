package com.anjira.routes

import com.anjira.db.*
import com.anjira.service.FcmNotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class FcmRegisterRequest(val token: String, val deviceName: String? = null)
data class FcmUnregisterRequest(val token: String)
data class NotificationPreferencesRequest(
    val taskAssigned: Boolean? = null,
    val taskStatusChanged: Boolean? = null,
    val meetingCreated: Boolean? = null,
    val meetingReminder: Boolean? = null,
    val announcementPosted: Boolean? = null,
    val groupInvite: Boolean? = null,
    val meetingReminderMinutes: Int? = null
)
data class NotificationPreferencesResponse(
    val taskAssigned: Boolean, val taskStatusChanged: Boolean,
    val meetingCreated: Boolean, val meetingReminder: Boolean,
    val announcementPosted: Boolean, val groupInvite: Boolean,
    val meetingReminderMinutes: Int
)

fun Route.NotificationRoutes() {
    route("/notifications") {
        post("/fcm/register") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<FcmRegisterRequest>()
            FcmNotificationService.registerToken(userId, request.token, request.deviceName)
            call.respond(HttpStatusCode.Created, mapOf("message" to "Token registered"))
        }

        post("/fcm/unregister") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<FcmUnregisterRequest>()
            FcmNotificationService.unregisterToken(userId, request.token)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/preferences/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get
            }
            val prefs = transaction {
                NotificationPreferenceTable.select {
                    (NotificationPreferenceTable.userId eq userId) and (NotificationPreferenceTable.groupId eq groupId)
                }.firstOrNull()
            }
            call.respond(NotificationPreferencesResponse(
                taskAssigned = prefs?.get(NotificationPreferenceTable.taskAssigned) ?: true,
                taskStatusChanged = prefs?.get(NotificationPreferenceTable.taskStatusChanged) ?: true,
                meetingCreated = prefs?.get(NotificationPreferenceTable.meetingCreated) ?: true,
                meetingReminder = prefs?.get(NotificationPreferenceTable.meetingReminder) ?: true,
                announcementPosted = prefs?.get(NotificationPreferenceTable.announcementPosted) ?: true,
                groupInvite = prefs?.get(NotificationPreferenceTable.groupInvite) ?: true,
                meetingReminderMinutes = prefs?.get(NotificationPreferenceTable.meetingReminderMinutes) ?: 30
            ))
        }

        put("/preferences/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@put
            }
            val request = call.receive<NotificationPreferencesRequest>()
            transaction {
                val existing = NotificationPreferenceTable.select {
                    (NotificationPreferenceTable.userId eq userId) and (NotificationPreferenceTable.groupId eq groupId)
                }.firstOrNull()
                if (existing == null) {
                    NotificationPreferenceTable.insert {
                        it[NotificationPreferenceTable.userId] = userId
                        it[NotificationPreferenceTable.groupId] = groupId
                        request.taskAssigned?.let { v -> it[NotificationPreferenceTable.taskAssigned] = v }
                        request.taskStatusChanged?.let { v -> it[NotificationPreferenceTable.taskStatusChanged] = v }
                        request.meetingCreated?.let { v -> it[NotificationPreferenceTable.meetingCreated] = v }
                        request.meetingReminder?.let { v -> it[NotificationPreferenceTable.meetingReminder] = v }
                        request.announcementPosted?.let { v -> it[NotificationPreferenceTable.announcementPosted] = v }
                        request.groupInvite?.let { v -> it[NotificationPreferenceTable.groupInvite] = v }
                        request.meetingReminderMinutes?.let { v -> it[NotificationPreferenceTable.meetingReminderMinutes] = v }
                    }
                } else {
                    NotificationPreferenceTable.update({
                        (NotificationPreferenceTable.userId eq userId) and (NotificationPreferenceTable.groupId eq groupId)
                    }) { upd ->
                        request.taskAssigned?.let { upd[NotificationPreferenceTable.taskAssigned] = it }
                        request.taskStatusChanged?.let { upd[NotificationPreferenceTable.taskStatusChanged] = it }
                        request.meetingCreated?.let { upd[NotificationPreferenceTable.meetingCreated] = it }
                        request.meetingReminder?.let { upd[NotificationPreferenceTable.meetingReminder] = it }
                        request.announcementPosted?.let { upd[NotificationPreferenceTable.announcementPosted] = it }
                        request.groupInvite?.let { upd[NotificationPreferenceTable.groupInvite] = it }
                        request.meetingReminderMinutes?.let { upd[NotificationPreferenceTable.meetingReminderMinutes] = it }
                    }
                }
            }
            call.respond(mapOf("message" to "Preferences updated"))
        }
    }
}
