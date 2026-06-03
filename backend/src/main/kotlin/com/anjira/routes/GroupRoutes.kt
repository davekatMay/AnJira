package com.anjira.routes

import com.anjira.db.*
import com.anjira.security.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger("GroupRoutes")
private val httpClient = HttpClient(CIO)

// ─── Group DTOs ───────────────────────────────────────────────────────────
data class GroupCreateRequest(val name: String, val description: String? = null, val avatar: String? = null)
data class GroupUpdateRequest(val name: String? = null, val description: String? = null, val avatar: String? = null)
data class GroupMemberResponse(val userId: Int, val username: String, val email: String, val role: String, val createdAt: String, val updatedAt: String)
data class GroupResponse(val id: Int, val name: String, val description: String?, val avatar: String?, val inviteCode: String, val createdBy: String, val members: List<GroupMemberResponse>, val createdAt: String, val updatedAt: String)
data class AddMemberRequest(val userId: Int, val role: String = "member")
data class UpdateRoleRequest(val role: String)
data class JoinByCodeRequest(val inviteCode: String)

// ─── Task DTOs ────────────────────────────────────────────────────────────
data class TaskCreateRequest(val title: String, val description: String? = null, val deadline: String? = null, val assignedTo: Int? = null)
data class TaskUpdateRequest(val title: String? = null, val description: String? = null, val deadline: String? = null, val status: String? = null, val assignedTo: Int? = null)
data class TaskResponse(val id: Int, val title: String, val description: String?, val deadline: String?, val status: String, val createdBy: Int, val assignedTo: Int?, val createdAt: String, val updatedAt: String)

// ─── Subtask DTOs ─────────────────────────────────────────────────────────
data class SubtaskCreateRequest(val title: String)
data class SubtaskResponse(val id: Int, val title: String, val isCompleted: Boolean, val createdAt: String, val updatedAt: String)

// ─── Meeting DTOs ─────────────────────────────────────────────────────────
data class MeetingCreateRequest(val title: String, val description: String? = null, val dateTime: String, val endDateTime: String? = null, val location: String? = null)
data class MeetingUpdateRequest(val title: String? = null, val description: String? = null, val dateTime: String? = null, val endDateTime: String? = null, val location: String? = null)
data class MeetingResponse(val id: Int, val title: String, val description: String?, val dateTime: String, val endDateTime: String?, val location: String?, val createdBy: Int, val createdAt: String, val updatedAt: String)
data class MeetingParticipantResponse(val userId: Int, val username: String, val email: String)
data class AddParticipantRequest(val userId: Int)
data class RsvpRequest(val status: String) // going, maybe, declined

// ─── Announcement DTOs ────────────────────────────────────────────────────
data class AnnouncementCreateRequest(val text: String, val attachments: String = "[]")
data class AnnouncementUpdateRequest(val text: String? = null, val attachments: String? = null)
data class AnnouncementResponse(val id: Int, val groupId: Int, val text: String, val attachments: String, val isPinned: Boolean, val createdBy: Int, val createdByUsername: String, val createdAt: String, val updatedAt: String)

// ─── Playlist DTOs ────────────────────────────────────────────────────────
data class PlaylistCreateRequest(val name: String, val type: String = "group", val meetingId: Int? = null)
data class PlaylistUpdateRequest(val name: String? = null)
data class PlaylistResponse(val id: Int, val groupId: Int, val name: String, val type: String, val meetingId: Int?, val createdBy: Int, val createdByUsername: String, val createdAt: String, val updatedAt: String)
data class AddTrackRequest(val trackId: String, val trackName: String, val artistName: String, val trackViewUrl: String, val artworkUrl100: String? = null, val previewUrl: String? = null)
data class TrackResponse(val id: Int, val playlistId: Int, val trackId: String, val trackName: String, val artistName: String, val trackViewUrl: String, val artworkUrl100: String?, val previewUrl: String?, val sortOrder: Int, val createdAt: String)
data class ReorderTracksRequest(val trackIds: List<Int>)

