package com.anjira.taskplanner.data.repository

import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.ApiService
import com.anjira.taskplanner.data.remote.dto.AddMemberRequest
import com.anjira.taskplanner.data.remote.dto.AddParticipantRequest
import com.anjira.taskplanner.data.remote.dto.GroupCreateRequest
import com.anjira.taskplanner.data.remote.dto.GroupUpdateRequest
import com.anjira.taskplanner.data.remote.dto.JoinByCodeRequest
import com.anjira.taskplanner.data.remote.dto.UpdateRoleRequest
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.model.GroupMember
import com.anjira.taskplanner.domain.model.Meeting
import com.anjira.taskplanner.domain.model.Subtask
import com.anjira.taskplanner.domain.model.Task
import com.anjira.taskplanner.domain.model.User
import com.anjira.taskplanner.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupRepositoryImpl(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : GroupRepository {

    override suspend fun createGroup(name: String, description: String?, avatar: String?): Group {
        return withContext(Dispatchers.IO) {
            val request = GroupCreateRequest(name, description, avatar)
            val response = apiService.createGroup(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to create group: ${response.errorBody()?.string()}")
            }
            response.body()?.toGroup() ?: throw Exception("Empty response body")
        }
    }

    override suspend fun getUserGroups(): List<Group> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getUserGroups().execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get groups: ${response.errorBody()?.string()}")
            }
            (response.body() ?: emptyList()).map { it.toGroup() }
        }
    }

    override suspend fun getGroupDetail(groupId: Int): Group {
        return withContext(Dispatchers.IO) {
            val response = apiService.getGroupDetail(groupId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get group: ${response.errorBody()?.string()}")
            }
            response.body()?.toGroup() ?: throw Exception("Empty response body")
        }
    }

    override suspend fun updateGroup(groupId: Int, name: String?, description: String?, avatar: String?): Group {
        return withContext(Dispatchers.IO) {
            val request = GroupUpdateRequest(name, description, avatar)
            val response = apiService.updateGroup(groupId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to update group: ${response.errorBody()?.string()}")
            }
            response.body()?.toGroup() ?: throw Exception("Empty response body")
        }
    }

    override suspend fun deleteGroup(groupId: Int) {
        withContext(Dispatchers.IO) {
            val response = apiService.deleteGroup(groupId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to delete group: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun joinGroupByCode(inviteCode: String): Group {
        return withContext(Dispatchers.IO) {
            val request = JoinByCodeRequest(inviteCode)
            val response = apiService.joinGroupByCode(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to join group: ${response.errorBody()?.string()}")
            }
            response.body()?.toGroup() ?: throw Exception("Empty response body")
        }
    }

    override suspend fun addGroupMember(groupId: Int, userId: Int, role: String) {
        withContext(Dispatchers.IO) {
            val request = AddMemberRequest(userId, role)
            val response = apiService.addGroupMember(groupId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to add member: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun removeGroupMember(groupId: Int, userId: Int) {
        withContext(Dispatchers.IO) {
            val response = apiService.removeGroupMember(groupId, userId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to remove member: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun updateMemberRole(groupId: Int, memberId: Int, role: String) {
        withContext(Dispatchers.IO) {
            val request = UpdateRoleRequest(role)
            val response = apiService.updateMemberRole(groupId, memberId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to update role: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun getGroupTasks(groupId: Int): List<Task> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getGroupTasks(groupId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get tasks: ${response.errorBody()?.string()}")
            }
            (response.body() ?: emptyList()).map { task ->
                Task(
                    task.id, task.title, task.description, task.status,
                    task.createdBy, task.assignedTo, task.createdAt, task.updatedAt
                )
            }
        }
    }

    override suspend fun createTask(groupId: Int, title: String, description: String?, assignedTo: Int?): Task {
        return withContext(Dispatchers.IO) {
            val request = com.anjira.taskplanner.data.remote.dto.TaskCreateRequest(title, description, assignedTo)
            val response = apiService.createTask(groupId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to create task: ${response.errorBody()?.string()}")
            }
            val task = response.body() ?: throw Exception("Empty response body")
            Task(task.id, task.title, task.description, task.status,
                task.createdBy, task.assignedTo, task.createdAt, task.updatedAt)
        }
    }

    override suspend fun updateTask(taskId: Int, title: String?, description: String?, status: String?, assignedTo: Int?): Task {
        return withContext(Dispatchers.IO) {
            val request = com.anjira.taskplanner.data.remote.dto.TaskUpdateRequest(title, description, status, assignedTo)
            val response = apiService.updateTask(taskId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to update task: ${response.errorBody()?.string()}")
            }
            val task = response.body() ?: throw Exception("Empty response body")
            Task(task.id, task.title, task.description, task.status,
                task.createdBy, task.assignedTo, task.createdAt, task.updatedAt)
        }
    }

    override suspend fun deleteTask(taskId: Int) {
        withContext(Dispatchers.IO) {
            val response = apiService.deleteTask(taskId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to delete task: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun createSubtask(taskId: Int, title: String): Subtask {
        return withContext(Dispatchers.IO) {
            val request = com.anjira.taskplanner.data.remote.dto.SubtaskCreateRequest(title)
            val response = apiService.createSubtask(taskId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to create subtask: ${response.errorBody()?.string()}")
            }
            val subtask = response.body() ?: throw Exception("Empty response body")
            Subtask(subtask.id, subtask.title, subtask.isCompleted, subtask.createdAt, subtask.updatedAt)
        }
    }

    override suspend fun getTaskSubtasks(taskId: Int): List<Subtask> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getTaskSubtasks(taskId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get subtasks: ${response.errorBody()?.string()}")
            }
            (response.body() ?: emptyList()).map { subtask ->
                Subtask(subtask.id, subtask.title, subtask.isCompleted, subtask.createdAt, subtask.updatedAt)
            }
        }
    }

    override suspend fun updateSubtask(subtaskId: Int, isCompleted: Boolean): Subtask {
        return withContext(Dispatchers.IO) {
            val response = apiService.updateSubtask(subtaskId, isCompleted).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to update subtask: ${response.errorBody()?.string()}")
            }
            val subtask = response.body() ?: throw Exception("Empty response body")
            Subtask(subtask.id, subtask.title, subtask.isCompleted, subtask.createdAt, subtask.updatedAt)
        }
    }

    override suspend fun getGroupMeetings(groupId: Int): List<Meeting> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getGroupMeetings(groupId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get meetings: ${response.errorBody()?.string()}")
            }
            (response.body() ?: emptyList()).map { meeting ->
                Meeting(meeting.id, meeting.title, meeting.description, meeting.dateTime,
                    meeting.location, meeting.createdBy, meeting.createdAt, meeting.updatedAt)
            }
        }
    }

    override suspend fun createMeeting(groupId: Int, title: String, description: String?, dateTime: String, location: String?): Meeting {
        return withContext(Dispatchers.IO) {
            val request = com.anjira.taskplanner.data.remote.dto.MeetingCreateRequest(title, description, dateTime, location)
            val response = apiService.createMeeting(groupId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to create meeting: ${response.errorBody()?.string()}")
            }
            val meeting = response.body() ?: throw Exception("Empty response body")
            Meeting(meeting.id, meeting.title, meeting.description, meeting.dateTime,
                meeting.location, meeting.createdBy, meeting.createdAt, meeting.updatedAt)
        }
    }

    override suspend fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, location: String?): Meeting {
        return withContext(Dispatchers.IO) {
            val request = com.anjira.taskplanner.data.remote.dto.MeetingUpdateRequest(title, description, dateTime, location)
            val response = apiService.updateMeeting(meetingId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to update meeting: ${response.errorBody()?.string()}")
            }
            val meeting = response.body() ?: throw Exception("Empty response body")
            Meeting(meeting.id, meeting.title, meeting.description, meeting.dateTime,
                meeting.location, meeting.createdBy, meeting.createdAt, meeting.updatedAt)
        }
    }

    override suspend fun deleteMeeting(meetingId: Int) {
        withContext(Dispatchers.IO) {
            val response = apiService.deleteMeeting(meetingId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to delete meeting: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun addMeetingParticipant(meetingId: Int, userId: Int) {
        withContext(Dispatchers.IO) {
            val request = AddParticipantRequest(userId)
            val response = apiService.addMeetingParticipant(meetingId, request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to add participant: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun removeMeetingParticipant(meetingId: Int, userId: Int) {
        withContext(Dispatchers.IO) {
            val response = apiService.removeMeetingParticipant(meetingId, userId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to remove participant: ${response.errorBody()?.string()}")
            }
        }
    }

    override suspend fun getMeetingParticipants(meetingId: Int): List<User> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getMeetingParticipants(meetingId).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get participants: ${response.errorBody()?.string()}")
            }
            (response.body() ?: emptyList()).map { member ->
                User(member.userId, member.username, member.email)
            }
        }
    }

    private fun com.anjira.taskplanner.data.remote.dto.GroupResponse.toGroup() = Group(
        id = id,
        name = name,
        description = description,
        avatar = avatar,
        inviteCode = inviteCode,
        createdBy = createdBy,
        members = members.map { m ->
            GroupMember(m.userId, m.username, m.email, m.role, m.createdAt, m.updatedAt)
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
