package com.anjira.taskplanner.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anjira.taskplanner.data.remote.dto.*
import com.anjira.taskplanner.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun groupDao_insertAndRetrieve() = runTest {
        val group = CachedGroup(1, "Test Group", "Description", null, "ABC123", "creator", "2024-01-01", "2024-01-01")
        db.groupDao().insertGroups(listOf(group))

        val groups = db.groupDao().getAllGroups()
        assertEquals(1, groups.size)
        assertEquals("Test Group", groups[0].name)
        assertEquals("ABC123", groups[0].inviteCode)
    }

    @Test
    fun groupDao_insertReplacesOnConflict() = runTest {
        val original = CachedGroup(1, "Original", null, null, "XYZ", "user1", "now", "now")
        db.groupDao().insertGroups(listOf(original))

        val updated = CachedGroup(1, "Updated", "New desc", null, "XYZ", "user1", "now", "now")
        db.groupDao().insertGroups(listOf(updated))

        val groups = db.groupDao().getAllGroups()
        assertEquals(1, groups.size)
        assertEquals("Updated", groups[0].name)
        assertEquals("New desc", groups[0].description)
    }

    @Test
    fun groupDao_clear() = runTest {
        val group = CachedGroup(1, "Test", null, null, "CODE", "u", "t", "t")
        db.groupDao().insertGroups(listOf(group))
        db.groupDao().clearGroups()

        assertTrue(db.groupDao().getAllGroups().isEmpty())
    }

    @Test
    fun taskDao_insertAndRetrieve() = runTest {
        val task = CachedTask(10, 1, "Task 1", "Desc", "2024-06-01", "to_do", 1, null, "now", "now")
        db.taskDao().insertTasks(listOf(task))

        val tasks = db.taskDao().getTasksForGroup(1)
        assertEquals(1, tasks.size)
        assertEquals("Task 1", tasks[0].title)
        assertEquals("to_do", tasks[0].status)
    }

    @Test
    fun taskDao_filterByGroup() = runTest {
        val t1 = CachedTask(1, 1, "Group1 Task", null, null, "to_do", 1, null, "now", "now")
        val t2 = CachedTask(2, 2, "Group2 Task", null, null, "to_do", 1, null, "now", "now")
        db.taskDao().insertTasks(listOf(t1, t2))

        val group1Tasks = db.taskDao().getTasksForGroup(1)
        assertEquals(1, group1Tasks.size)
        assertEquals("Group1 Task", group1Tasks[0].title)
    }

    @Test
    fun taskDao_clear() = runTest {
        db.taskDao().insertTasks(listOf(CachedTask(1, 1, "T", null, null, "to_do", 1, null, "n", "n")))
        db.taskDao().clearTasks()
        assertTrue(db.taskDao().getTasksForGroup(1).isEmpty())
    }

    @Test
    fun meetingDao_insertAndRetrieve() = runTest {
        val meeting = CachedMeeting(1, 1, "Sprint Review", "Review sprint", "2024-06-01T10:00", "2024-06-01T11:00", "Room 1", 1, "now", "now", "going")
        db.meetingDao().insertMeetings(listOf(meeting))

        val meetings = db.meetingDao().getMeetingsForGroup(1)
        assertEquals(1, meetings.size)
        assertEquals("Sprint Review", meetings[0].title)
        assertEquals("going", meetings[0].myRsvp)
    }

    @Test
    fun meetingDao_clear() = runTest {
        db.meetingDao().insertMeetings(listOf(CachedMeeting(1, 1, "M", null, "t", null, null, 1, "n", "n", null)))
        db.meetingDao().clearMeetings()
        assertTrue(db.meetingDao().getMeetingsForGroup(1).isEmpty())
    }

    @Test
    fun announcementDao_insertAndRetrieve() = runTest {
        val announcement = CachedAnnouncement(1, 1, "Hello everyone!", "", false, 1, "john", "now", "now")
        db.announcementDao().insertAnnouncements(listOf(announcement))

        val announcements = db.announcementDao().getAnnouncementsForGroup(1)
        assertEquals(1, announcements.size)
        assertEquals("Hello everyone!", announcements[0].text)
        assertFalse(announcements[0].isPinned)
    }

    @Test
    fun announcementDao_clear() = runTest {
        db.announcementDao().insertAnnouncements(listOf(CachedAnnouncement(1, 1, "A", "", false, 1, "u", "n", "n")))
        db.announcementDao().clearAnnouncements()
        assertTrue(db.announcementDao().getAnnouncementsForGroup(1).isEmpty())
    }

    @Test
    fun playlistDao_insertAndRetrieve() = runTest {
        val playlist = CachedPlaylist(1, 1, "Chill Vibes", "custom", null, 1, "john", "now", "now")
        db.playlistDao().insertPlaylists(listOf(playlist))

        val playlists = db.playlistDao().getPlaylistsForGroup(1)
        assertEquals(1, playlists.size)
        assertEquals("Chill Vibes", playlists[0].name)
    }

    @Test
    fun playlistDao_clear() = runTest {
        db.playlistDao().insertPlaylists(listOf(CachedPlaylist(1, 1, "P", "custom", null, 1, "u", "n", "n")))
        db.playlistDao().clearPlaylists()
        assertTrue(db.playlistDao().getPlaylistsForGroup(1).isEmpty())
    }

    @Test
    fun trackDao_insertAndRetrieve() = runTest {
        val track = CachedTrack(1, 1, "12345", "Song Title", "Artist", "https://example.com", null, null, 1, "now")
        db.trackDao().insertTracks(listOf(track))

        val tracks = db.trackDao().getTracksForPlaylist(1)
        assertEquals(1, tracks.size)
        assertEquals("Song Title", tracks[0].trackName)
        assertEquals("12345", tracks[0].trackId)
    }

    @Test
    fun trackDao_clear() = runTest {
        db.trackDao().insertTracks(listOf(CachedTrack(1, 1, "t", "n", "a", "u", null, null, 1, "n")))
        db.trackDao().clearTracks()
        assertTrue(db.trackDao().getTracksForPlaylist(1).isEmpty())
    }

    @Test
    fun memberDao_insertAndRetrieve() = runTest {
        val member = CachedMember(1, 1, "john", "john@example.com", "admin", "now", "now")
        db.memberDao().insertMembers(listOf(member))

        val members = db.memberDao().getMembersForGroup(1)
        assertEquals(1, members.size)
        assertEquals("john", members[0].username)
        assertEquals("admin", members[0].role)
    }

    @Test
    fun memberDao_clear() = runTest {
        db.memberDao().insertMembers(listOf(CachedMember(1, 1, "u", "e", "member", "n", "n")))
        db.memberDao().clearMembers()
        assertTrue(db.memberDao().getMembersForGroup(1).isEmpty())
    }

    @Test
    fun replaceAll_storesAllEntities() = runTest {
        val member = GroupMemberResponse(1, 1, "john", "john@example.com", "admin", "now", "now")
        val sync = SyncResponse(
            groups = listOf(GroupResponse(1, "Group", null, null, "INV", "user1", listOf(member), "now", "now")),
            tasks = listOf(TaskResponse(1, 1, "Task", null, null, "to_do", 1, null, "now", "now")),
            meetings = listOf(MeetingResponse(1, 1, "Meeting", null, "2024-01-01T10:00", null, null, 1, "now", "now", null)),
            announcements = listOf(AnnouncementResponse(1, 1, "Text", "", false, 1, "john", "now", "now")),
            playlists = listOf(PlaylistResponse(1, 1, "Playlist", "custom", null, 1, "john", "now", "now")),
            tracks = listOf(TrackResponse(1, 1, "tid", "Song", "Artist", "url", null, null, 1, "now")),
            members = listOf(member)
        )

        db.replaceAll(sync)

        assertEquals(1, db.groupDao().getAllGroups().size)
        assertEquals(1, db.taskDao().getTasksForGroup(1).size)
        assertEquals(1, db.meetingDao().getMeetingsForGroup(1).size)
        assertEquals(1, db.announcementDao().getAnnouncementsForGroup(1).size)
        assertEquals(1, db.playlistDao().getPlaylistsForGroup(1).size)
        assertEquals(1, db.trackDao().getTracksForPlaylist(1).size)
        assertEquals(1, db.memberDao().getMembersForGroup(1).size)
    }

    @Test
    fun replaceAll_clearsOldData() = runTest {
        db.groupDao().insertGroups(listOf(CachedGroup(99, "Old Group", null, null, "OLD", "u", "t", "t")))
        db.taskDao().insertTasks(listOf(CachedTask(99, 99, "Old Task", null, null, "to_do", 1, null, "n", "n")))

        val sync = SyncResponse(
            groups = listOf(GroupResponse(1, "New Group", null, null, "NEW", "u", emptyList(), "t", "t")),
            tasks = listOf(TaskResponse(1, 1, "New Task", null, null, "to_do", 1, null, "n", "n")),
            meetings = emptyList(),
            announcements = emptyList(),
            playlists = emptyList(),
            tracks = emptyList(),
            members = emptyList()
        )

        db.replaceAll(sync)

        assertEquals(1, db.groupDao().getAllGroups().size)
        assertEquals("New Group", db.groupDao().getAllGroups()[0].name)
        assertEquals(1, db.taskDao().getTasksForGroup(1).size)
        assertEquals("New Task", db.taskDao().getTasksForGroup(1)[0].title)
    }

    @Test
    fun cachedGroup_toGroup_mapsCorrectly() {
        val cached = CachedGroup(1, "G", "D", "avatar", "CODE", "creator", "2024-01-01", "2024-01-02")
        val members = listOf(GroupMember(1, "john", "j@e.com", "admin", "t", "t"))
        val group = cached.toGroup(members)
        assertEquals(1, group.id)
        assertEquals("G", group.name)
        assertEquals(1, group.members.size)
    }

    @Test
    fun cachedTask_toTask_mapsCorrectly() {
        val cached = CachedTask(1, 1, "Title", "Desc", "2024-06-01", "done", 1, 2, "now", "now")
        val task = cached.toTask()
        assertEquals("Title", task.title)
        assertEquals("done", task.status)
        assertEquals(2, task.assignedTo)
    }

    @Test
    fun cachedMeeting_toMeeting_mapsCorrectly() {
        val cached = CachedMeeting(1, 1, "M", "D", "2024-01-01T10:00", "2024-01-01T11:00", "Loc", 1, "n", "n", "going")
        val meeting = cached.toMeeting()
        assertEquals("M", meeting.title)
        assertEquals("going", meeting.myRsvp)
    }

    @Test
    fun cachedAnnouncement_toAnnouncement_mapsCorrectly() {
        val cached = CachedAnnouncement(1, 1, "Text", "url1,url2", true, 1, "john", "n", "n")
        val announcement = cached.toAnnouncement()
        assertTrue(announcement.isPinned)
        assertEquals("url1,url2", announcement.attachments)
    }

    @Test
    fun cachedPlaylist_toPlaylist_mapsCorrectly() {
        val cached = CachedPlaylist(1, 1, "P", "meeting", 5, 1, "john", "n", "n")
        val playlist = cached.toPlaylist()
        assertEquals("meeting", playlist.type)
        assertEquals(5, playlist.meetingId)
    }

    @Test
    fun cachedTrack_toPlaylistTrack_mapsCorrectly() {
        val cached = CachedTrack(1, 1, "tid", "Song", "Artist", "url", "artUrl", "previewUrl", 1, "now")
        val track = cached.toPlaylistTrack()
        assertEquals("Song", track.trackName)
        assertEquals("artUrl", track.artworkUrl100)
        assertEquals("previewUrl", track.previewUrl)
    }

    @Test
    fun cachedMember_toGroupMember_mapsCorrectly() {
        val cached = CachedMember(1, 1, "john", "j@e.com", "admin", "t", "t")
        val member = cached.toGroupMember()
        assertEquals("john", member.username)
        assertEquals("admin", member.role)
    }
}
