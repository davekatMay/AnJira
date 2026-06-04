package com.anjira.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object UserTable : IntIdTable("users") {
    val username: Column<String> = varchar("username", 50).uniqueIndex()
    val passwordHash: Column<String> = varchar("password_hash", 255)
    val email: Column<String> = varchar("email", 100).uniqueIndex()
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object RefreshTokenTable : Table("refresh_tokens") {
    val id = integer("id").autoIncrement()
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val token: Column<String> = varchar("token", 512)
    val expiresAt: Column<String> = varchar("expires_at", 50)
    val createdAt: Column<String> = varchar("created_at", 50)
    override val primaryKey = PrimaryKey(id)
}

object GroupTable : IntIdTable("groups") {
    val name: Column<String> = varchar("name", 100)
    val description: Column<String?> = varchar("description", 500).nullable()
    val avatar: Column<String?> = varchar("avatar", 500).nullable()
    val inviteCode: Column<String> = varchar("invite_code", 20).uniqueIndex()
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
    init { uniqueIndex(groupId, userId) }
}

object TaskTable : IntIdTable("tasks") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val title: Column<String> = varchar("title", 200)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val deadline: Column<String?> = varchar("deadline", 50).nullable()
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
    val endDateTime: Column<String?> = varchar("end_date_time", 50).nullable()
    val location: Column<String?> = varchar("location", 200).nullable()
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object MeetingParticipantTable : IntIdTable("meeting_participants") {
    val meetingId: Column<Int> = integer("meeting_id").references(MeetingTable.id)
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val status: Column<String> = varchar("status", 20).default("pending") // going, maybe, declined, pending
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
    init { uniqueIndex(meetingId, userId) }
}

object AnnouncementTable : IntIdTable("announcements") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val text: Column<String> = text("text")
    val attachments: Column<String> = text("attachments").default("[]")
    val isPinned: Column<Boolean> = bool("is_pinned").default(false)
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object PlaylistTable : IntIdTable("playlists") {
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val name: Column<String> = varchar("name", 200)
    val type: Column<String> = varchar("type", 20).default("group") // "group" or "meeting"
    val meetingId: Column<Int?> = integer("meeting_id").references(MeetingTable.id).nullable()
    val createdBy: Column<Int> = integer("created_by").references(UserTable.id)
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
}

object PlaylistTrackTable : IntIdTable("playlist_tracks") {
    val playlistId: Column<Int> = integer("playlist_id").references(PlaylistTable.id)
    val trackId: Column<String> = varchar("track_id", 50)
    val trackName: Column<String> = varchar("track_name", 300)
    val artistName: Column<String> = varchar("artist_name", 300)
    val trackViewUrl: Column<String> = varchar("track_view_url", 500)
    val artworkUrl100: Column<String?> = varchar("artwork_url", 500).nullable()
    val previewUrl: Column<String?> = varchar("preview_url", 500).nullable()
    val sortOrder: Column<Int> = integer("sort_order")
    val createdAt: Column<String> = varchar("created_at", 50)
}

object NotificationTable : IntIdTable("notifications") {
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val type: Column<String> = varchar("type", 50)
    val referenceId: Column<Int> = integer("reference_id")
    val message: Column<String> = varchar("message", 500)
    val isRead: Column<Boolean> = bool("is_read").default(false)
    val createdAt: Column<String> = varchar("created_at", 50)
}

object FcmTokenTable : Table("fcm_tokens") {
    val id = integer("id").autoIncrement()
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val token: Column<String> = varchar("token", 512)
    val deviceName: Column<String?> = varchar("device_name", 100).nullable()
    val createdAt: Column<String> = varchar("created_at", 50)
    val updatedAt: Column<String> = varchar("updated_at", 50)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, token) }
}

object NotificationPreferenceTable : Table("notification_preferences") {
    val id = integer("id").autoIncrement()
    val userId: Column<Int> = integer("user_id").references(UserTable.id)
    val groupId: Column<Int> = integer("group_id").references(GroupTable.id)
    val taskAssigned: Column<Boolean> = bool("task_assigned").default(true)
    val taskStatusChanged: Column<Boolean> = bool("task_status_changed").default(true)
    val meetingCreated: Column<Boolean> = bool("meeting_created").default(true)
    val meetingReminder: Column<Boolean> = bool("meeting_reminder").default(true)
    val announcementPosted: Column<Boolean> = bool("announcement_posted").default(true)
    val groupInvite: Column<Boolean> = bool("group_invite").default(true)
    val meetingReminderMinutes: Column<Int> = integer("meeting_reminder_minutes").default(30)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, groupId) }
}


