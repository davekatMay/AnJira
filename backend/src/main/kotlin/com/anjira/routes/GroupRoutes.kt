package com.anjira.routes

import com.anjira.db.GroupMemberTable
import com.anjira.db.GroupTable
import com.anjira.db.MeetingParticipantTable
import com.anjira.db.MeetingTable
import com.anjira.db.SubtaskTable
import com.anjira.db.TaskTable
import com.anjira.db.UserTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("GroupRoutes")
data class GroupCreateRequest(val name: String, val description: String? = null)
data class GroupResponse(
    val id: Int, val name: String, val description: String?,
    val createdBy: Int, val createdAt: String, val updatedAt: String
)
data class GroupMemberResponse(val userId: Int, val username: String, val email: String, val role: String)
data class AddMemberRequest(val userId: Int, val role: String = "member")
data class TaskCreateRequest(val title: String, val description: String? = null, val assignedTo: Int? = null)
data class TaskResponse(
    val id: Int, val title: String, val description: String?, val status: String,
    val createdBy: Int, val assignedTo: Int?, val createdAt: String, val updatedAt: String
)
data class TaskUpdateRequest(val title: String? = null, val description: String? = null, val status: String? = null, val assignedTo: Int? = null)
data class SubtaskCreateRequest(val title: String)
data class SubtaskResponse(val id: Int, val title: String, val isCompleted: Boolean, val createdAt: String, val updatedAt: String)
data class MeetingCreateRequest(val title: String, val description: String? = null, val dateTime: String, val location: String? = null)
data class MeetingResponse(
    val id: Int, val title: String, val description: String?, val dateTime: String,
    val location: String?, val createdBy: Int, val createdAt: String, val updatedAt: String
)
data class MeetingUpdateRequest(val title: String? = null, val description: String? = null, val dateTime: String? = null, val location: String? = null)
data class AddParticipantRequest(val userId: Int)

