package com.anjira.taskplanner.domain.model

data class GroupMember(
    val userId: Int,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String
)

data class Group(
    val id: Int,
    val name: String,
    val description: String?,
    val avatar: String?,
    val inviteCode: String,
    val createdBy: String,
    val members: List<GroupMember>,
    val createdAt: String,
    val updatedAt: String
)

data class Announcement(
    val id: Int,
    val groupId: Int,
    val text: String,
    val attachments: String,
    val isPinned: Boolean,
    val createdBy: Int,
    val createdByUsername: String,
    val createdAt: String,
    val updatedAt: String
)

data class Playlist(
    val id: Int,
    val groupId: Int,
    val name: String,
    val type: String,
    val meetingId: Int?,
    val createdBy: Int,
    val createdByUsername: String,
    val createdAt: String,
    val updatedAt: String
)

data class PlaylistTrack(
    val id: Int,
    val playlistId: Int,
    val trackId: String,
    val trackName: String,
    val artistName: String,
    val trackViewUrl: String,
    val artworkUrl100: String?,
    val previewUrl: String?,
    val sortOrder: Int,
    val createdAt: String
)

data class AppNotification(
    val id: Int,
    val type: String,
    val referenceId: Int,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
