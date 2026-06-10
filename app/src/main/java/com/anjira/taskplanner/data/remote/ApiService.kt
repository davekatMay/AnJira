package com.anjira.taskplanner.data.remote

import com.anjira.taskplanner.data.remote.dto.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshRequest): Call<AuthResponse>

    // Groups
    @POST("groups")
    fun createGroup(@Body request: GroupCreateRequest): Call<GroupResponse>

    @GET("groups")
    fun getUserGroups(): Call<List<GroupResponse>>

    @GET("groups/{groupId}")
    fun getGroupDetail(@Path("groupId") groupId: Int): Call<GroupResponse>

    @PUT("groups/{groupId}")
    fun updateGroup(@Path("groupId") groupId: Int, @Body request: GroupUpdateRequest): Call<GroupResponse>

    @DELETE("groups/{groupId}")
    fun deleteGroup(@Path("groupId") groupId: Int): Call<Void>

    @POST("groups/join")
    fun joinGroupByCode(@Body request: JoinByCodeRequest): Call<GroupResponse>

    @POST("groups/{groupId}/members")
    fun addGroupMember(@Path("groupId") groupId: Int, @Body request: AddMemberRequest): Call<Void>

    @DELETE("groups/{groupId}/members/{memberId}")
    fun removeGroupMember(@Path("groupId") groupId: Int, @Path("memberId") memberId: Int): Call<Void>

    @PUT("groups/{groupId}/members/{memberId}/role")
    fun updateMemberRole(@Path("groupId") groupId: Int, @Path("memberId") memberId: Int, @Body request: UpdateRoleRequest): Call<Map<String, String>>

    // Tasks
    @GET("groups/{groupId}/tasks")
    fun getGroupTasks(@Path("groupId") groupId: Int, @Query("filter") filter: String? = null, @Query("status") status: String? = null): Call<List<TaskResponse>>

    @POST("groups/{groupId}/tasks")
    fun createTask(@Path("groupId") groupId: Int, @Body request: TaskCreateRequest): Call<TaskResponse>

    @PUT("tasks/{taskId}")
    fun updateTask(@Path("taskId") taskId: Int, @Body request: TaskUpdateRequest): Call<TaskResponse>

    @DELETE("tasks/{taskId}")
    fun deleteTask(@Path("taskId") taskId: Int): Call<Void>

    // Subtasks
    @POST("tasks/{taskId}/subtasks")
    fun createSubtask(@Path("taskId") taskId: Int, @Body request: SubtaskCreateRequest): Call<SubtaskResponse>

    @GET("tasks/{taskId}/subtasks")
    fun getTaskSubtasks(@Path("taskId") taskId: Int): Call<List<SubtaskResponse>>

    @PUT("subtasks/{subtaskId}")
    fun updateSubtask(@Path("subtaskId") subtaskId: Int, @Query("isCompleted") isCompleted: Boolean): Call<SubtaskResponse>

    // Meetings
    @GET("groups/{groupId}/meetings")
    fun getGroupMeetings(@Path("groupId") groupId: Int): Call<List<MeetingResponse>>

    @POST("groups/{groupId}/meetings")
    fun createMeeting(@Path("groupId") groupId: Int, @Body request: MeetingCreateRequest): Call<MeetingResponse>

    @PUT("meetings/{meetingId}")
    fun updateMeeting(@Path("meetingId") meetingId: Int, @Body request: MeetingUpdateRequest): Call<MeetingResponse>

    @DELETE("meetings/{meetingId}")
    fun deleteMeeting(@Path("meetingId") meetingId: Int): Call<Void>

    // Meeting participants / RSVP
    @POST("meetings/{meetingId}/participants")
    fun addMeetingParticipant(@Path("meetingId") meetingId: Int, @Body request: AddParticipantRequest): Call<Void>

    @DELETE("meetings/{meetingId}/participants/{userId}")
    fun removeMeetingParticipant(@Path("meetingId") meetingId: Int, @Path("userId") userId: Int): Call<Void>

    @GET("meetings/{meetingId}/participants")
    fun getMeetingParticipants(@Path("meetingId") meetingId: Int): Call<List<MeetingParticipantResponse>>

    @PUT("meetings/{meetingId}/participants/{participantId}/rsvp")
    fun updateRsvp(@Path("meetingId") meetingId: Int, @Path("participantId") participantId: Int, @Body request: RsvpRequest): Call<Map<String, String>>

    // Announcements
    @GET("groups/{groupId}/announcements")
    fun getAnnouncements(@Path("groupId") groupId: Int): Call<List<AnnouncementResponse>>

    @POST("groups/{groupId}/announcements")
    fun createAnnouncement(@Path("groupId") groupId: Int, @Body request: AnnouncementCreateRequest): Call<AnnouncementResponse>

    @PUT("announcements/{announcementId}")
    fun updateAnnouncement(@Path("announcementId") announcementId: Int, @Body request: AnnouncementUpdateRequest): Call<AnnouncementResponse>

    @DELETE("announcements/{announcementId}")
    fun deleteAnnouncement(@Path("announcementId") announcementId: Int): Call<Void>

    @PUT("announcements/{announcementId}/pin")
    fun togglePinAnnouncement(@Path("announcementId") announcementId: Int): Call<Map<String, String>>

    // Playlists
    @GET("groups/{groupId}/playlists")
    fun getPlaylists(@Path("groupId") groupId: Int): Call<List<PlaylistResponse>>

    @POST("groups/{groupId}/playlists")
    fun createPlaylist(@Path("groupId") groupId: Int, @Body request: PlaylistCreateRequest): Call<PlaylistResponse>

    @PUT("playlists/{playlistId}")
    fun updatePlaylist(@Path("playlistId") playlistId: Int, @Body request: PlaylistUpdateRequest): Call<Map<String, String>>

    @DELETE("playlists/{playlistId}")
    fun deletePlaylist(@Path("playlistId") playlistId: Int): Call<Void>

    // Tracks
    @GET("playlists/{playlistId}/tracks")
    fun getTracks(@Path("playlistId") playlistId: Int): Call<List<TrackResponse>>

    @POST("playlists/{playlistId}/tracks")
    fun addTrack(@Path("playlistId") playlistId: Int, @Body request: AddTrackRequest): Call<TrackResponse>

    @DELETE("playlists/{playlistId}/tracks/{trackId}")
    fun removeTrack(@Path("playlistId") playlistId: Int, @Path("trackId") trackId: Int): Call<Void>

    @PUT("playlists/{playlistId}/tracks/reorder")
    fun reorderTracks(@Path("playlistId") playlistId: Int, @Body request: ReorderTracksRequest): Call<Map<String, String>>

    // iTunes Search
    @GET("itunes/search")
    fun searchItunes(@Query("term") term: String, @Query("limit") limit: Int = 20): Call<String>

    @GET("itunes/track/{trackId}")
    fun getItunesTrack(@Path("trackId") trackId: String): Call<String>

    // Notifications
    @GET("notifications")
    fun getNotifications(): Call<List<NotificationResponse>>

    @PUT("notifications/{notificationId}/read")
    fun markNotificationRead(@Path("notificationId") notificationId: Int): Call<Map<String, String>>

    // FCM
    @POST("notifications/fcm/register")
    fun registerFcmToken(@Body request: FcmRegisterRequest): Call<Void>

    @POST("notifications/fcm/unregister")
    fun unregisterFcmToken(@Body request: FcmUnregisterRequest): Call<Void>

    // Notification preferences
    @GET("notifications/preferences/{groupId}")
    fun getNotificationPreferences(@Path("groupId") groupId: Int): Call<NotificationPreferencesResponse>

    @PUT("notifications/preferences/{groupId}")
    fun updateNotificationPreferences(@Path("groupId") groupId: Int, @Body request: NotificationPreferencesRequest): Call<Map<String, String>>

    // User profile
    @GET("users/me")
    fun getUserProfile(): Call<UserProfileResponse>

    @GET("users/{userId}")
    fun getUserProfileById(@Path("userId") userId: Int): Call<UserProfileResponse>

    @PUT("users/me")
    fun updateUserProfile(@Body body: Map<String, String?>): Call<Map<String, Any?>>

    // User Stats
    @GET("users/me/stats")
    fun getUserStats(): Call<UserStatsResponse>

    // Sync
    @GET("sync")
    fun syncData(@Query("since") since: String? = null): Call<SyncResponse>
}
