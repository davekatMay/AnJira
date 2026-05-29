package com.anjira.taskplanner.data.remote

import com.anjira.taskplanner.data.remote.dto.AuthResponse
import com.anjira.taskplanner.data.remote.dto.AddMemberRequest
import com.anjira.taskplanner.data.remote.dto.AddParticipantRequest
import com.anjira.taskplanner.data.remote.dto.GroupCreateRequest
import com.anjira.taskplanner.data.remote.dto.GroupResponse
import com.anjira.taskplanner.data.remote.dto.GroupMemberResponse
import com.anjira.taskplanner.data.remote.dto.MeetingCreateRequest
import com.anjira.taskplanner.data.remote.dto.MeetingResponse
import com.anjira.taskplanner.data.remote.dto.MeetingUpdateRequest
import com.anjira.taskplanner.data.remote.dto.RegisterRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.TaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskUpdateRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/login")
    fun login(@Body request: RegisterRequest): Call<AuthResponse>

    // Group endpoints
    @POST("groups")
    fun createGroup(
        @Header("Authorization") token: String,
        @Body request: GroupCreateRequest
    ): Call<GroupResponse>

    @GET("groups")
    fun getUserGroups(
        @Header("Authorization") token: String
    ): Call<List<GroupResponse>>

    @DELETE("groups/{groupId}")
    fun deleteGroup(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Call<Void>

    @GET("groups/{groupId}/members")
    fun getGroupMembers(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Call<List<GroupMemberResponse>>

    @POST("groups/{groupId}/members")
    fun addGroupMember(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Body request: AddMemberRequest
    ): Call<Void>

    @DELETE("groups/{groupId}/members/{userId}")
    fun removeGroupMember(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Path("userId") userId: Int
    ): Call<Void>

    // Task endpoints
    @GET("groups/{groupId}/tasks")
    fun getGroupTasks(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Call<List<TaskResponse>>

    @POST("groups/{groupId}/tasks")
    fun createTask(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Body request: TaskCreateRequest
    ): Call<TaskResponse>

    @PUT("tasks/{taskId}")
    fun updateTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Int,
        @Body request: TaskUpdateRequest
    ): Call<TaskResponse>

    @DELETE("tasks/{taskId}")
    fun deleteTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Int
    ): Call<Void>

    // Subtask endpoints
    @POST("tasks/{taskId}/subtasks")
    fun createSubtask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Int,
        @Body request: SubtaskCreateRequest
    ): Call<SubtaskResponse>

    @GET("tasks/{taskId}/subtasks")
    fun getTaskSubtasks(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Int
    ): Call<List<SubtaskResponse>>

    @PUT("subtasks/{subtaskId}")
    fun updateSubtask(
        @Header("Authorization") token: String,
        @Path("subtaskId") subtaskId: Int,
        @Query("isCompleted") isCompleted: Boolean
    ): Call<SubtaskResponse>

    // Meeting endpoints
    @GET("groups/{groupId}/meetings")
    fun getGroupMeetings(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Call<List<MeetingResponse>>

    @POST("groups/{groupId}/meetings")
    fun createMeeting(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Body request: MeetingCreateRequest
    ): Call<MeetingResponse>

    @PUT("meetings/{meetingId}")
    fun updateMeeting(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: Int,
        @Body request: MeetingUpdateRequest
    ): Call<MeetingResponse>

    @DELETE("meetings/{meetingId}")
    fun deleteMeeting(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: Int
    ): Call<Void>

    @POST("meetings/{meetingId}/participants")
    fun addMeetingParticipant(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: Int,
        @Body request: AddParticipantRequest
    ): Call<Void>

    @DELETE("meetings/{meetingId}/participants/{userId}")
    fun removeMeetingParticipant(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: Int,
        @Path("userId") userId: Int
    ): Call<Void>

    @GET("meetings/{meetingId}/participants")
    fun getMeetingParticipants(
        @Header("Authorization") token: String,
        @Path("meetingId") meetingId: Int
    ): Call<List<GroupMemberResponse>>
}