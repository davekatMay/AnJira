package com.anjira.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object UserTable : IntIdTable("users") {
    val username: Column<String> = varchar("username", 50).uniqueIndex()
    val passwordHash: Column<String> = varchar("password_hash", 255)
    val email: Column<String> = varchar("email", 100).uniqueIndex()
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object GroupTable : IntIdTable("groups") {
    val name: Column<String> = varchar("name", 100)
    val description: Column<String?> = varchar("description", 500).nullable()
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object GroupMemberTable : IntIdTable("group_members") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val role: Column<String> = varchar("role", 20)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)

    init {
        uniqueIndex(groupId, userId)
    }
}

object TaskTable : IntIdTable("tasks") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val title: Column<String> = varchar("title", 200)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val status: Column<String> = varchar("status", 50)
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val assignedTo: Column<Int?> = integer("assigned_to").references(UserTable.id).nullable()
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object SubtaskTable : IntIdTable("subtasks") {
    val taskId: Column<Int> = integer("task_id").references(TaskTable.id)
    val title: Column<String> = varchar("title", 200)
    val isCompleted: Column<Boolean> = bool("is_completed")
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object MeetingTable : IntIdTable("meetings") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val title: Column<String> = varchar("title", 200)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val dateTime: Column<String> = varchar("date_time", 50)
    val location: Column<String?> = varchar("location", 200).nullable()
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object MeetingParticipantTable : IntIdTable("meeting_participants") {
    val meetingId: Column<Int> = integer("meeting_id").references(MeetingTable.id)
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)

    init {
        uniqueIndex(meetingId, userId)
    }
}
