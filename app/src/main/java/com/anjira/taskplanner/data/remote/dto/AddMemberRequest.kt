package com.anjira.taskplanner.data.remote.dto

data class AddMemberRequest(val userId: Int, val role: String = "member")
data class RefreshRequest(val refreshToken: String)
data class GroupUpdateRequest(val name: String? = null, val description: String? = null, val avatar: String? = null)
data class JoinByCodeRequest(val inviteCode: String)
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
data class SyncResponse(
    val groups: List<GroupResponse> = emptyList(),
    val tasks: List<TaskResponse> = emptyList(),
    val meetings: List<MeetingResponse> = emptyList(),
    val announcements: List<AnnouncementResponse> = emptyList(),
    val playlists: List<PlaylistResponse> = emptyList(),
    val tracks: List<TrackResponse> = emptyList(),
    val members: List<GroupMemberResponse> = emptyList(),
    val notifications: List<NotificationResponse> = emptyList(),
    val serverTime: String = ""
)
data class UpdateRoleRequest(val role: String)
data class RsvpRequest(val status: String)
data class ReorderTracksRequest(val trackIds: List<Int>)

data class AnnouncementCreateRequest(val text: String, val attachments: String = "[]")
data class AnnouncementUpdateRequest(val text: String? = null, val attachments: String? = null)
data class AnnouncementResponse(val id: Int, val groupId: Int, val text: String, val attachments: String, val isPinned: Boolean, val createdBy: Int, val createdByUsername: String, val createdAt: String, val updatedAt: String)

data class PlaylistCreateRequest(val name: String, val type: String = "group", val meetingId: Int? = null)
data class PlaylistUpdateRequest(val name: String? = null)

data class AddTrackRequest(val trackId: String, val trackName: String, val artistName: String, val trackViewUrl: String, val artworkUrl100: String? = null, val previewUrl: String? = null)
data class UserStatsResponse(val completedTasks: Int, val attendedMeetings: Int)
