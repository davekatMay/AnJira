package com.anjira.taskplanner.data.remote.dto

data class AddMemberRequest(val userId: Int, val role: String = "member")
data class RefreshRequest(val refreshToken: String)
data class GroupUpdateRequest(val name: String? = null, val description: String? = null, val avatar: String? = null)
data class JoinByCodeRequest(val inviteCode: String)
data class UpdateRoleRequest(val role: String)
data class RsvpRequest(val status: String)
data class ReorderTracksRequest(val trackIds: List<Int>)

data class AnnouncementCreateRequest(val text: String, val attachments: String = "[]")
data class AnnouncementUpdateRequest(val text: String? = null, val attachments: String? = null)
data class AnnouncementResponse(val id: Int, val groupId: Int, val text: String, val attachments: String, val isPinned: Boolean, val createdBy: Int, val createdByUsername: String, val createdAt: String, val updatedAt: String)

data class PlaylistCreateRequest(val name: String, val type: String = "group", val meetingId: Int? = null)
data class PlaylistUpdateRequest(val name: String? = null)
data class PlaylistResponse(val id: Int, val groupId: Int, val name: String, val type: String, val meetingId: Int?, val createdBy: Int, val createdByUsername: String, val createdAt: String, val updatedAt: String)

data class AddTrackRequest(val trackId: String, val trackName: String, val artistName: String, val trackViewUrl: String, val artworkUrl100: String? = null, val previewUrl: String? = null)
data class TrackResponse(val id: Int, val playlistId: Int, val trackId: String, val trackName: String, val artistName: String, val trackViewUrl: String, val artworkUrl100: String?, val previewUrl: String?, val sortOrder: Int, val createdAt: String)

data class NotificationResponse(val id: Int, val type: String, val referenceId: Int, val message: String, val isRead: Boolean, val createdAt: String)
