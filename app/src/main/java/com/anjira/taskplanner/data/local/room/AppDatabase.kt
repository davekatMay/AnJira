package com.anjira.taskplanner.data.local.room

import androidx.room.*
import com.anjira.taskplanner.data.remote.dto.SyncResponse
import com.anjira.taskplanner.domain.model.*

@Entity(tableName = "cached_groups")
data class CachedGroup(
    @PrimaryKey val id: Int,
    val name: String, val description: String?, val avatar: String?,
    val inviteCode: String, val createdBy: String,
    val createdAt: String, val updatedAt: String
) {
    fun toGroup(members: List<GroupMember>) = Group(id, name, description, avatar, inviteCode, createdBy, members, createdAt, updatedAt)
}

@Entity(tableName = "cached_tasks")
data class CachedTask(
    @PrimaryKey val id: Int, val groupId: Int,
    val title: String, val description: String?, val deadline: String?,
    val status: String, val createdBy: Int, val assignedTo: Int?,
    val createdAt: String, val updatedAt: String
) {
    fun toTask() = Task(id, title, description, deadline, status, createdBy, assignedTo, createdAt, updatedAt)
}

@Entity(tableName = "cached_meetings")
data class CachedMeeting(
    @PrimaryKey val id: Int, val groupId: Int,
    val title: String, val description: String?, val dateTime: String,
    val endDateTime: String?, val location: String?, val createdBy: Int,
    val createdAt: String, val updatedAt: String, val myRsvp: String?
) {
    fun toMeeting() = Meeting(id, title, description, dateTime, endDateTime, location, createdBy, createdAt, updatedAt, myRsvp)
}

@Entity(tableName = "cached_announcements")
data class CachedAnnouncement(
    @PrimaryKey val id: Int, val groupId: Int, val text: String,
    val attachments: String, val isPinned: Boolean, val createdBy: Int,
    val createdByUsername: String, val createdAt: String, val updatedAt: String
) {
    fun toAnnouncement() = Announcement(id, groupId, text, attachments, isPinned, createdBy, createdByUsername, createdAt, updatedAt)
}

@Entity(tableName = "cached_playlists")
data class CachedPlaylist(
    @PrimaryKey val id: Int, val groupId: Int, val name: String,
    val type: String, val meetingId: Int?, val createdBy: Int,
    val createdByUsername: String, val createdAt: String, val updatedAt: String
) {
    fun toPlaylist() = Playlist(id, groupId, name, type, meetingId, createdBy, createdByUsername, createdAt, updatedAt)
}

@Entity(tableName = "cached_tracks")
data class CachedTrack(
    @PrimaryKey val id: Int, val playlistId: Int, val trackId: String,
    val trackName: String, val artistName: String, val trackViewUrl: String,
    val artworkUrl100: String?, val previewUrl: String?, val sortOrder: Int,
    val createdAt: String
) {
    fun toPlaylistTrack() = PlaylistTrack(id, playlistId, trackId, trackName, artistName, trackViewUrl, artworkUrl100, previewUrl, sortOrder, createdAt)
}

@Entity(tableName = "cached_members")
data class CachedMember(
    @PrimaryKey val userId: Int, val groupId: Int,
    val username: String, val email: String, val role: String,
    val createdAt: String, val updatedAt: String
) {
    fun toGroupMember() = GroupMember(userId, username, email, role, createdAt, updatedAt)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM cached_groups")
    suspend fun getAllGroups(): List<CachedGroup>

    @Query("SELECT * FROM cached_groups WHERE id = :id")
    suspend fun getGroupById(id: Int): CachedGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<CachedGroup>)

    @Query("DELETE FROM cached_groups")
    suspend fun clearGroups()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM cached_tasks WHERE groupId = :groupId")
    suspend fun getTasksForGroup(groupId: Int): List<CachedTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<CachedTask>)

    @Query("DELETE FROM cached_tasks")
    suspend fun clearTasks()
}

@Dao
interface MeetingDao {
    @Query("SELECT * FROM cached_meetings WHERE groupId = :groupId")
    suspend fun getMeetingsForGroup(groupId: Int): List<CachedMeeting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<CachedMeeting>)

    @Query("DELETE FROM cached_meetings")
    suspend fun clearMeetings()
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM cached_announcements WHERE groupId = :groupId")
    suspend fun getAnnouncementsForGroup(groupId: Int): List<CachedAnnouncement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<CachedAnnouncement>)

    @Query("DELETE FROM cached_announcements")
    suspend fun clearAnnouncements()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM cached_playlists WHERE groupId = :groupId")
    suspend fun getPlaylistsForGroup(groupId: Int): List<CachedPlaylist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<CachedPlaylist>)

    @Query("DELETE FROM cached_playlists")
    suspend fun clearPlaylists()
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM cached_tracks WHERE playlistId = :playlistId")
    suspend fun getTracksForPlaylist(playlistId: Int): List<CachedTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<CachedTrack>)

    @Query("DELETE FROM cached_tracks")
    suspend fun clearTracks()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM cached_members WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: Int): List<CachedMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CachedMember>)

    @Query("DELETE FROM cached_members")
    suspend fun clearMembers()
}

@Database(
    entities = [CachedGroup::class, CachedTask::class, CachedMeeting::class,
        CachedAnnouncement::class, CachedPlaylist::class, CachedTrack::class, CachedMember::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun taskDao(): TaskDao
    abstract fun meetingDao(): MeetingDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun trackDao(): TrackDao
    abstract fun memberDao(): MemberDao

    @Transaction
    open suspend fun replaceAll(sync: com.anjira.taskplanner.data.remote.dto.SyncResponse) {
        groupDao().clearGroups()
        taskDao().clearTasks()
        meetingDao().clearMeetings()
        announcementDao().clearAnnouncements()
        playlistDao().clearPlaylists()
        trackDao().clearTracks()
        memberDao().clearMembers()

        groupDao().insertGroups(sync.groups.map { g ->
            CachedGroup(g.id, g.name, g.description, g.avatar, g.inviteCode, g.createdBy, g.createdAt, g.updatedAt)
        })
        taskDao().insertTasks(sync.tasks.map { t ->
            CachedTask(t.id, t.groupId, t.title, t.description, t.deadline, t.status, t.createdBy, t.assignedTo, t.createdAt, t.updatedAt)
        })
        meetingDao().insertMeetings(sync.meetings.map { m ->
            CachedMeeting(m.id, m.groupId, m.title, m.description, m.dateTime, m.endDateTime, m.location, m.createdBy, m.createdAt, m.updatedAt, m.myRsvp)
        })
        announcementDao().insertAnnouncements(sync.announcements.map { a ->
            CachedAnnouncement(a.id, a.groupId, a.text, a.attachments, a.isPinned, a.createdBy, a.createdByUsername, a.createdAt, a.updatedAt)
        })
        playlistDao().insertPlaylists(sync.playlists.map { p ->
            CachedPlaylist(p.id, p.groupId, p.name, p.type, p.meetingId, p.createdBy, p.createdByUsername, p.createdAt, p.updatedAt)
        })
        trackDao().insertTracks(sync.tracks.map { t ->
            CachedTrack(t.id, t.playlistId, t.trackId, t.trackName, t.artistName, t.trackViewUrl, t.artworkUrl100, t.previewUrl, t.sortOrder, t.createdAt)
        })
        memberDao().insertMembers(sync.members.map { m ->
            CachedMember(m.userId, m.groupId, m.username, m.email, m.role, m.createdAt, m.updatedAt)
        })
    }

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "anjira_cache")
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
