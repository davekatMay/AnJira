package com.anjira.taskplanner.domain.repository

import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.model.GroupMember
import com.anjira.taskplanner.domain.model.Meeting
import com.anjira.taskplanner.domain.model.Subtask
import com.anjira.taskplanner.domain.model.Task
import com.anjira.taskplanner.domain.model.User

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

    // Task operations
    suspend fun getGroupTasks(groupId: Int): List<Task>
    suspend fun createTask(groupId: Int, title: String, description: String?, assignedTo: Int?): Task
    suspend fun updateTask(taskId: Int, title: String?, description: String?, status: String?, assignedTo: Int?): Task
    suspend fun deleteTask(taskId: Int)

    // Subtask operations
    suspend fun createSubtask(taskId: Int, title: String): Subtask
    suspend fun getTaskSubtasks(taskId: Int): List<Subtask>
    suspend fun updateSubtask(subtaskId: Int, isCompleted: Boolean): Subtask

    // Meeting operations
    suspend fun getGroupMeetings(groupId: Int): List<Meeting>
    suspend fun createMeeting(groupId: Int, title: String, description: String?, dateTime: String, location: String?): Meeting
    suspend fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, location: String?): Meeting
    suspend fun deleteMeeting(meetingId: Int)

    // Meeting participant operations
    suspend fun addMeetingParticipant(meetingId: Int, userId: Int)
    suspend fun removeMeetingParticipant(meetingId: Int, userId: Int)
    suspend fun getMeetingParticipants(meetingId: Int): List<User>
}