fun Route.GroupRoute() {
    authenticate("jwt") {
        route("/groups") {
            post {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val request = call.receive<GroupCreateRequest>()
                val gid = transaction {
                    GroupTable.insertAndGetId {
                        it[GroupTable.name] = request.name
                        it[GroupTable.description] = request.description
                        it[GroupTable.createdBy] = uid
                        it[GroupTable.createdAt] = LocalDateTime.now().toString()
                        it[GroupTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                transaction {
                    GroupMemberTable.insertAndGetId {
                        it[GroupMemberTable.groupId] = gid.value
                        it[GroupMemberTable.userId] = uid
                        it[GroupMemberTable.role] = "admin"
                        it[GroupMemberTable.createdAt] = LocalDateTime.now().toString()
                        it[GroupMemberTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                call.respond(GroupResponse(gid.value, request.name, request.description, uid, LocalDateTime.now().toString(), LocalDateTime.now().toString()))
            }

            get {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val groups = transaction {
                    GroupMemberTable.select(GroupMemberTable.userId eq uid).map { membership ->
                        val gidVal = membership[GroupMemberTable.groupId]
                        val gRow = GroupTable.select(GroupTable.id eq EntityID(gidVal, GroupTable)).first()
                        GroupResponse(gRow[GroupTable.id].value, gRow[GroupTable.name], gRow[GroupTable.description], gRow[GroupTable.createdBy], gRow[GroupTable.createdAt], gRow[GroupTable.updatedAt])
                    }
                }
                call.respond(groups)
            }

            get("/{groupId}") {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val gid = call.parameters["groupId"]!!.toInt()
                val isMember = transaction {
                    GroupMemberTable.select((GroupMemberTable.groupId eq gid) and (GroupMemberTable.userId eq uid)).firstOrNull() != null
                }
                if (!isMember) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member"))
                    return@get
                }
                val row = transaction { GroupTable.select(GroupTable.id eq EntityID(gid, GroupTable)).first() }
                call.respond(GroupResponse(row[GroupTable.id].value, row[GroupTable.name], row[GroupTable.description], row[GroupTable.createdBy], row[GroupTable.createdAt], row[GroupTable.updatedAt]))
            }

            delete("/{groupId}") {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val gid = call.parameters["groupId"]!!.toInt()
                val row = transaction { GroupTable.select(GroupTable.id eq EntityID(gid, GroupTable)).firstOrNull() }
                if (row == null || row[GroupTable.createdBy] != uid) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@delete
                }
                transaction {
                    GroupMemberTable.deleteWhere { GroupMemberTable.groupId eq gid }
                    GroupTable.deleteWhere { GroupTable.id eq EntityID(gid, GroupTable) }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/{groupId}/members") {
                val gid = call.parameters["groupId"]!!.toInt()
                val members = transaction {
                    GroupMemberTable.select(GroupMemberTable.groupId eq gid).map { row ->
                        val uidVal = row[GroupMemberTable.userId]
                        val uRow = UserTable.select(UserTable.id eq EntityID(uidVal, UserTable)).firstOrNull()
                        GroupMemberResponse(uidVal, uRow?.get(UserTable.username) ?: "", uRow?.get(UserTable.email) ?: "", row[GroupMemberTable.role])
                    }
                }
                call.respond(members)
            }

            post("/{groupId}/members") {
                val gid = call.parameters["groupId"]!!.toInt()
                val request = call.receive<AddMemberRequest>()
                transaction {
                    GroupMemberTable.insertAndGetId {
                        it[GroupMemberTable.groupId] = gid
                        it[GroupMemberTable.userId] = request.userId
                        it[GroupMemberTable.role] = request.role
                        it[GroupMemberTable.createdAt] = LocalDateTime.now().toString()
                        it[GroupMemberTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                call.respond(HttpStatusCode.Created)
            }

            delete("/{groupId}/members/{userId}") {
                val gid = call.parameters["groupId"]!!.toInt()
                val uidParam = call.parameters["userId"]!!.toInt()
                transaction { GroupMemberTable.deleteWhere { (GroupMemberTable.groupId eq gid) and (GroupMemberTable.userId eq uidParam) } }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/{groupId}/tasks") {
                val gid = call.parameters["groupId"]!!.toInt()
                val tasks = transaction {
                    TaskTable.select(TaskTable.groupId eq gid).map { row ->
                        TaskResponse(row[TaskTable.id].value, row[TaskTable.title], row[TaskTable.description], row[TaskTable.status], row[TaskTable.createdBy], row[TaskTable.assignedTo], row[TaskTable.createdAt], row[TaskTable.updatedAt])
                    }
                }
                call.respond(tasks)
            }

            post("/{groupId}/tasks") {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val gid = call.parameters["groupId"]!!.toInt()
                val request = call.receive<TaskCreateRequest>()
                val newId = transaction {
                    TaskTable.insertAndGetId {
                        it[TaskTable.groupId] = gid
                        it[TaskTable.title] = request.title
                        it[TaskTable.description] = request.description
                        it[TaskTable.status] = "todo"
                        it[TaskTable.createdBy] = uid
                        it[TaskTable.assignedTo] = request.assignedTo
                        it[TaskTable.createdAt] = LocalDateTime.now().toString()
                        it[TaskTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                val row = transaction { TaskTable.select(TaskTable.id eq newId).first() }
                call.respond(TaskResponse(row[TaskTable.id].value, row[TaskTable.title], row[TaskTable.description], row[TaskTable.status], row[TaskTable.createdBy], row[TaskTable.assignedTo], row[TaskTable.createdAt], row[TaskTable.updatedAt]))
            }

            put("/tasks/{taskId}") {
                val taskId = call.parameters["taskId"]!!.toInt()
                val request = call.receive<TaskUpdateRequest>()
                val row = transaction { TaskTable.select(TaskTable.id eq taskId).firstOrNull() }
                if (row == null) { call.respond(HttpStatusCode.NotFound); return@put }
                transaction {
                    TaskTable.update(where = { TaskTable.id eq taskId }) { stmt ->
                        request.title?.let { stmt[TaskTable.title] = it }
                        request.description?.let { stmt[TaskTable.description] = it }
                        request.status?.let { stmt[TaskTable.status] = it }
                        request.assignedTo?.let { stmt[TaskTable.assignedTo] = it }
                    }
                }
                val updated = transaction { TaskTable.select(TaskTable.id eq taskId).first() }
                call.respond(TaskResponse(updated[TaskTable.id].value, updated[TaskTable.title], updated[TaskTable.description], updated[TaskTable.status], updated[TaskTable.createdBy], updated[TaskTable.assignedTo], updated[TaskTable.createdAt], updated[TaskTable.updatedAt]))
            }

            delete("/tasks/{taskId}") {
                val taskId = call.parameters["taskId"]!!.toInt()
                transaction { SubtaskTable.deleteWhere { SubtaskTable.taskId eq taskId }; TaskTable.deleteWhere { TaskTable.id eq taskId } }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/tasks/{taskId}/subtasks") {
                val taskId = call.parameters["taskId"]!!.toInt()
                val request = call.receive<SubtaskCreateRequest>()
                val newId = transaction {
                    SubtaskTable.insertAndGetId {
                        it[SubtaskTable.taskId] = taskId
                        it[SubtaskTable.title] = request.title
                        it[SubtaskTable.isCompleted] = false
                        it[SubtaskTable.createdAt] = LocalDateTime.now().toString()
                        it[SubtaskTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                val row = transaction { SubtaskTable.select(SubtaskTable.id eq newId).first() }
                call.respond(SubtaskResponse(row[SubtaskTable.id].value, row[SubtaskTable.title], row[SubtaskTable.isCompleted], row[SubtaskTable.createdAt], row[SubtaskTable.updatedAt]))
            }

            get("/tasks/{taskId}/subtasks") {
                val taskId = call.parameters["taskId"]!!.toInt()
                val subtasks = transaction {
                    SubtaskTable.select(SubtaskTable.taskId eq taskId).map { row ->
                        SubtaskResponse(row[SubtaskTable.id].value, row[SubtaskTable.title], row[SubtaskTable.isCompleted], row[SubtaskTable.createdAt], row[SubtaskTable.updatedAt])
                    }
                }
                call.respond(subtasks)
            }

            put("/subtasks/{subtaskId}") {
                val subtaskId = call.parameters["subtaskId"]!!.toInt()
                val isCompleted = call.parameters["isCompleted"]!!.toBoolean()
                transaction { SubtaskTable.update(where = { SubtaskTable.id eq subtaskId }) { stmt -> stmt[SubtaskTable.isCompleted] = isCompleted } }
                val row = transaction { SubtaskTable.select(SubtaskTable.id eq subtaskId).first() }
                call.respond(SubtaskResponse(row[SubtaskTable.id].value, row[SubtaskTable.title], row[SubtaskTable.isCompleted], row[SubtaskTable.createdAt], row[SubtaskTable.updatedAt]))
            }

            get("/{groupId}/meetings") {
                val gid = call.parameters["groupId"]!!.toInt()
                val meetings = transaction {
                    MeetingTable.select(MeetingTable.groupId eq gid).map { row ->
                        MeetingResponse(row[MeetingTable.id].value, row[MeetingTable.title], row[MeetingTable.description], row[MeetingTable.dateTime], row[MeetingTable.location], row[MeetingTable.createdBy], row[MeetingTable.createdAt], row[MeetingTable.updatedAt])
                    }
                }
                call.respond(meetings)
            }

            post("/{groupId}/meetings") {
                val uid = call.principal<UserIdPrincipal>()?.name!!.toInt()
                val gid = call.parameters["groupId"]!!.toInt()
                val request = call.receive<MeetingCreateRequest>()
                val newId = transaction {
                    MeetingTable.insertAndGetId {
                        it[MeetingTable.groupId] = gid
                        it[MeetingTable.title] = request.title
                        it[MeetingTable.description] = request.description
                        it[MeetingTable.dateTime] = request.dateTime
                        it[MeetingTable.location] = request.location
                        it[MeetingTable.createdBy] = uid
                        it[MeetingTable.createdAt] = LocalDateTime.now().toString()
                        it[MeetingTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                transaction {
                    MeetingParticipantTable.insertAndGetId {
                        it[MeetingParticipantTable.meetingId] = newId.value
                        it[MeetingParticipantTable.userId] = uid
                        it[MeetingParticipantTable.createdAt] = LocalDateTime.now().toString()
                        it[MeetingParticipantTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                val row = transaction { MeetingTable.select(MeetingTable.id eq newId).first() }
                call.respond(MeetingResponse(row[MeetingTable.id].value, row[MeetingTable.title], row[MeetingTable.description], row[MeetingTable.dateTime], row[MeetingTable.location], row[MeetingTable.createdBy], row[MeetingTable.createdAt], row[MeetingTable.updatedAt]))
            }

            put("/meetings/{meetingId}") {
                val meetingId = call.parameters["meetingId"]!!.toInt()
                val request = call.receive<MeetingUpdateRequest>()
                transaction {
                    MeetingTable.update(where = { MeetingTable.id eq meetingId }) { stmt ->
                        request.title?.let { stmt[MeetingTable.title] = it }
                        request.description?.let { stmt[MeetingTable.description] = it }
                        request.dateTime?.let { stmt[MeetingTable.dateTime] = it }
                        request.location?.let { stmt[MeetingTable.location] = it }
                    }
                }
                val row = transaction { MeetingTable.select(MeetingTable.id eq meetingId).first() }
                call.respond(MeetingResponse(row[MeetingTable.id].value, row[MeetingTable.title], row[MeetingTable.description], row[MeetingTable.dateTime], row[MeetingTable.location], row[MeetingTable.createdBy], row[MeetingTable.createdAt], row[MeetingTable.updatedAt]))
            }

            delete("/meetings/{meetingId}") {
                val meetingId = call.parameters["meetingId"]!!.toInt()
                transaction { MeetingParticipantTable.deleteWhere { MeetingParticipantTable.meetingId eq meetingId }; MeetingTable.deleteWhere { MeetingTable.id eq meetingId } }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/meetings/{meetingId}/participants") {
                val meetingId = call.parameters["meetingId"]!!.toInt()
                val request = call.receive<AddParticipantRequest>()
                transaction {
                    MeetingParticipantTable.insertAndGetId {
                        it[MeetingParticipantTable.meetingId] = meetingId
                        it[MeetingParticipantTable.userId] = request.userId
                        it[MeetingParticipantTable.createdAt] = LocalDateTime.now().toString()
                        it[MeetingParticipantTable.updatedAt] = LocalDateTime.now().toString()
                    }
                }
                call.respond(HttpStatusCode.Created)
            }

            delete("/meetings/{meetingId}/participants/{userId}") {
                val meetingId = call.parameters["meetingId"]!!.toInt()
                val uidParam = call.parameters["userId"]!!.toInt()
                transaction { MeetingParticipantTable.deleteWhere { (MeetingParticipantTable.meetingId eq meetingId) and (MeetingParticipantTable.userId eq uidParam) } }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/meetings/{meetingId}/participants") {
                val meetingId = call.parameters["meetingId"]!!.toInt()
                val participants = transaction {
                    MeetingParticipantTable.select(MeetingParticipantTable.meetingId eq meetingId).map { row ->
                        val uidVal = row[MeetingParticipantTable.userId]
                        val uRow = UserTable.select(UserTable.id eq EntityID(uidVal, UserTable)).firstOrNull()
                        GroupMemberResponse(uidVal, uRow?.get(UserTable.username) ?: "", uRow?.get(UserTable.email) ?: "", "")
                    }
                }
                call.respond(participants)
            }
        }
    }
}