// ─── Notification DTOs ────────────────────────────────────────────────────
data class NotificationResponse(val id: Int, val type: String, val referenceId: Int, val message: String, val isRead: Boolean, val createdAt: String)

fun Route.GroupRoute() {

    // ─── GROUPS ───────────────────────────────────────────────────────────
    route("/groups") {
        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<GroupCreateRequest>()
            val inviteCode = generateInviteCode()
            val now = LocalDateTime.now().toString()
            val groupId = transaction {
                GroupTable.insertAndGetId {
                    it[GroupTable.name] = request.name; it[GroupTable.description] = request.description
                    it[GroupTable.avatar] = request.avatar; it[GroupTable.inviteCode] = inviteCode
                    it[GroupTable.createdBy] = userId; it[GroupTable.createdAt] = now; it[GroupTable.updatedAt] = now
                }
            }
            transaction { GroupMemberTable.insert { m -> m[GroupMemberTable.groupId] = groupId.value; m[GroupMemberTable.userId] = userId; m[GroupMemberTable.role] = "admin"; m[GroupMemberTable.createdAt] = now; m[GroupMemberTable.updatedAt] = now } }
            call.respond(HttpStatusCode.Created, GroupResponse(groupId.value, request.name, request.description, request.avatar, inviteCode, getUsername(userId), listOf(GroupMemberResponse(userId, getUsername(userId), getEmail(userId), "admin", now, now)), now, now))
        }

        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupIds = transaction { GroupMemberTable.select { GroupMemberTable.userId eq userId }.map { it[GroupMemberTable.groupId] } }
            call.respond(transaction { GroupTable.select { GroupTable.id inList groupIds }.map { row -> row.toGroupResponse() } })
        }

        post("/join") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<JoinByCodeRequest>()
            val group = transaction { GroupTable.select { GroupTable.inviteCode eq request.inviteCode }.firstOrNull() }
            if (group == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invalid invite code")); return@post }
            val groupId = group[GroupTable.id].value
            if (transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) }.firstOrNull() } != null) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already a member")); return@post }
            val now = LocalDateTime.now().toString()
            transaction { GroupMemberTable.insert { m -> m[GroupMemberTable.groupId] = groupId; m[GroupMemberTable.userId] = userId; m[GroupMemberTable.role] = "member"; m[GroupMemberTable.createdAt] = now; m[GroupMemberTable.updatedAt] = now } }
            call.respond(group.toGroupResponse())
        }

        get("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val group = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Group not found")); return@get }
            call.respond(group.toGroupResponse())
        }

        put("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@put }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can edit group")); return@put }
            val request = call.receive<GroupUpdateRequest>()
            val group = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Group not found")); return@put }
            val now = LocalDateTime.now().toString()
            transaction { GroupTable.update({ GroupTable.id eq groupId }) { upd -> request.name?.let { upd[GroupTable.name] = it }; upd[GroupTable.description] = request.description; upd[GroupTable.avatar] = request.avatar; upd[GroupTable.updatedAt] = now } }
            val updated = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull()!! }
            call.respond(updated.toGroupResponse())
        }

        delete("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@delete }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can delete group")); return@delete }
            transaction {
                MeetingParticipantTable.deleteWhere { MeetingParticipantTable.meetingId inList MeetingTable.select { MeetingTable.groupId eq groupId }.map { it[MeetingTable.id].value } }
                MeetingTable.deleteWhere { MeetingTable.groupId eq groupId }
                SubtaskTable.deleteWhere { SubtaskTable.taskId inList TaskTable.select { TaskTable.groupId eq groupId }.map { it[TaskTable.id].value } }
                TaskTable.deleteWhere { TaskTable.groupId eq groupId }
                GroupMemberTable.deleteWhere { GroupMemberTable.groupId eq groupId }
                GroupTable.deleteWhere { GroupTable.id eq groupId }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        // ─── Members ──────────────────────────────────────────────────────
        post("/{groupId}/members") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can add members")); return@post }
            val request = call.receive<AddMemberRequest>()
            if (transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq request.userId) }.firstOrNull() } != null) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already a member")); return@post }
            val now = LocalDateTime.now().toString()
            transaction { GroupMemberTable.insert { m -> m[GroupMemberTable.groupId] = groupId; m[GroupMemberTable.userId] = request.userId; m[GroupMemberTable.role] = request.role; m[GroupMemberTable.createdAt] = now; m[GroupMemberTable.updatedAt] = now } }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Member added"))
        }

        delete("/{groupId}/members/{memberId}") {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@delete }
            val memberId = call.parameters["memberId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid member ID")); return@delete }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can remove members")); return@delete }
            if (userId == memberId) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot remove yourself")); return@delete }
            transaction { GroupMemberTable.deleteWhere { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq memberId) } }
            call.respond(HttpStatusCode.NoContent)
        }

        put("/{groupId}/members/{memberId}/role") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@put }
            val memberId = call.parameters["memberId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid member ID")); return@put }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can change roles")); return@put }
            val request = call.receive<UpdateRoleRequest>()
            if (request.role !in listOf("admin", "member")) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role")); return@put }
            val now = LocalDateTime.now().toString()
            transaction { GroupMemberTable.update({ (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq memberId) }) { it[GroupMemberTable.role] = request.role; it[GroupMemberTable.updatedAt] = now } }
            call.respond(mapOf("message" to "Role updated"))
        }
    }

    // ─── TASKS ────────────────────────────────────────────────────────────
    route("/groups/{groupId}/tasks") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }

            val filter = call.request.queryParameters["filter"] ?: "all"
            val statusFilter = call.request.queryParameters["status"]

            var query = TaskTable.select { TaskTable.groupId eq groupId }
            when (filter) {
                "mine" -> query = query.andWhere { TaskTable.assignedTo eq userId }
            }
            statusFilter?.let { query = query.andWhere { TaskTable.status eq it } }

            call.respond(query.orderBy(TaskTable.createdAt, SortOrder.DESC).map { it.toTaskResponse() })
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<TaskCreateRequest>()
            val now = LocalDateTime.now().toString()
            val taskId = transaction {
                TaskTable.insertAndGetId {
                    it[TaskTable.groupId] = groupId; it[TaskTable.title] = request.title
                    it[TaskTable.description] = request.description; it[TaskTable.deadline] = request.deadline
                    it[TaskTable.status] = "to_do"; it[TaskTable.createdBy] = userId
                    it[TaskTable.assignedTo] = request.assignedTo; it[TaskTable.createdAt] = now; it[TaskTable.updatedAt] = now
                }
            }
            request.assignedTo?.let { createNotification(it, "task_assigned", taskId.value, "Вам назначена задача: ${request.title}") }
            call.respond(TaskResponse(taskId.value, request.title, request.description, request.deadline, "to_do", userId, request.assignedTo, now, now))
        }
    }

    route("/tasks/{taskId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val taskId = call.parameters["taskId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@put }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@put }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val request = call.receive<TaskUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                TaskTable.update({ TaskTable.id eq taskId }) { upd ->
                    request.title?.let { upd[TaskTable.title] = it }
                    upd[TaskTable.description] = request.description
                    upd[TaskTable.deadline] = request.deadline
                    request.status?.let { upd[TaskTable.status] = it }
                    upd[TaskTable.assignedTo] = request.assignedTo
                    upd[TaskTable.updatedAt] = now
                }
            }
            val updated = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull()!! }
            call.respond(updated.toTaskResponse())
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val taskId = call.parameters["taskId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@delete }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@delete }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { SubtaskTable.deleteWhere { SubtaskTable.taskId eq taskId }; TaskTable.deleteWhere { TaskTable.id eq taskId } }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    // ─── SUBTASKS ─────────────────────────────────────────────────────────
    route("/tasks/{taskId}/subtasks") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val taskId = call.parameters["taskId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@get }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@get }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            call.respond(transaction { SubtaskTable.select { SubtaskTable.taskId eq taskId }.map { row -> SubtaskResponse(row[SubtaskTable.id].value, row[SubtaskTable.title], row[SubtaskTable.isCompleted], row[SubtaskTable.createdAt], row[SubtaskTable.updatedAt]) } })
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val taskId = call.parameters["taskId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@post }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@post }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<SubtaskCreateRequest>()
            val now = LocalDateTime.now().toString()
            val subtaskId = transaction { SubtaskTable.insertAndGetId { s -> s[SubtaskTable.taskId] = taskId; s[SubtaskTable.title] = request.title; s[SubtaskTable.isCompleted] = false; s[SubtaskTable.createdAt] = now; s[SubtaskTable.updatedAt] = now } }
            call.respond(SubtaskResponse(subtaskId.value, request.title, false, now, now))
        }
    }

    route("/subtasks/{subtaskId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val subtaskId = call.parameters["subtaskId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid subtask ID")); return@put }
            val subtask = transaction { SubtaskTable.select { SubtaskTable.id eq subtaskId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subtask not found")); return@put }
            val task = transaction { TaskTable.select { TaskTable.id eq subtask[SubtaskTable.taskId] }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@put }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val isCompleted = call.request.queryParameters["isCompleted"]?.toBoolean()
            val now = LocalDateTime.now().toString()
            transaction { SubtaskTable.update({ SubtaskTable.id eq subtaskId }) { upd -> isCompleted?.let { upd[SubtaskTable.isCompleted] = it }; upd[SubtaskTable.updatedAt] = now } }
            val updated = transaction { SubtaskTable.select { SubtaskTable.id eq subtaskId }.firstOrNull()!! }
            call.respond(SubtaskResponse(subtaskId, updated[SubtaskTable.title], updated[SubtaskTable.isCompleted], updated[SubtaskTable.createdAt], updated[SubtaskTable.updatedAt]))
        }
    }

    // ─── MEETINGS ─────────────────────────────────────────────────────────
    route("/groups/{groupId}/meetings") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            call.respond(transaction { MeetingTable.select { MeetingTable.groupId eq groupId }.orderBy(MeetingTable.dateTime, SortOrder.ASC).map { it.toMeetingResponse() } })
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<MeetingCreateRequest>()
            val now = LocalDateTime.now().toString()
            val meetingId = transaction {
                MeetingTable.insertAndGetId {
                    it[MeetingTable.groupId] = groupId; it[MeetingTable.title] = request.title
                    it[MeetingTable.description] = request.description; it[MeetingTable.dateTime] = request.dateTime
                    it[MeetingTable.endDateTime] = request.endDateTime; it[MeetingTable.location] = request.location
                    it[MeetingTable.createdBy] = userId; it[MeetingTable.createdAt] = now; it[MeetingTable.updatedAt] = now
                }
            }
            call.respond(MeetingResponse(meetingId.value, request.title, request.description, request.dateTime, request.endDateTime, request.location, userId, now, now))
        }
    }

    route("/meetings/{meetingId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@put }
            val meeting = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@put }
            if (!isMember(meeting[MeetingTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val request = call.receive<MeetingUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                MeetingTable.update({ MeetingTable.id eq meetingId }) { upd ->
                    request.title?.let { upd[MeetingTable.title] = it }; upd[MeetingTable.description] = request.description
                    request.dateTime?.let { upd[MeetingTable.dateTime] = it }; upd[MeetingTable.endDateTime] = request.endDateTime
                    upd[MeetingTable.location] = request.location; upd[MeetingTable.updatedAt] = now
                }
            }
            val updated = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()!! }
            call.respond(updated.toMeetingResponse())
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@delete }
            val mid = meetingId; val groupId = transaction { MeetingTable.select { MeetingTable.id eq mid }.firstOrNull()?.get(MeetingTable.groupId) } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@delete }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { MeetingParticipantTable.deleteWhere { MeetingParticipantTable.meetingId eq mid }; MeetingTable.deleteWhere { MeetingTable.id eq mid } }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    // ─── MEETING PARTICIPANTS / RSVP ──────────────────────────────────────
    route("/meetings/{meetingId}/participants") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@get }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            call.respond(transaction { (MeetingParticipantTable innerJoin UserTable).select { MeetingParticipantTable.meetingId eq meetingId }.map { row -> MeetingParticipantResponse(row[UserTable.id].value, row[UserTable.username], row[UserTable.email]) } })
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@post }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<AddParticipantRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                MeetingParticipantTable.insert { m -> m[MeetingParticipantTable.meetingId] = meetingId; m[MeetingParticipantTable.userId] = request.userId; m[MeetingParticipantTable.status] = "pending"; m[MeetingParticipantTable.createdAt] = now; m[MeetingParticipantTable.updatedAt] = now }
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Participant added"))
        }

        delete("/{participantId}") {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@delete }
            val targetUserId = call.parameters["participantId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid participant ID")); return@delete }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@delete }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { MeetingParticipantTable.deleteWhere { (MeetingParticipantTable.meetingId eq meetingId) and (MeetingParticipantTable.userId eq targetUserId) } }
            call.respond(HttpStatusCode.NoContent)
        }

        put("/{participantId}/rsvp") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@put }
            val targetUserId = call.parameters["participantId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid participant ID")); return@put }
            // Only the participant themselves can RSVP
            if (targetUserId != userId) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot change another user's RSVP")); return@put }
            val request = call.receive<RsvpRequest>()
            if (request.status !in listOf("going", "maybe", "declined")) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status")); return@put }
            val now = LocalDateTime.now().toString()
            val updated = transaction {
                MeetingParticipantTable.update({ (MeetingParticipantTable.meetingId eq meetingId) and (MeetingParticipantTable.userId eq targetUserId) }) { upd -> upd[MeetingParticipantTable.status] = request.status; upd[MeetingParticipantTable.updatedAt] = now }
            }
            if (updated == 0) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Participant not found")); return@put }
            call.respond(mapOf("message" to "RSVP updated", "status" to request.status))
        }
    }

    // ─── ANNOUNCEMENTS ────────────────────────────────────────────────────
    route("/groups/{groupId}/announcements") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val announcements = transaction {
                (AnnouncementTable innerJoin UserTable).select { AnnouncementTable.groupId eq groupId }
                    .orderBy(AnnouncementTable.isPinned, SortOrder.DESC)
                    .orderBy(AnnouncementTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        AnnouncementResponse(
                            id = row[AnnouncementTable.id].value, groupId = groupId,
                            text = row[AnnouncementTable.text], attachments = row[AnnouncementTable.attachments],
                            isPinned = row[AnnouncementTable.isPinned], createdBy = row[AnnouncementTable.createdBy],
                            createdByUsername = row[UserTable.username],
                            createdAt = row[AnnouncementTable.createdAt], updatedAt = row[AnnouncementTable.updatedAt]
                        )
                    }
            }
            call.respond(announcements)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<AnnouncementCreateRequest>()
            val now = LocalDateTime.now().toString()
            val annId = transaction {
                AnnouncementTable.insertAndGetId {
                    it[AnnouncementTable.groupId] = groupId; it[AnnouncementTable.text] = request.text
                    it[AnnouncementTable.attachments] = request.attachments
                    it[AnnouncementTable.isPinned] = false; it[AnnouncementTable.createdBy] = userId
                    it[AnnouncementTable.createdAt] = now; it[AnnouncementTable.updatedAt] = now
                }
            }
            call.respond(HttpStatusCode.Created, AnnouncementResponse(annId.value, groupId, request.text, request.attachments, false, userId, getUsername(userId), now, now))
        }
    }

    route("/announcements/{announcementId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val annId = call.parameters["announcementId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid announcement ID")); return@put }
            val ann = transaction { AnnouncementTable.select { AnnouncementTable.id eq annId }.firstOrNull() }
            if (ann == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Announcement not found")); return@put }
            val groupId = ann[AnnouncementTable.groupId]
            val authorId = ann[AnnouncementTable.createdBy]
            val isGroupAdmin = transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) and (GroupMemberTable.role eq "admin") }.firstOrNull() != null }
            if (userId != authorId && !isGroupAdmin) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only author or admin can edit")); return@put }
            val request = call.receive<AnnouncementUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction { AnnouncementTable.update({ AnnouncementTable.id eq annId }) { upd -> request.text?.let { upd[AnnouncementTable.text] = it }; request.attachments?.let { upd[AnnouncementTable.attachments] = it }; upd[AnnouncementTable.updatedAt] = now } }
            val updated = transaction { (AnnouncementTable innerJoin UserTable).select { AnnouncementTable.id eq annId }.firstOrNull()!! }
            call.respond(updated.toAnnouncementResponse(groupId))
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val annId = call.parameters["announcementId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid announcement ID")); return@delete }
            val ann = transaction { AnnouncementTable.select { AnnouncementTable.id eq annId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Announcement not found")); return@delete }
            val groupId = ann[AnnouncementTable.groupId]
            val authorId = ann[AnnouncementTable.createdBy]
            val isGroupAdmin = transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) and (GroupMemberTable.role eq "admin") }.firstOrNull() != null }
            if (userId != authorId && !isGroupAdmin) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only author or admin can delete")); return@delete }
            transaction { AnnouncementTable.deleteWhere { AnnouncementTable.id eq annId } }
            call.respond(HttpStatusCode.NoContent)
        }

        put("/pin") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val annId = call.parameters["announcementId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid announcement ID")); return@put }
            val ann = transaction { AnnouncementTable.select { AnnouncementTable.id eq annId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Announcement not found")); return@put }
            val groupId = ann[AnnouncementTable.groupId]
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can pin announcements")); return@put }
            val now = LocalDateTime.now().toString()
            transaction { AnnouncementTable.update({ AnnouncementTable.id eq annId }) { upd -> upd[AnnouncementTable.isPinned] = !ann[AnnouncementTable.isPinned]; upd[AnnouncementTable.updatedAt] = now } }
            call.respond(mapOf("message" to "Pin status toggled"))
        }
    }

    // ─── PLAYLISTS ────────────────────────────────────────────────────────
    route("/groups/{groupId}/playlists") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val playlists = transaction {
                (PlaylistTable innerJoin UserTable).select { PlaylistTable.groupId eq groupId }
                    .orderBy(PlaylistTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        PlaylistResponse(row[PlaylistTable.id].value, groupId, row[PlaylistTable.name],
                            row[PlaylistTable.type], row[PlaylistTable.meetingId],
                            row[PlaylistTable.createdBy], row[UserTable.username],
                            row[PlaylistTable.createdAt], row[PlaylistTable.updatedAt])
                    }
            }
            call.respond(playlists)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<PlaylistCreateRequest>()
            val now = LocalDateTime.now().toString()
            val plId = transaction {
                PlaylistTable.insertAndGetId {
                    it[PlaylistTable.groupId] = groupId; it[PlaylistTable.name] = request.name
                    it[PlaylistTable.type] = request.type; it[PlaylistTable.meetingId] = request.meetingId
                    it[PlaylistTable.createdBy] = userId; it[PlaylistTable.createdAt] = now; it[PlaylistTable.updatedAt] = now
                }
            }
            call.respond(HttpStatusCode.Created, PlaylistResponse(plId.value, groupId, request.name, request.type, request.meetingId, userId, getUsername(userId), now, now))
        }
    }

    route("/playlists/{playlistId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@put }
            val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@put }
            if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val request = call.receive<PlaylistUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction { PlaylistTable.update({ PlaylistTable.id eq plId }) { upd -> request.name?.let { upd[PlaylistTable.name] = it }; upd[PlaylistTable.updatedAt] = now } }
            call.respond(mapOf("message" to "Playlist updated"))
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@delete }
            val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@delete }
            if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { PlaylistTrackTable.deleteWhere { PlaylistTrackTable.playlistId eq plId }; PlaylistTable.deleteWhere { PlaylistTable.id eq plId } }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    // ─── PLAYLIST TRACKS ──────────────────────────────────────────────────
    route("/playlists/{playlistId}/tracks") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@get }
            val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@get }
            if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val tracks = transaction {
                PlaylistTrackTable.select { PlaylistTrackTable.playlistId eq plId }
                    .orderBy(PlaylistTrackTable.sortOrder, SortOrder.ASC)
                    .map { row ->
                        TrackResponse(row[PlaylistTrackTable.id].value, plId, row[PlaylistTrackTable.trackId],
                            row[PlaylistTrackTable.trackName], row[PlaylistTrackTable.artistName],
                            row[PlaylistTrackTable.trackViewUrl], row[PlaylistTrackTable.artworkUrl100],
                            row[PlaylistTrackTable.previewUrl], row[PlaylistTrackTable.sortOrder],
                            row[PlaylistTrackTable.createdAt])
                    }
            }
            call.respond(tracks)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@post }
            val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@post }
            if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<AddTrackRequest>()
            val now = LocalDateTime.now().toString()
            val maxOrder = transaction { PlaylistTrackTable.select { PlaylistTrackTable.playlistId eq plId }.maxOfOrNull { it[PlaylistTrackTable.sortOrder] } ?: 0 }
            val trId = transaction {
                PlaylistTrackTable.insertAndGetId {
                    it[PlaylistTrackTable.playlistId] = plId; it[PlaylistTrackTable.trackId] = request.trackId
                    it[PlaylistTrackTable.trackName] = request.trackName; it[PlaylistTrackTable.artistName] = request.artistName
                    it[PlaylistTrackTable.trackViewUrl] = request.trackViewUrl; it[PlaylistTrackTable.artworkUrl100] = request.artworkUrl100
                    it[PlaylistTrackTable.previewUrl] = request.previewUrl; it[PlaylistTrackTable.sortOrder] = maxOrder + 1
                    it[PlaylistTrackTable.createdAt] = now
                }
            }
            call.respond(HttpStatusCode.Created, TrackResponse(trId.value, plId, request.trackId, request.trackName, request.artistName, request.trackViewUrl, request.artworkUrl100, request.previewUrl, maxOrder + 1, now))
        }
    }

    route("/playlists/{playlistId}/tracks/{trackId}") {
        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@delete }
            val trId = call.parameters["trackId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid track ID")); return@delete }
            val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@delete }
            if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { PlaylistTrackTable.deleteWhere { (PlaylistTrackTable.playlistId eq plId) and (PlaylistTrackTable.id eq trId) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    put("/playlists/{playlistId}/tracks/reorder") {
        val userId = getAuthenticatedUserId(call) ?: return@put
        val plId = call.parameters["playlistId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID")); return@put }
        val pl = transaction { PlaylistTable.select { PlaylistTable.id eq plId }.firstOrNull() } ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Playlist not found")); return@put }
        if (!isMember(pl[PlaylistTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
        val request = call.receive<ReorderTracksRequest>()
        transaction {
            request.trackIds.forEachIndexed { index, trackId ->
                PlaylistTrackTable.update({ (PlaylistTrackTable.playlistId eq plId) and (PlaylistTrackTable.id eq trackId) }) { upd -> upd[PlaylistTrackTable.sortOrder] = index + 1 }
            }
        }
        call.respond(mapOf("message" to "Tracks reordered"))
    }

    // ─── iTunes SEARCH ────────────────────────────────────────────────────
    get("/itunes/search") {
        val userId = getAuthenticatedUserId(call) ?: return@get
        val term = call.request.queryParameters["term"] ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Search term required")); return@get }
        val limit = call.request.queryParameters["limit"] ?: "20"
        val response = httpClient.get("https://itunes.apple.com/search") {
            parameter("term", term)
            parameter("limit", limit)
            parameter("entity", "song")
        }
        call.respondText(response.bodyAsText(), ContentType.Application.Json)
    }

    // ─── NOTIFICATIONS ────────────────────────────────────────────────────
    get("/notifications") {
        val userId = getAuthenticatedUserId(call) ?: return@get
        val notifications = transaction {
            NotificationTable.select { NotificationTable.userId eq userId }
                .orderBy(NotificationTable.createdAt, SortOrder.DESC)
                .map { row ->
                    NotificationResponse(row[NotificationTable.id].value, row[NotificationTable.type],
                        row[NotificationTable.referenceId], row[NotificationTable.message],
                        row[NotificationTable.isRead], row[NotificationTable.createdAt])
                }
        }
        call.respond(notifications)
    }

    put("/notifications/{notificationId}/read") {
        val userId = getAuthenticatedUserId(call) ?: return@put
        val notId = call.parameters["notificationId"]?.toIntOrNull() ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid notification ID")); return@put }
        transaction { NotificationTable.update({ (NotificationTable.id eq notId) and (NotificationTable.userId eq userId) }) { upd -> upd[NotificationTable.isRead] = true } }
        call.respond(mapOf("message" to "Notification marked as read"))
    }
}

// ─── HELPERS ──────────────────────────────────────────────────────────────
suspend fun getAuthenticatedUserId(call: io.ktor.server.application.ApplicationCall): Int? {
    val principal = call.principal<UserIdPrincipal>()
    if (principal == null) { call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return null }
    val userId = principal.name.toIntOrNull()
    if (userId == null) { call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token")); return null }
    return userId
}

private fun isMember(groupId: Int, userId: Int): Boolean =
    transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) }.firstOrNull() != null }

