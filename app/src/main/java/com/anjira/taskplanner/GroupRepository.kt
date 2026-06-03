package com.anjira.taskplanner.domain.repository

import com.anjira.taskplanner.domain.model.*

interface GroupRepository {
    suspend fun createGroup(name: String, description: String?, avatar: String?): Group
    suspend fun getUserGroups(): List<Group>
    suspend fun getGroupDetail(groupId: Int): Group
    suspend fun updateGroup(groupId: Int, name: String?, description: String?, avatar: String?): Group
    suspend fun deleteGroup(groupId: Int)
    suspend fun joinGroupByCode(inviteCode: String): Group
    suspend fun addGroupMember(groupId: Int, userId: Int, role: String)
    suspend fun removeGroupMember(groupId: Int, userId: Int)
    suspend fun updateMemberRole(groupId: Int, memberId: Int, role: String)

    // Tasks
    suspend fun getGroupTasks(groupId: Int, filter: String? = null, status: String? = null): List<Task>
    suspend fun createTask(groupId: Int, title: String, description: String?, deadline: String?, assignedTo: Int?): Task
    suspend fun updateTask(taskId: Int, title: String?, description: String?, deadline: String?, status: String?, assignedTo: Int?): Task
    suspend fun deleteTask(taskId: Int)

    // Subtasks
    suspend fun createSubtask(taskId: Int, title: String): Subtask
    suspend fun getTaskSubtasks(taskId: Int): List<Subtask>
    suspend fun updateSubtask(subtaskId: Int, isCompleted: Boolean): Subtask

    // Meetings
    suspend fun getGroupMeetings(groupId: Int): List<Meeting>
    suspend fun createMeeting(groupId: Int, title: String, description: String?, dateTime: String, endDateTime: String?, location: String?): Meeting
    suspend fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, endDateTime: String?, location: String?): Meeting
    suspend fun deleteMeeting(meetingId: Int)
    suspend fun addMeetingParticipant(meetingId: Int, userId: Int)
    suspend fun removeMeetingParticipant(meetingId: Int, userId: Int)
    suspend fun getMeetingParticipants(meetingId: Int): List<MeetingParticipant>
    suspend fun updateRsvp(meetingId: Int, participantId: Int, status: String)

    // Announcements
    suspend fun getAnnouncements(groupId: Int): List<Announcement>
    suspend fun createAnnouncement(groupId: Int, text: String, attachments: String): Announcement
    suspend fun updateAnnouncement(announcementId: Int, text: String?, attachments: String?): Announcement
    suspend fun deleteAnnouncement(announcementId: Int)
    suspend fun togglePinAnnouncement(announcementId: Int)

    // Playlists
    suspend fun getPlaylists(groupId: Int): List<Playlist>
    suspend fun createPlaylist(groupId: Int, name: String, type: String, meetingId: Int?): Playlist
    suspend fun updatePlaylist(playlistId: Int, name: String?)
    suspend fun deletePlaylist(playlistId: Int)

    // Tracks
    suspend fun getTracks(playlistId: Int): List<PlaylistTrack>
    suspend fun addTrack(playlistId: Int, trackId: String, trackName: String, artistName: String, trackViewUrl: String, artworkUrl100: String?, previewUrl: String?): PlaylistTrack
    suspend fun removeTrack(playlistId: Int, trackId: Int)
    suspend fun reorderTracks(playlistId: Int, trackIds: List<Int>)

    // iTunes
    suspend fun searchItunes(term: String): String

    // Notifications
    suspend fun getNotifications(): List<AppNotification>
    suspend fun markNotificationRead(notificationId: Int)
}
