package com.anjira.routes

import com.anjira.db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class SyncResponse(
    val groups: List<GroupResponse> = emptyList(),
    val tasks: List<TaskResponse> = emptyList(),
    val meetings: List<MeetingResponse> = emptyList(),
    val announcements: List<AnnouncementResponse> = emptyList(),
    val playlists: List<PlaylistResponse> = emptyList(),
    val tracks: List<TrackResponse> = emptyList(),
    val members: List<GroupMemberResponse> = emptyList(),
    val notifications: List<NotificationResponse> = emptyList(),
    val serverTime: String
)

fun Route.SyncRoutes() {
    get("/sync") {
        val userId = getAuthenticatedUserId(call) ?: return@get
        val sinceStr = call.request.queryParameters["since"]
        val syncTime = if (sinceStr != null) {
            try { LocalDateTime.parse(sinceStr) } catch (_: java.time.format.DateTimeParseException) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'since' date format. Use ISO-8601")); return@get }
        } else LocalDateTime.MIN

        val serverTime = LocalDateTime.now().toString()

        val userGroupIds = transaction {
            GroupMemberTable.select { GroupMemberTable.userId eq userId }
                .map { it[GroupMemberTable.groupId] }
        }

        val allEntityIds = userGroupIds.map { EntityID(it, GroupTable) }

        val groups = transaction {
            GroupTable.select {
                (GroupTable.id inList allEntityIds) and (GroupTable.updatedAt greaterEq syncTime.toString())
            }.map { it.toGroupResponse() }
        }

        val tasks = transaction {
            TaskTable.select {
                (TaskTable.groupId inList userGroupIds) and (TaskTable.updatedAt greaterEq syncTime.toString())
            }.orderBy(TaskTable.createdAt, SortOrder.DESC)
                .map { it.toTaskResponse() }
        }

        val meetings = transaction {
            MeetingTable.select {
                (MeetingTable.groupId inList userGroupIds) and (MeetingTable.updatedAt greaterEq syncTime.toString())
            }.orderBy(MeetingTable.dateTime, SortOrder.ASC)
                .map { row ->
                    val mid = row[MeetingTable.id].value
                    val rsvp = MeetingParticipantTable.select {
                        (MeetingParticipantTable.meetingId eq mid) and (MeetingParticipantTable.userId eq userId)
                    }.firstOrNull()?.get(MeetingParticipantTable.status)
                    row.toMeetingResponse(rsvp)
                }
        }

        val announcements = transaction {
            (AnnouncementTable innerJoin UserTable).select {
                (AnnouncementTable.groupId inList userGroupIds) and (AnnouncementTable.updatedAt greaterEq syncTime.toString())
            }.orderBy(AnnouncementTable.createdAt, SortOrder.DESC)
                .map { row ->
                    val gId = row[AnnouncementTable.groupId]
                    AnnouncementResponse(
                        id = row[AnnouncementTable.id].value, groupId = gId,
                        text = row[AnnouncementTable.text], attachments = row[AnnouncementTable.attachments],
                        isPinned = row[AnnouncementTable.isPinned], createdBy = row[AnnouncementTable.createdBy],
                        createdByUsername = row[UserTable.username],
                        createdAt = row[AnnouncementTable.createdAt], updatedAt = row[AnnouncementTable.updatedAt]
                    )
                }
        }

        val playlists = transaction {
            (PlaylistTable innerJoin UserTable).select {
                (PlaylistTable.groupId inList userGroupIds) and (PlaylistTable.updatedAt greaterEq syncTime.toString())
            }.orderBy(PlaylistTable.createdAt, SortOrder.DESC)
                .map { row ->
                    PlaylistResponse(row[PlaylistTable.id].value, row[PlaylistTable.groupId], row[PlaylistTable.name],
                        row[PlaylistTable.type], row[PlaylistTable.meetingId],
                        row[PlaylistTable.createdBy], row[UserTable.username],
                        row[PlaylistTable.createdAt], row[PlaylistTable.updatedAt])
                }
        }

        val playlistIds = playlists.map { it.id }
        val tracks = if (playlistIds.isEmpty()) emptyList() else transaction {
            PlaylistTrackTable.select {
                PlaylistTrackTable.playlistId inList playlistIds
            }.orderBy(PlaylistTrackTable.sortOrder, SortOrder.ASC)
                .map { row ->
                    TrackResponse(row[PlaylistTrackTable.id].value, row[PlaylistTrackTable.playlistId],
                        row[PlaylistTrackTable.trackId], row[PlaylistTrackTable.trackName],
                        row[PlaylistTrackTable.artistName], row[PlaylistTrackTable.trackViewUrl],
                        row[PlaylistTrackTable.artworkUrl100], row[PlaylistTrackTable.previewUrl],
                        row[PlaylistTrackTable.sortOrder], row[PlaylistTrackTable.createdAt])
                }
        }

        val members = transaction {
            (GroupMemberTable innerJoin UserTable).select {
                (GroupMemberTable.groupId inList userGroupIds) and (GroupMemberTable.updatedAt greaterEq syncTime.toString())
            }.map { row ->
                GroupMemberResponse(row[UserTable.id].value, row[GroupMemberTable.groupId],
                    row[UserTable.username], row[UserTable.email], row[GroupMemberTable.role],
                    row[GroupMemberTable.createdAt], row[GroupMemberTable.updatedAt])
            }
        }

        val notifications = transaction {
            NotificationTable.select {
                (NotificationTable.userId eq userId) and (NotificationTable.createdAt greaterEq syncTime.toString())
            }.orderBy(NotificationTable.createdAt, SortOrder.DESC)
                .limit(50)
                .map { row ->
                    NotificationResponse(row[NotificationTable.id].value, row[NotificationTable.type],
                        row[NotificationTable.referenceId], row[NotificationTable.message],
                        row[NotificationTable.isRead], row[NotificationTable.createdAt])
                }
        }

        call.respond(SyncResponse(
            groups = groups, tasks = tasks, meetings = meetings,
            announcements = announcements, playlists = playlists,
            tracks = tracks, members = members,
            notifications = notifications, serverTime = serverTime
        ))
    }
}