private fun isAdmin(groupId: Int, userId: Int): Boolean =
    transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) and (GroupMemberTable.role eq "admin") }.firstOrNull() != null }

private fun getGroupMembers(groupId: Int): List<GroupMemberResponse> =
    transaction { (GroupMemberTable innerJoin UserTable).select { GroupMemberTable.groupId eq groupId }.map { row -> GroupMemberResponse(row[UserTable.id].value, row[UserTable.username], row[UserTable.email], row[GroupMemberTable.role], row[GroupMemberTable.createdAt], row[GroupMemberTable.updatedAt]) } }

private fun getUsername(userId: Int): String =
    transaction { UserTable.select { UserTable.id eq userId }.firstOrNull()?.get(UserTable.username) ?: "unknown" }

private fun getEmail(userId: Int): String =
    transaction { UserTable.select { UserTable.id eq userId }.firstOrNull()?.get(UserTable.email) ?: "unknown" }

private fun generateInviteCode(): String =
    UUID.randomUUID().toString().take(8).uppercase()

private fun createNotification(userId: Int, type: String, referenceId: Int, message: String) {
    transaction { NotificationTable.insert { n -> n[NotificationTable.userId] = userId; n[NotificationTable.type] = type; n[NotificationTable.referenceId] = referenceId; n[NotificationTable.message] = message; n[NotificationTable.createdAt] = LocalDateTime.now().toString() } }
}

