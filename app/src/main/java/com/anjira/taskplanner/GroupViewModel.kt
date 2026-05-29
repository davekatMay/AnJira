package com.anjira.taskplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.model.Meeting
import com.anjira.taskplanner.domain.model.Subtask
import com.anjira.taskplanner.domain.model.Task
import com.anjira.taskplanner.domain.model.User
import com.anjira.taskplanner.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _meetings = MutableStateFlow<List<Meeting>>(emptyList())
    val meetings: StateFlow<List<Meeting>> = _meetings.asStateFlow()

    init {
        loadGroupData()
    }

    private fun loadGroupData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val tasks = groupRepository.getGroupTasks(groupId)
                val meetings = groupRepository.getGroupMeetings(groupId)
                _tasks.value = tasks
                _meetings.value = meetings
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load group data")
            }
        }
    }

    fun createTask(title: String, description: String?, assignedTo: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val task = groupRepository.createTask(groupId, title, description, assignedTo)
                val currentTasks = _tasks.value
                _tasks.value = currentTasks + task
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create task")
            }
        }
    }

    fun updateTask(taskId: Int, title: String?, description: String?, status: String?, assignedTo: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val updatedTask = groupRepository.updateTask(taskId, title, description, status, assignedTo)
                val currentTasks = _tasks.value
                val index = currentTasks.indexOfFirst { it.id == taskId }
                if (index != -1) {
                    val updatedTasks = currentTasks.toMutableList()
                    updatedTasks[index] = updatedTask
                    _tasks.value = updatedTasks
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                groupRepository.deleteTask(taskId)
                val currentTasks = _tasks.value
                _tasks.value = currentTasks.filter { it.id != taskId }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete task")
            }
        }
    }

    fun createSubtask(taskId: Int, title: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val subtask = groupRepository.createSubtask(taskId, title)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create subtask")
            }
        }
    }

    fun updateSubtask(subtaskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val updatedSubtask = groupRepository.updateSubtask(subtaskId, isCompleted)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update subtask")
            }
        }
    }

    fun createMeeting(title: String, description: String?, dateTime: String, location: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val meeting = groupRepository.createMeeting(groupId, title, description, dateTime, location)
                val currentMeetings = _meetings.value
                _meetings.value = currentMeetings + meeting
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create meeting")
            }
        }
    }

    fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, location: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val updatedMeeting = groupRepository.updateMeeting(meetingId, title, description, dateTime, location)
                val currentMeetings = _meetings.value
                val index = currentMeetings.indexOfFirst { it.id == meetingId }
                if (index != -1) {
                    val updatedMeetings = currentMeetings.toMutableList()
                    updatedMeetings[index] = updatedMeeting
                    _meetings.value = updatedMeetings
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update meeting")
            }
        }
    }

    fun deleteMeeting(meetingId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                groupRepository.deleteMeeting(meetingId)
                val currentMeetings = _meetings.value
                _meetings.value = currentMeetings.filter { it.id != meetingId }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete meeting")
            }
        }
    }

    fun addMeetingParticipant(meetingId: Int, userId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                groupRepository.addMeetingParticipant(meetingId, userId)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to add meeting participant")
            }
        }
    }

    fun removeMeetingParticipant(meetingId: Int, userId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                groupRepository.removeMeetingParticipant(meetingId, userId)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to remove meeting participant")
            }
        }
    }

    fun getMeetingParticipants(meetingId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                groupRepository.getMeetingParticipants(meetingId)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to get meeting participants")
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}

class GroupViewModelFactory(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GroupViewModel(groupRepository, groupId) as T
    }
}
