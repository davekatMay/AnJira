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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger("GroupRoutes")

// Group DTOs
data class GroupCreateRequest(val name: String, val description: String? = null, val avatar: String? = null)
data class GroupUpdateRequest(val name: String? = null, val description: String? = null, val avatar: String? = null)
data class GroupMemberResponse(val userId: Int, val username: String, val email: String, val role: String, val createdAt: String, val updatedAt: String)
data class GroupResponse(val id: Int, val name: String, val description: String?, val avatar: String?, val inviteCode: String, val createdBy: String, val members: List<GroupMemberResponse>, val createdAt: String, val updatedAt: String)
data class AddMemberRequest(val userId: Int, val role: String = "member")
data class UpdateRoleRequest(val role: String)
data class JoinByCodeRequest(val inviteCode: String)

// Task DTOs
data class TaskCreateRequest(val title: String, val description: String? = null, val assignedTo: Int? = null)
data class TaskUpdateRequest(val title: String? = null, val description: String? = null, val status: String? = null, val assignedTo: Int? = null)
data class TaskResponse(val id: Int, val title: String, val description: String?, val status: String, val createdBy: Int, val assignedTo: Int?, val createdAt: String, val updatedAt: String)

// Subtask DTOs
data class SubtaskCreateRequest(val title: String)
data class SubtaskResponse(val id: Int, val title: String, val isCompleted: Boolean, val createdAt: String, val updatedAt: String)

// Meeting DTOs
data class MeetingCreateRequest(val title: String, val description: String? = null, val dateTime: String, val location: String? = null)
data class MeetingUpdateRequest(val title: String? = null, val description: String? = null, val dateTime: String? = null, val location: String? = null)
data class MeetingResponse(val id: Int, val title: String, val description: String?, val dateTime: String, val location: String?, val createdBy: Int, val createdAt: String, val updatedAt: String)
data class MeetingParticipantResponse(val userId: Int, val username: String, val email: String)
data class AddParticipantRequest(val userId: Int)