// ─── Extension mappers ────────────────────────────────────────────────────
private fun ResultRow.toGroupResponse(): GroupResponse {
    val gId = this[GroupTable.id].value
    return GroupResponse(gId, this[GroupTable.name], this[GroupTable.description], this[GroupTable.avatar],
        this[GroupTable.inviteCode], getUsername(this[GroupTable.createdBy]), getGroupMembers(gId),
        this[GroupTable.createdAt], this[GroupTable.updatedAt])
}

private fun ResultRow.toTaskResponse(): TaskResponse {
    return TaskResponse(this[TaskTable.id].value, this[TaskTable.title], this[TaskTable.description],
        this[TaskTable.deadline], this[TaskTable.status], this[TaskTable.createdBy],
        this[TaskTable.assignedTo], this[TaskTable.createdAt], this[TaskTable.updatedAt])
}

private fun ResultRow.toMeetingResponse(): MeetingResponse {
    return MeetingResponse(this[MeetingTable.id].value, this[MeetingTable.title], this[MeetingTable.description],
        this[MeetingTable.dateTime], this[MeetingTable.endDateTime], this[MeetingTable.location],
        this[MeetingTable.createdBy], this[MeetingTable.createdAt], this[MeetingTable.updatedAt])
}

private fun ResultRow.toAnnouncementResponse(groupId: Int): AnnouncementResponse {
    return AnnouncementResponse(this[AnnouncementTable.id].value, groupId, this[AnnouncementTable.text],
        this[AnnouncementTable.attachments], this[AnnouncementTable.isPinned],
        this[AnnouncementTable.createdBy], this[UserTable.username],
        this[AnnouncementTable.createdAt], this[AnnouncementTable.updatedAt])
}
