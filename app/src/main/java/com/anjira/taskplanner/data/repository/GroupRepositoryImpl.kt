package com.anjira.taskplanner.data.repository

import org.json.JSONObject
import com.anjira.taskplanner.data.local.room.AppDatabase
import com.anjira.taskplanner.data.local.room.CachedGroup
import com.anjira.taskplanner.data.local.room.CachedMember
import com.anjira.taskplanner.data.remote.ApiService
import com.anjira.taskplanner.data.remote.dto.*
import com.anjira.taskplanner.domain.model.*
import com.anjira.taskplanner.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupRepositoryImpl(
    private val apiService: ApiService,
    private val db: AppDatabase
) : GroupRepository {

    private fun <T> checkBody(r: retrofit2.Response<T>): T = r.body() ?: throw Exception("Пустой ответ сервера")

    private suspend fun <T> apiOrCache(api: suspend () -> T, cache: suspend () -> T): T {
        return try { api() } catch (e: Exception) { cache() }
    }

    override suspend fun createGroup(name: String, description: String?, avatar: String?): Group =
        withContext(Dispatchers.IO) { apiService.createGroup(GroupCreateRequest(name, description, avatar)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toGroup() } }

    override suspend fun getUserGroups(): List<Group> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getUserGroups().execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toGroup() } } } },
        cache = { withContext(Dispatchers.IO) { db.groupDao().getAllGroups().map { it.toGroup(emptyList()) } } }
    )

    override suspend fun getGroupDetail(groupId: Int): Group = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getGroupDetail(groupId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); val g = checkBody(r).toGroup(); db.groupDao().insertGroups(listOf(CachedGroup(g.id, g.name, g.description, g.avatar, g.inviteCode, g.createdBy, g.createdAt, g.updatedAt))); db.memberDao().insertMembers(g.members.map { CachedMember(it.userId, groupId, it.username, it.email, it.role, it.createdAt, it.updatedAt) }); g } } },
        cache = { withContext(Dispatchers.IO) { val g = db.groupDao().getGroupById(groupId) ?: throw Exception("Нет кэша"); val members = db.memberDao().getMembersForGroup(groupId).map { it.toGroupMember() }; g.toGroup(members) } }
    )

    override suspend fun updateGroup(groupId: Int, name: String?, description: String?, avatar: String?): Group =
        withContext(Dispatchers.IO) { apiService.updateGroup(groupId, GroupUpdateRequest(name, description, avatar)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toGroup() } }

    override suspend fun deleteGroup(groupId: Int) =
        withContext(Dispatchers.IO) { apiService.deleteGroup(groupId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun joinGroupByCode(inviteCode: String): Group =
        withContext(Dispatchers.IO) { apiService.joinGroupByCode(JoinByCodeRequest(inviteCode)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toGroup() } }

    override suspend fun addGroupMember(groupId: Int, userId: Int, role: String) =
        withContext(Dispatchers.IO) { apiService.addGroupMember(groupId, AddMemberRequest(userId, role)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun removeGroupMember(groupId: Int, userId: Int) =
        withContext(Dispatchers.IO) { apiService.removeGroupMember(groupId, userId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun updateMemberRole(groupId: Int, memberId: Int, role: String) =
        withContext(Dispatchers.IO) { apiService.updateMemberRole(groupId, memberId, UpdateRoleRequest(role)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun getGroupTasks(groupId: Int, filter: String?, status: String?): List<Task> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getGroupTasks(groupId, filter, status).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toTask() } } } },
        cache = { withContext(Dispatchers.IO) { db.taskDao().getTasksForGroup(groupId).map { it.toTask() } } }
    )

    override suspend fun createTask(groupId: Int, title: String, description: String?, deadline: String?, assignedTo: Int?): Task =
        withContext(Dispatchers.IO) { apiService.createTask(groupId, TaskCreateRequest(title, description, deadline, assignedTo)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toTask() } }

    override suspend fun updateTask(taskId: Int, title: String?, description: String?, deadline: String?, status: String?, assignedTo: Int?): Task =
        withContext(Dispatchers.IO) { apiService.updateTask(taskId, TaskUpdateRequest(title, description, deadline, status, assignedTo)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toTask() } }

    override suspend fun deleteTask(taskId: Int) =
        withContext(Dispatchers.IO) { apiService.deleteTask(taskId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun createSubtask(taskId: Int, title: String): Subtask =
        withContext(Dispatchers.IO) { apiService.createSubtask(taskId, SubtaskCreateRequest(title)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); val s = checkBody(r); Subtask(s.id, s.title, s.isCompleted, s.createdAt, s.updatedAt) } }

    override suspend fun getTaskSubtasks(taskId: Int): List<Subtask> =
        withContext(Dispatchers.IO) { apiService.getTaskSubtasks(taskId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { Subtask(it.id, it.title, it.isCompleted, it.createdAt, it.updatedAt) } } }

    override suspend fun updateSubtask(subtaskId: Int, isCompleted: Boolean): Subtask =
        withContext(Dispatchers.IO) { apiService.updateSubtask(subtaskId, isCompleted).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); val s = checkBody(r); Subtask(s.id, s.title, s.isCompleted, s.createdAt, s.updatedAt) } }

    override suspend fun getGroupMeetings(groupId: Int): List<Meeting> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getGroupMeetings(groupId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toMeeting() } } } },
        cache = { withContext(Dispatchers.IO) { db.meetingDao().getMeetingsForGroup(groupId).map { it.toMeeting() } } }
    )

    override suspend fun createMeeting(groupId: Int, title: String, description: String?, dateTime: String, endDateTime: String?, location: String?, invitedUserIds: List<Int>): Meeting =
        withContext(Dispatchers.IO) { apiService.createMeeting(groupId, MeetingCreateRequest(title, description, dateTime, endDateTime, location, invitedUserIds)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toMeeting() } }

    override suspend fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, endDateTime: String?, location: String?): Meeting =
        withContext(Dispatchers.IO) { apiService.updateMeeting(meetingId, MeetingUpdateRequest(title, description, dateTime, endDateTime, location)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toMeeting() } }

    override suspend fun deleteMeeting(meetingId: Int) =
        withContext(Dispatchers.IO) { apiService.deleteMeeting(meetingId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun addMeetingParticipant(meetingId: Int, userId: Int) =
        withContext(Dispatchers.IO) { apiService.addMeetingParticipant(meetingId, AddParticipantRequest(userId)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun removeMeetingParticipant(meetingId: Int, userId: Int) =
        withContext(Dispatchers.IO) { apiService.removeMeetingParticipant(meetingId, userId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun getMeetingParticipants(meetingId: Int): List<MeetingParticipant> =
        withContext(Dispatchers.IO) { apiService.getMeetingParticipants(meetingId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { MeetingParticipant(it.userId, it.username, it.email, it.status) } } }

    override suspend fun updateRsvp(meetingId: Int, participantId: Int, status: String) =
        withContext(Dispatchers.IO) { apiService.updateRsvp(meetingId, participantId, RsvpRequest(status)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun getAnnouncements(groupId: Int): List<Announcement> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getAnnouncements(groupId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toAnnouncement() } } } },
        cache = { withContext(Dispatchers.IO) { db.announcementDao().getAnnouncementsForGroup(groupId).map { it.toAnnouncement() } } }
    )

    override suspend fun createAnnouncement(groupId: Int, text: String, attachments: String): Announcement =
        withContext(Dispatchers.IO) { apiService.createAnnouncement(groupId, AnnouncementCreateRequest(text, attachments)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toAnnouncement() } }

    override suspend fun updateAnnouncement(announcementId: Int, text: String?, attachments: String?): Announcement =
        withContext(Dispatchers.IO) { apiService.updateAnnouncement(announcementId, AnnouncementUpdateRequest(text, attachments)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toAnnouncement() } }

    override suspend fun deleteAnnouncement(announcementId: Int) =
        withContext(Dispatchers.IO) { apiService.deleteAnnouncement(announcementId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun togglePinAnnouncement(announcementId: Int) =
        withContext(Dispatchers.IO) { apiService.togglePinAnnouncement(announcementId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun getPlaylists(groupId: Int): List<Playlist> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getPlaylists(groupId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toPlaylist() } } } },
        cache = { withContext(Dispatchers.IO) { db.playlistDao().getPlaylistsForGroup(groupId).map { it.toPlaylist() } } }
    )

    override suspend fun createPlaylist(groupId: Int, name: String, type: String, meetingId: Int?): Playlist =
        withContext(Dispatchers.IO) { apiService.createPlaylist(groupId, PlaylistCreateRequest(name, type, meetingId)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toPlaylist() } }

    override suspend fun updatePlaylist(playlistId: Int, name: String?) =
        withContext(Dispatchers.IO) { apiService.updatePlaylist(playlistId, PlaylistUpdateRequest(name)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun deletePlaylist(playlistId: Int) =
        withContext(Dispatchers.IO) { apiService.deletePlaylist(playlistId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun getTracks(playlistId: Int): List<PlaylistTrack> = apiOrCache(
        api = { withContext(Dispatchers.IO) { apiService.getTracks(playlistId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { it.toPlaylistTrack() } } } },
        cache = { withContext(Dispatchers.IO) { db.trackDao().getTracksForPlaylist(playlistId).map { it.toPlaylistTrack() } } }
    )

    override suspend fun addTrack(playlistId: Int, trackId: String, trackName: String, artistName: String, trackViewUrl: String, artworkUrl100: String?, previewUrl: String?): PlaylistTrack =
        withContext(Dispatchers.IO) { apiService.addTrack(playlistId, AddTrackRequest(trackId, trackName, artistName, trackViewUrl, artworkUrl100, previewUrl)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toPlaylistTrack() } }

    override suspend fun removeTrack(playlistId: Int, trackId: Int) =
        withContext(Dispatchers.IO) { apiService.removeTrack(playlistId, trackId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun reorderTracks(playlistId: Int, trackIds: List<Int>) =
        withContext(Dispatchers.IO) { apiService.reorderTracks(playlistId, ReorderTracksRequest(trackIds)).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    override suspend fun searchItunes(term: String): String =
        withContext(Dispatchers.IO) { apiService.searchItunes(term).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); r.body() ?: "[]" } }

    override suspend fun getItunesTrack(trackId: String): String =
        withContext(Dispatchers.IO) { apiService.getItunesTrack(trackId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); r.body() ?: "{}" } }

    override suspend fun getUserStats(): UserStats =
        withContext(Dispatchers.IO) { apiService.getUserStats().execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); checkBody(r).toUserStats() } }

    override suspend fun getNotifications(): List<AppNotification> =
        withContext(Dispatchers.IO) { apiService.getNotifications().execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())); (r.body() ?: emptyList()).map { AppNotification(it.id, it.type, it.referenceId, it.message, it.isRead, it.createdAt) } } }

    override suspend fun markNotificationRead(notificationId: Int) =
        withContext(Dispatchers.IO) { apiService.markNotificationRead(notificationId).execute().let { r -> if (!r.isSuccessful) throw Exception(parseError(r.errorBody()?.string())) } }

    private fun GroupResponse.toGroup() = Group(id, name, description, avatar, inviteCode, createdBy, members.map { GroupMember(it.userId, it.username, it.email, it.role, it.createdAt, it.updatedAt) }, createdAt, updatedAt)
    private fun TaskResponse.toTask() = Task(id, title, description, deadline, status, createdBy, assignedTo, createdAt, updatedAt)
    private fun MeetingResponse.toMeeting() = Meeting(id, title, description, dateTime, endDateTime, location, createdBy, createdAt, updatedAt, myRsvp)
    private fun AnnouncementResponse.toAnnouncement() = Announcement(id, groupId, text, attachments, isPinned, createdBy, createdByUsername, createdAt, updatedAt)
    private fun PlaylistResponse.toPlaylist() = Playlist(id, groupId, name, type, meetingId, createdBy, createdByUsername, createdAt, updatedAt)
    private fun TrackResponse.toPlaylistTrack() = PlaylistTrack(id, playlistId, trackId, trackName, artistName, trackViewUrl, artworkUrl100, previewUrl, sortOrder, createdAt)
    private fun UserStatsResponse.toUserStats() = UserStats(completedTasks, attendedMeetings)

    private fun parseError(errorBody: String?): String {
        return try { JSONObject(errorBody ?: "{}").optString("error", "Неизвестная ошибка") } catch (_: Exception) { "Неизвестная ошибка" }
    }
}
