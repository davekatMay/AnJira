package com.anjira.taskplanner.data.repository

import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.anjira.taskplanner.data.remote.dto.AddMemberRequest
import com.anjira.taskplanner.data.remote.dto.AddParticipantRequest
import com.anjira.taskplanner.data.remote.dto.GroupResponse
import com.anjira.taskplanner.data.remote.dto.GroupMemberResponse
import com.anjira.taskplanner.data.remote.dto.MeetingCreateRequest
import com.anjira.taskplanner.data.remote.dto.MeetingResponse
import com.anjira.taskplanner.data.remote.dto.MeetingUpdateRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.SubtaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskCreateRequest
import com.anjira.taskplanner.data.remote.dto.TaskResponse
import com.anjira.taskplanner.data.remote.dto.TaskUpdateRequest
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.model.Subtask
import com.anjira.taskplanner.domain.model.Task
import com.anjira.taskplanner.domain.model.User
import com.anjira.taskplanner.domain.model.Meeting
import com.anjira.taskplanner.domain.repository.GroupRepository
class GroupRepositoryImpl(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : GroupRepository {
    override suspend fun createGroup(name: String, description: String?): Group = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = com.anjira.taskplanner.data.remote.dto.GroupCreateRequest(name, description)
        val response = apiService.createGroup("Bearer $token", request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create group: ${response.errorBody()?.string()}")
        }
        val groupResponse = response.body() ?: throw Exception("Empty response body")
        Group(
            groupResponse.id,
            groupResponse.name,
            groupResponse.description,
            groupResponse.createdBy,
            groupResponse.createdAt,
            groupResponse.updatedAt
        )
    }

    override suspend fun getUserGroups(): List<Group> = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.getUserGroups("Bearer $token").execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get groups: ${response.errorBody()?.string()}")
        }
        val groupResponses = response.body() ?: throw Exception("Empty response body")
        groupResponses.map { grp ->
            Group(
                grp.id,
                grp.name,
                grp.description,
                grp.createdBy,
                grp.createdAt,
                grp.updatedAt
            )
        }
    }

    override suspend fun deleteGroup(groupId: Int) = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.deleteGroup("Bearer $token", groupId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete group: ${response.errorBody()?.string()}")
        }
    }

    // Task operations
    override suspend fun getGroupTasks(groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.getGroupTasks("Bearer $token", groupId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get tasks: ${response.errorBody()?.string()}")
        }
        val taskResponses = response.body() ?: throw Exception("Empty response body")
        taskResponses.map { task ->
            Task(
                task.id,
                task.title,
                task.description,
                task.status,
                task.createdBy,
                task.assignedTo,
                task.createdAt,
                task.updatedAt
            )
        }
    }

    override suspend fun createTask(groupId: Int, title: String, description: String?, assignedTo: Int?): Task = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = TaskCreateRequest(title, description, assignedTo)
        val response = apiService.createTask("Bearer $token", groupId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create task: ${response.errorBody()?.string()}")
        }
        val taskResponse = response.body() ?: throw Exception("Empty response body")
        Task(
            taskResponse.id,
            taskResponse.title,
            taskResponse.description,
            taskResponse.status,
            taskResponse.createdBy,
            taskResponse.assignedTo,
            taskResponse.createdAt,
            taskResponse.updatedAt
        )
    }

    override suspend fun updateTask(taskId: Int, title: String?, description: String?, status: String?, assignedTo: Int?): Task = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = TaskUpdateRequest(title, description, status, assignedTo)
        val response = apiService.updateTask("Bearer $token", taskId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to update task: ${response.errorBody()?.string()}")
        }
        val taskResponse = response.body() ?: throw Exception("Empty response body")
        Task(
            taskResponse.id,
            taskResponse.title,
            taskResponse.description,
            taskResponse.status,
            taskResponse.createdBy,
            taskResponse.assignedTo,
            taskResponse.createdAt,
            taskResponse.updatedAt
        )
    }

    override suspend fun deleteTask(taskId: Int) = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.deleteTask("Bearer $token", taskId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete task: ${response.errorBody()?.string()}")
        }
    }

    // Subtask operations
    override suspend fun createSubtask(taskId: Int, title: String): Subtask = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = SubtaskCreateRequest(title)
        val response = apiService.createSubtask("Bearer $token", taskId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create subtask: ${response.errorBody()?.string()}")
        }
        val subtaskResponse = response.body() ?: throw Exception("Empty response body")
        Subtask(
            subtaskResponse.id,
            subtaskResponse.title,
            subtaskResponse.isCompleted,
            subtaskResponse.createdAt,
            subtaskResponse.updatedAt
        )
    }

    override suspend fun getTaskSubtasks(taskId: Int): List<Subtask> = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.getTaskSubtasks("Bearer $token", taskId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get subtasks: ${response.errorBody()?.string()}")
        }
        val subtaskResponses = response.body() ?: throw Exception("Empty response body")
        subtaskResponses.map { subtask ->
            Subtask(
                subtask.id,
                subtask.title,
                subtask.isCompleted,
                subtask.createdAt,
                subtask.updatedAt
            )
        }
    }

    override suspend fun updateSubtask(subtaskId: Int, isCompleted: Boolean): Subtask = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.updateSubtask("Bearer $token", subtaskId, isCompleted).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to update subtask: ${response.errorBody()?.string()}")
        }
        val subtaskResponse = response.body() ?: throw Exception("Empty response body")
        Subtask(
            subtaskResponse.id,
            subtaskResponse.title,
            subtaskResponse.isCompleted,
            subtaskResponse.createdAt,
            subtaskResponse.updatedAt
        )
    }

    // Meeting operations
    override suspend fun getGroupMeetings(groupId: Int): List<Meeting> = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.getGroupMeetings("Bearer $token", groupId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get meetings: ${response.errorBody()?.string()}")
        }
        val meetingResponses = response.body() ?: throw Exception("Empty response body")
        meetingResponses.map { meeting ->
            Meeting(
                meeting.id,
                meeting.title,
                meeting.description,
                meeting.dateTime,
                meeting.location,
                meeting.createdBy,
                meeting.createdAt,
                meeting.updatedAt
            )
        }
    }

    override suspend fun createMeeting(groupId: Int, title: String, description: String?, dateTime: String, location: String?): Meeting = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = MeetingCreateRequest(title, description, dateTime, location)
        val response = apiService.createMeeting("Bearer $token", groupId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create meeting: ${response.errorBody()?.string()}")
        }
        val meetingResponse = response.body() ?: throw Exception("Empty response body")
        Meeting(
            meetingResponse.id,
            meetingResponse.title,
            meetingResponse.description,
            meetingResponse.dateTime,
            meetingResponse.location,
            meetingResponse.createdBy,
            meetingResponse.createdAt,
            meetingResponse.updatedAt
        )
    }

    override suspend fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, location: String?): Meeting = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = MeetingUpdateRequest(title, description, dateTime, location)
        val response = apiService.updateMeeting("Bearer $token", meetingId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to update meeting: ${response.errorBody()?.string()}")
        }
        val meetingResponse = response.body() ?: throw Exception("Empty response body")
        Meeting(
            meetingResponse.id,
            meetingResponse.title,
            meetingResponse.description,
            meetingResponse.dateTime,
            meetingResponse.location,
            meetingResponse.createdBy,
            meetingResponse.createdAt,
            meetingResponse.updatedAt
        )
    }

    override suspend fun deleteMeeting(meetingId: Int) = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.deleteMeeting("Bearer $token", meetingId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete meeting: ${response.errorBody()?.string()}")
        }
    }

    // Meeting participant operations
    override suspend fun addMeetingParticipant(meetingId: Int, userId: Int) = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val request = AddParticipantRequest(userId)
        val response = apiService.addMeetingParticipant("Bearer $token", meetingId, request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to add meeting participant: ${response.errorBody()?.string()}")
        }
    }

    override suspend fun removeMeetingParticipant(meetingId: Int, userId: Int) = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.removeMeetingParticipant("Bearer $token", meetingId, userId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to remove meeting participant: ${response.errorBody()?.string()}")
        }
    }

    override suspend fun getMeetingParticipants(meetingId: Int): List<User> = withContext(Dispatchers.IO) {
        val token = dataStoreManager.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.getMeetingParticipants("Bearer $token", meetingId).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get meeting participants: ${response.errorBody()?.string()}")
        }
        val participantResponses = response.body() ?: throw Exception("Empty response body")
        participantResponses.map { participant ->
            User(
                participant.userId,
                participant.username,
                participant.email
            )
        }
    }
}