fun Route.GroupRoute() {
    // ─── Group CRUD ───────────────────────────────────────────────────────
    route("/groups") {
        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<GroupCreateRequest>()
            logger.info("Creating group '${request.name}' by user $userId")

            val inviteCode = generateInviteCode()
            val now = LocalDateTime.now().toString()
            val groupId = transaction {
                GroupTable.insertAndGetId {
                    it[GroupTable.name] = request.name
                    it[GroupTable.description] = request.description
                    it[GroupTable.avatar] = request.avatar
                    it[GroupTable.inviteCode] = inviteCode
                    it[GroupTable.createdBy] = userId
                    it[GroupTable.createdAt] = now
                    it[GroupTable.updatedAt] = now
                }
            }

            transaction {
                GroupMemberTable.insert {
                    it[GroupMemberTable.groupId] = groupId.value
                    it[GroupMemberTable.userId] = userId
                    it[GroupMemberTable.role] = "admin"
                    it[GroupMemberTable.createdAt] = now
                    it[GroupMemberTable.updatedAt] = now
                }
            }

            val response = GroupResponse(
                id = groupId.value,
                name = request.name,
                description = request.description,
                avatar = request.avatar,
                inviteCode = inviteCode,
                createdBy = getUsername(userId),
                members = listOf(
                    GroupMemberResponse(userId, getUsername(userId), getEmail(userId), "admin", now, now)
                ),
                createdAt = now,
                updatedAt = now
            )
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupIds = transaction {
                GroupMemberTable.select { GroupMemberTable.userId eq userId }.map { it[GroupMemberTable.groupId] }
            }
            val groups = transaction {
                GroupTable.select { GroupTable.id inList groupIds }.map { row ->
                    val gId = row[GroupTable.id].value
                    GroupResponse(
                        id = gId, name = row[GroupTable.name], description = row[GroupTable.description],
                        avatar = row[GroupTable.avatar], inviteCode = row[GroupTable.inviteCode],
                        createdBy = getUsername(row[GroupTable.createdBy]),
                        members = getGroupMembers(gId),
                        createdAt = row[GroupTable.createdAt], updatedAt = row[GroupTable.updatedAt]
                    )
                }
            }
            call.respond(groups)
        }

        post("/join") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val request = call.receive<JoinByCodeRequest>()
            val group = transaction { GroupTable.select { GroupTable.inviteCode eq request.inviteCode }.firstOrNull() }
            if (group == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invalid invite code"))
                return@post
            }
            val groupId = group[GroupTable.id].value
            val alreadyMember = transaction {
                GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) }.firstOrNull()
            } != null
            if (alreadyMember) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already a member"))
                return@post
            }
            val now = LocalDateTime.now().toString()
            transaction {
                GroupMemberTable.insert {
                    it[GroupMemberTable.groupId] = groupId
                    it[GroupMemberTable.userId] = userId
                    it[GroupMemberTable.role] = "member"
                    it[GroupMemberTable.createdAt] = now
                    it[GroupMemberTable.updatedAt] = now
                }
            }
            call.respond(
                GroupResponse(
                    id = groupId, name = group[GroupTable.name], description = group[GroupTable.description],
                    avatar = group[GroupTable.avatar], inviteCode = group[GroupTable.inviteCode],
                    createdBy = getUsername(group[GroupTable.createdBy]),
                    members = getGroupMembers(groupId),
                    createdAt = group[GroupTable.createdAt], updatedAt = group[GroupTable.updatedAt]
                )
            )
        }

        get("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val group = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull() }
            if (group == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Group not found")); return@get }
            call.respond(
                GroupResponse(
                    id = groupId, name = group[GroupTable.name], description = group[GroupTable.description],
                    avatar = group[GroupTable.avatar], inviteCode = group[GroupTable.inviteCode],
                    createdBy = getUsername(group[GroupTable.createdBy]),
                    members = getGroupMembers(groupId),
                    createdAt = group[GroupTable.createdAt], updatedAt = group[GroupTable.updatedAt]
                )
            )
        }

        put("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@put }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can edit group")); return@put }
            val request = call.receive<GroupUpdateRequest>()
            val existing = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull() }
            if (existing == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Group not found")); return@put }
            val now = LocalDateTime.now().toString()
            transaction {
                GroupTable.update({ GroupTable.id eq groupId }) { upd ->
                    request.name?.let { upd[GroupTable.name] = it }
                    upd[GroupTable.description] = request.description
                    upd[GroupTable.avatar] = request.avatar
                    upd[GroupTable.updatedAt] = now
                }
            }
            val updated = transaction { GroupTable.select { GroupTable.id eq groupId }.firstOrNull()!! }
            call.respond(
                GroupResponse(
                    id = groupId, name = updated[GroupTable.name], description = updated[GroupTable.description],
                    avatar = updated[GroupTable.avatar], inviteCode = updated[GroupTable.inviteCode],
                    createdBy = getUsername(updated[GroupTable.createdBy]),
                    members = getGroupMembers(groupId),
                    createdAt = updated[GroupTable.createdAt], updatedAt = updated[GroupTable.updatedAt]
                )
            )
        }

        delete("/{groupId}") {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@delete }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can delete group")); return@delete }
            transaction {
                MeetingParticipantTable.deleteWhere { MeetingParticipantTable.meetingId inList MeetingTable.select { MeetingTable.groupId eq groupId }.map { row -> row[MeetingTable.id].value } }
                MeetingTable.deleteWhere { MeetingTable.groupId eq groupId }
                SubtaskTable.deleteWhere { SubtaskTable.taskId inList TaskTable.select { TaskTable.groupId eq groupId }.map { row -> row[TaskTable.id].value } }
                TaskTable.deleteWhere { TaskTable.groupId eq groupId }
                GroupMemberTable.deleteWhere { GroupMemberTable.groupId eq groupId }
                GroupTable.deleteWhere { GroupTable.id eq groupId }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        // ─── Members ──────────────────────────────────────────────────────
        post("/{groupId}/members") {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can add members")); return@post }
            val request = call.receive<AddMemberRequest>()
            val existing = transaction { GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq request.userId) }.firstOrNull() }
            if (existing != null) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already a member")); return@post }
            val now = LocalDateTime.now().toString()
            transaction {
                GroupMemberTable.insert {
                    it[GroupMemberTable.groupId] = groupId; it[GroupMemberTable.userId] = request.userId
                    it[GroupMemberTable.role] = request.role; it[GroupMemberTable.createdAt] = now; it[GroupMemberTable.updatedAt] = now
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Member added"))
        }

        delete("/{groupId}/members/{memberId}") {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            val memberId = call.parameters["memberId"]?.toIntOrNull()
            if (groupId == null || memberId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@delete }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can remove members")); return@delete }
            if (userId == memberId) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot remove yourself")); return@delete }
            transaction { GroupMemberTable.deleteWhere { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq memberId) } }
            call.respond(HttpStatusCode.NoContent)
        }

        put("/{groupId}/members/{memberId}/role") {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            val memberId = call.parameters["memberId"]?.toIntOrNull()
            if (groupId == null || memberId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@put }
            if (!isAdmin(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admin can change roles")); return@put }
            val request = call.receive<UpdateRoleRequest>()
            if (request.role !in listOf("admin", "member")) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role")); return@put }
            val now = LocalDateTime.now().toString()
            transaction { GroupMemberTable.update({ (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq memberId) }) { it[GroupMemberTable.role] = request.role; it[GroupMemberTable.updatedAt] = now } }
            call.respond(mapOf("message" to "Role updated"))
        }
    }

    // ─── Tasks ────────────────────────────────────────────────────────────
    route("/groups/{groupId}/tasks") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val tasks = transaction {
                TaskTable.select { TaskTable.groupId eq groupId }.map { row ->
                    TaskResponse(
                        id = row[TaskTable.id].value, title = row[TaskTable.title],
                        description = row[TaskTable.description], status = row[TaskTable.status],
                        createdBy = row[TaskTable.createdBy], assignedTo = row[TaskTable.assignedTo],
                        createdAt = row[TaskTable.createdAt], updatedAt = row[TaskTable.updatedAt]
                    )
                }
            }
            call.respond(tasks)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<TaskCreateRequest>()
            val now = LocalDateTime.now().toString()
            val taskId = transaction {
                TaskTable.insertAndGetId {
                    it[TaskTable.groupId] = groupId; it[TaskTable.title] = request.title
                    it[TaskTable.description] = request.description; it[TaskTable.status] = "todo"
                    it[TaskTable.createdBy] = userId; it[TaskTable.assignedTo] = request.assignedTo
                    it[TaskTable.createdAt] = now; it[TaskTable.updatedAt] = now
                }
            }
            call.respond(
                TaskResponse(taskId.value, request.title, request.description, "todo", userId, request.assignedTo, now, now)
            )
        }
    }

    route("/tasks/{taskId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val taskId = call.parameters["taskId"]?.toIntOrNull()
            if (taskId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@put }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() }
            if (task == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@put }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val request = call.receive<TaskUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                TaskTable.update({ TaskTable.id eq taskId }) { upd ->
                    request.title?.let { upd[TaskTable.title] = it }
                    upd[TaskTable.description] = request.description
                    request.status?.let { upd[TaskTable.status] = it }
                    upd[TaskTable.assignedTo] = request.assignedTo
                    upd[TaskTable.updatedAt] = now
                }
            }
            val updated = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull()!! }
            call.respond(
                TaskResponse(
                    taskId, updated[TaskTable.title], updated[TaskTable.description], updated[TaskTable.status],
                    updated[TaskTable.createdBy], updated[TaskTable.assignedTo], updated[TaskTable.createdAt], updated[TaskTable.updatedAt]
                )
            )
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val taskId = call.parameters["taskId"]?.toIntOrNull()
            if (taskId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@delete }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() }
            if (task == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@delete }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction {
                SubtaskTable.deleteWhere { SubtaskTable.taskId eq taskId }
                TaskTable.deleteWhere { TaskTable.id eq taskId }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    // ─── Subtasks ─────────────────────────────────────────────────────────
    route("/tasks/{taskId}/subtasks") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val taskId = call.parameters["taskId"]?.toIntOrNull()
            if (taskId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@get }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() }
            if (task == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@get }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val subtasks = transaction {
                SubtaskTable.select { SubtaskTable.taskId eq taskId }.map { row ->
                    SubtaskResponse(row[SubtaskTable.id].value, row[SubtaskTable.title], row[SubtaskTable.isCompleted], row[SubtaskTable.createdAt], row[SubtaskTable.updatedAt])
                }
            }
            call.respond(subtasks)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val taskId = call.parameters["taskId"]?.toIntOrNull()
            if (taskId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid task ID")); return@post }
            val task = transaction { TaskTable.select { TaskTable.id eq taskId }.firstOrNull() }
            if (task == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@post }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<SubtaskCreateRequest>()
            val now = LocalDateTime.now().toString()
            val subtaskId = transaction {
                SubtaskTable.insertAndGetId {
                    it[SubtaskTable.taskId] = taskId; it[SubtaskTable.title] = request.title
                    it[SubtaskTable.isCompleted] = false; it[SubtaskTable.createdAt] = now; it[SubtaskTable.updatedAt] = now
                }
            }
            call.respond(SubtaskResponse(subtaskId.value, request.title, false, now, now))
        }
    }

    route("/subtasks/{subtaskId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val subtaskId = call.parameters["subtaskId"]?.toIntOrNull()
            if (subtaskId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid subtask ID")); return@put }
            val subtask = transaction { SubtaskTable.select { SubtaskTable.id eq subtaskId }.firstOrNull() }
            if (subtask == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subtask not found")); return@put }
            val task = transaction { TaskTable.select { TaskTable.id eq subtask[SubtaskTable.taskId] }.firstOrNull() }
            if (task == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found")); return@put }
            if (!isMember(task[TaskTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val isCompleted = call.request.queryParameters["isCompleted"]?.toBoolean()
            val now = LocalDateTime.now().toString()
            transaction {
                SubtaskTable.update({ SubtaskTable.id eq subtaskId }) { upd ->
                    isCompleted?.let { upd[SubtaskTable.isCompleted] = it }
                    upd[SubtaskTable.updatedAt] = now
                }
            }
            val updated = transaction { SubtaskTable.select { SubtaskTable.id eq subtaskId }.firstOrNull()!! }
            call.respond(SubtaskResponse(subtaskId, updated[SubtaskTable.title], updated[SubtaskTable.isCompleted], updated[SubtaskTable.createdAt], updated[SubtaskTable.updatedAt]))
        }
    }

    // ─── Meetings ─────────────────────────────────────────────────────────
    route("/groups/{groupId}/meetings") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val meetings = transaction {
                MeetingTable.select { MeetingTable.groupId eq groupId }.map { row ->
                    MeetingResponse(
                        id = row[MeetingTable.id].value, title = row[MeetingTable.title],
                        description = row[MeetingTable.description], dateTime = row[MeetingTable.dateTime],
                        location = row[MeetingTable.location], createdBy = row[MeetingTable.createdBy],
                        createdAt = row[MeetingTable.createdAt], updatedAt = row[MeetingTable.updatedAt]
                    )
                }
            }
            call.respond(meetings)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val groupId = call.parameters["groupId"]?.toIntOrNull()
            if (groupId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid group ID")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<MeetingCreateRequest>()
            val now = LocalDateTime.now().toString()
            val meetingId = transaction {
                MeetingTable.insertAndGetId {
                    it[MeetingTable.groupId] = groupId; it[MeetingTable.title] = request.title
                    it[MeetingTable.description] = request.description; it[MeetingTable.dateTime] = request.dateTime
                    it[MeetingTable.location] = request.location; it[MeetingTable.createdBy] = userId
                    it[MeetingTable.createdAt] = now; it[MeetingTable.updatedAt] = now
                }
            }
            call.respond(MeetingResponse(meetingId.value, request.title, request.description, request.dateTime, request.location, userId, now, now))
        }
    }

    route("/meetings/{meetingId}") {
        put {
            val userId = getAuthenticatedUserId(call) ?: return@put
            val meetingId = call.parameters["meetingId"]?.toIntOrNull()
            if (meetingId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@put }
            val meeting = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull() }
            if (meeting == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@put }
            if (!isMember(meeting[MeetingTable.groupId], userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@put }
            val request = call.receive<MeetingUpdateRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                MeetingTable.update({ MeetingTable.id eq meetingId }) { upd ->
                    request.title?.let { upd[MeetingTable.title] = it }
                    upd[MeetingTable.description] = request.description
                    request.dateTime?.let { upd[MeetingTable.dateTime] = it }
                    upd[MeetingTable.location] = request.location
                    upd[MeetingTable.updatedAt] = now
                }
            }
            val updated = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()!! }
            call.respond(
                MeetingResponse(
                    meetingId, updated[MeetingTable.title], updated[MeetingTable.description],
                    updated[MeetingTable.dateTime], updated[MeetingTable.location],
                    updated[MeetingTable.createdBy], updated[MeetingTable.createdAt], updated[MeetingTable.updatedAt]
                )
            )
        }

        delete {
            val userId = getAuthenticatedUserId(call) ?: return@delete
            val meetingId = call.parameters["meetingId"]?.toIntOrNull()
            if (meetingId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@delete }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) }
            if (groupId == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@delete }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction {
                MeetingParticipantTable.deleteWhere { MeetingParticipantTable.meetingId eq meetingId }
                MeetingTable.deleteWhere { MeetingTable.id eq meetingId }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/meetings/{meetingId}/participants") {
        get {
            val userId = getAuthenticatedUserId(call) ?: return@get
            val meetingId = call.parameters["meetingId"]?.toIntOrNull()
            if (meetingId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@get }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) }
            if (groupId == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@get }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@get }
            val participants = transaction {
                (MeetingParticipantTable innerJoin UserTable)
                    .select { MeetingParticipantTable.meetingId eq meetingId }
                    .map { row ->
                        MeetingParticipantResponse(
                            userId = row[UserTable.id].value,
                            username = row[UserTable.username],
                            email = row[UserTable.email]
                        )
                    }
            }
            call.respond(participants)
        }

        post {
            val userId = getAuthenticatedUserId(call) ?: return@post
            val meetingId = call.parameters["meetingId"]?.toIntOrNull()
            if (meetingId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid meeting ID")); return@post }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) }
            if (groupId == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@post }
            if (!isMember(groupId, userId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@post }
            val request = call.receive<AddParticipantRequest>()
            val now = LocalDateTime.now().toString()
            transaction {
                MeetingParticipantTable.insert {
                    it[MeetingParticipantTable.meetingId] = meetingId
                    it[MeetingParticipantTable.userId] = request.userId
                    it[MeetingParticipantTable.createdAt] = now
                    it[MeetingParticipantTable.updatedAt] = now
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Participant added"))
        }

        delete("/{userId}") {
            val currentUserId = getAuthenticatedUserId(call) ?: return@delete
            val meetingId = call.parameters["meetingId"]?.toIntOrNull()
            val targetUserId = call.parameters["userId"]?.toIntOrNull()
            if (meetingId == null || targetUserId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid IDs")); return@delete }
            val groupId = transaction { MeetingTable.select { MeetingTable.id eq meetingId }.firstOrNull()?.get(MeetingTable.groupId) }
            if (groupId == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meeting not found")); return@delete }
            if (!isMember(groupId, currentUserId)) { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member")); return@delete }
            transaction { MeetingParticipantTable.deleteWhere { (MeetingParticipantTable.meetingId eq meetingId) and (MeetingParticipantTable.userId eq targetUserId) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
suspend fun getAuthenticatedUserId(call: io.ktor.server.application.ApplicationCall): Int? {
    val principal = call.principal<io.ktor.server.auth.UserIdPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return null
    }
    val userId = principal.name.toIntOrNull()
    if (userId == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
        return null
    }
    return userId
}

private fun isMember(groupId: Int, userId: Int): Boolean {
    return transaction {
        GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) }.firstOrNull() != null
    }
}

private fun isAdmin(groupId: Int, userId: Int): Boolean {
    return transaction {
        GroupMemberTable.select { (GroupMemberTable.groupId eq groupId) and (GroupMemberTable.userId eq userId) and (GroupMemberTable.role eq "admin") }.firstOrNull() != null
    }
}

private fun getGroupMembers(groupId: Int): List<GroupMemberResponse> {
    return transaction {
        (GroupMemberTable innerJoin UserTable).select { GroupMemberTable.groupId eq groupId }.map { row ->
            GroupMemberResponse(
                userId = row[UserTable.id].value, username = row[UserTable.username],
                email = row[UserTable.email], role = row[GroupMemberTable.role],
                createdAt = row[GroupMemberTable.createdAt], updatedAt = row[GroupMemberTable.updatedAt]
            )
        }
    }
}

private fun getUsername(userId: Int): String {
    return transaction { UserTable.select { UserTable.id eq userId }.firstOrNull()?.get(UserTable.username) ?: "unknown" }
}

private fun getEmail(userId: Int): String {
    return transaction { UserTable.select { UserTable.id eq userId }.firstOrNull()?.get(UserTable.email) ?: "unknown" }
}

private fun generateInviteCode(): String {
    return UUID.randomUUID().toString().take(8).uppercase()
}
