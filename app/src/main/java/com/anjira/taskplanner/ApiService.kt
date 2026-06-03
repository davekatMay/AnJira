package com.anjira.taskplanner.data.remote

import com.anjira.taskplanner.data.remote.dto.AuthResponse
import com.anjira.taskplanner.data.remote.dto.AddMemberRequest
import com.anjira.taskplanner.data.remote.dto.AddParticipantRequest
import com.anjira.taskplanner.data.remote.dto.GroupCreateRequest
import com.anjira.taskplanner.data.remote.dto.GroupResponse
import com.anjira.taskplanner.data.remote.dto.GroupMemberResponse
import com.anjira.taskplanner.data.remote.dto.GroupUpdateRequest
import com.anjira.taskplanner.data.remote.dto.JoinByCodeRequest
import com.anjira.taskplanner.data.remote.dto.MeetingCreateRequest
import com.anjira.taskplanner.data.remote.dto.MeetingResponse
import com.anjira.taskplanner.data.remote.dto.MeetingUpdateRequest
import com.anjira.taskplanner.data.remote.dto.RefreshRequest
import com.anjira.taskplanner.data.remote.dto.RegisterRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.TaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskUpdateRequest
import com.anjira.taskplanner.data.remote.dto.UpdateRoleRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/login")
    fun login(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshRequest): Call<AuthResponse>

    // Group endpoints
    @POST("groups")
    fun createGroup(@Body request: GroupCreateRequest): Call<GroupResponse>

    @GET("groups")
    fun getUserGroups(): Call<List<GroupResponse>>

    @GET("groups/{groupId}")
    fun getGroupDetail(@Path("groupId") groupId: Int): Call<GroupResponse>

    @PUT("groups/{groupId}")
    fun updateGroup(
        @Path("groupId") groupId: Int,
        @Body request: GroupUpdateRequest
    ): Call<GroupResponse>

    @DELETE("groups/{groupId}")
    fun deleteGroup(@Path("groupId") groupId: Int): Call<Void>

    @POST("groups/join")
    fun joinGroupByCode(@Body request: JoinByCodeRequest): Call<GroupResponse>

    @POST("groups/{groupId}/members")
    fun addGroupMember(
        @Path("groupId") groupId: Int,
        @Body request: AddMemberRequest
    ): Call<Void>

    @DELETE("groups/{groupId}/members/{userId}")
    fun removeGroupMember(
        @Path("groupId") groupId: Int,
        @Path("userId") userId: Int
    ): Call<Void>

    @PUT("groups/{groupId}/members/{memberId}/role")
    fun updateMemberRole(
        @Path("groupId") groupId: Int,
        @Path("memberId") memberId: Int,
        @Body request: UpdateRoleRequest
    ): Call<Map<String, String>>

    // Task endpoints
    @GET("groups/{groupId}/tasks")
    fun getGroupTasks(@Path("groupId") groupId: Int): Call<List<TaskResponse>>

    @POST("groups/{groupId}/tasks")
    fun createTask(
        @Path("groupId") groupId: Int,
        @Body request: TaskCreateRequest
    ): Call<TaskResponse>

    @PUT("tasks/{taskId}")
    fun updateTask(
        @Path("taskId") taskId: Int,
        @Body request: TaskUpdateRequest
    ): Call<TaskResponse>

    @DELETE("tasks/{taskId}")
    fun deleteTask(@Path("taskId") taskId: Int): Call<Void>

    // Subtask endpoints
    @POST("tasks/{taskId}/subtasks")
    fun createSubtask(
        @Path("taskId") taskId: Int,
        @Body request: SubtaskCreateRequest
    ): Call<SubtaskResponse>

    @GET("tasks/{taskId}/subtasks")
    fun getTaskSubtasks(@Path("taskId") taskId: Int): Call<List<SubtaskResponse>>

    @PUT("subtasks/{subtaskId}")
    fun updateSubtask(
        @Path("subtaskId") subtaskId: Int,
        isCompleted: Boolean
    ): Call<SubtaskResponse>

    // Meeting endpoints
    @GET("groups/{groupId}/meetings")
    fun getGroupMeetings(@Path("groupId") groupId: Int): Call<List<MeetingResponse>>

    @POST("groups/{groupId}/meetings")
    fun createMeeting(
        @Path("groupId") groupId: Int,
        @Body request: MeetingCreateRequest
    ): Call<MeetingResponse>

    @PUT("meetings/{meetingId}")
    fun updateMeeting(
        @Path("meetingId") meetingId: Int,
        @Body request: MeetingUpdateRequest
    ): Call<MeetingResponse>

    @DELETE("meetings/{meetingId}")
    fun deleteMeeting(@Path("meetingId") meetingId: Int): Call<Void>

    @POST("meetings/{meetingId}/participants")
    fun addMeetingParticipant(
        @Path("meetingId") meetingId: Int,
        @Body request: AddParticipantRequest
    ): Call<Void>

    @DELETE("meetings/{meetingId}/participants/{userId}")
    fun removeMeetingParticipant(
        @Path("meetingId") meetingId: Int,
        @Path("userId") userId: Int
    ): Call<Void>

    @GET("meetings/{meetingId}/participants")
    fun getMeetingParticipants(@Path("meetingId") meetingId: Int): Call<List<GroupMemberResponse>>
}
