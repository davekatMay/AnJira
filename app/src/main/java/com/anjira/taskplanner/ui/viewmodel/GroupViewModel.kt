package com.anjira.taskplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anjira.taskplanner.domain.model.*
import com.anjira.taskplanner.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val groupId: Int,
    val currentUserId: Int = -1
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _meetings = MutableStateFlow<List<Meeting>>(emptyList())
    val meetings: StateFlow<List<Meeting>> = _meetings.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _tracks = MutableStateFlow<Map<Int, List<PlaylistTrack>>>(emptyMap())
    val tracks: StateFlow<Map<Int, List<PlaylistTrack>>> = _tracks.asStateFlow()

    private val _itunesResults = MutableStateFlow<String>("[]")
    val itunesResults: StateFlow<String> = _itunesResults.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init { loadGroupData() }

    fun loadGroupData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _group.value = groupRepository.getGroupDetail(groupId)
                _tasks.value = groupRepository.getGroupTasks(groupId)
                _meetings.value = groupRepository.getGroupMeetings(groupId)
                _announcements.value = groupRepository.getAnnouncements(groupId)
                _playlists.value = groupRepository.getPlaylists(groupId)
                _notifications.value = groupRepository.getNotifications()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Не удалось загрузить данные")
            }
        }
    }

    fun loadTracks(playlistId: Int) {
        viewModelScope.launch {
            try {
                val t = groupRepository.getTracks(playlistId)
                _tracks.value = _tracks.value + (playlistId to t)
            } catch (_: Exception) {}
        }
    }

    fun updateGroup(groupId: Int, name: String?, description: String?, avatar: String?) {
        viewModelScope.launch {
            try {
                groupRepository.updateGroup(groupId, name, description, avatar)
                loadGroupData()
            } catch (_: Exception) {}
        }
    }

    // ─── Tasks ────────────────────────────────────────────────────────────
    fun loadTasks(filter: String? = null, status: String? = null) {
        viewModelScope.launch {
            try { _tasks.value = groupRepository.getGroupTasks(groupId, filter, status) } catch (_: Exception) {}
        }
    }

    fun createTask(title: String, description: String?, deadline: String?, assignedTo: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val t = groupRepository.createTask(groupId, title, description, deadline, assignedTo)
                _tasks.value = _tasks.value + t
                _uiState.value = UiState.Success
            } catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Ошибка") }
        }
    }

    fun updateTask(taskId: Int, title: String?, description: String?, deadline: String?, status: String?, assignedTo: Int?) {
        viewModelScope.launch {
            try {
                val ut = groupRepository.updateTask(taskId, title, description, deadline, status, assignedTo)
                _tasks.value = _tasks.value.map { if (it.id == taskId) ut else it }
            } catch (_: Exception) {}
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try { groupRepository.deleteTask(taskId); _tasks.value = _tasks.value.filter { it.id != taskId } } catch (_: Exception) {}
        }
    }

    fun toggleTaskStatus(taskId: Int, currentStatus: String) {
        val newStatus = when (currentStatus) {
            "to_do" -> "in_progress"
            "in_progress" -> "done"
            else -> "to_do"
        }
        updateTask(taskId, null, null, null, newStatus, null)
    }

    // ─── Meetings ─────────────────────────────────────────────────────────
    fun createMeeting(title: String, description: String?, dateTime: String, endDateTime: String?, location: String?, invitedUserIds: List<Int> = emptyList()) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val m = groupRepository.createMeeting(groupId, title, description, dateTime, endDateTime, location, invitedUserIds)
                _meetings.value = _meetings.value + m
                _uiState.value = UiState.Success
            } catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Ошибка") }
        }
    }

    fun updateMeeting(meetingId: Int, title: String?, description: String?, dateTime: String?, endDateTime: String?, location: String?) {
        viewModelScope.launch {
            try {
                val um = groupRepository.updateMeeting(meetingId, title, description, dateTime, endDateTime, location)
                _meetings.value = _meetings.value.map { if (it.id == meetingId) um else it }
            } catch (_: Exception) {}
        }
    }

    fun deleteMeeting(meetingId: Int) {
        viewModelScope.launch {
            try { groupRepository.deleteMeeting(meetingId); _meetings.value = _meetings.value.filter { it.id != meetingId } } catch (_: Exception) {}
        }
    }

    fun updateRsvp(meetingId: Int, participantId: Int, status: String) {
        viewModelScope.launch { try { groupRepository.updateRsvp(meetingId, participantId, status) } catch (_: Exception) {} }
    }

    // ─── Announcements ────────────────────────────────────────────────────
    fun createAnnouncement(text: String, attachments: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val a = groupRepository.createAnnouncement(groupId, text, attachments)
                _announcements.value = listOf(a) + _announcements.value
                _uiState.value = UiState.Success
            } catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Ошибка") }
        }
    }

    fun deleteAnnouncement(announcementId: Int) {
        viewModelScope.launch {
            try { groupRepository.deleteAnnouncement(announcementId); _announcements.value = _announcements.value.filter { it.id != announcementId } } catch (_: Exception) {}
        }
    }

    fun updateAnnouncement(announcementId: Int, text: String?, attachments: String?) {
        viewModelScope.launch {
            try { groupRepository.updateAnnouncement(announcementId, text, attachments); loadGroupData() } catch (_: Exception) {}
        }
    }

    fun togglePin(announcementId: Int) {
        viewModelScope.launch {
            try { groupRepository.togglePinAnnouncement(announcementId); loadGroupData() } catch (_: Exception) {}
        }
    }

    // ─── Playlists ────────────────────────────────────────────────────────
    fun createPlaylist(name: String, type: String, meetingId: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val p = groupRepository.createPlaylist(groupId, name, type, meetingId)
                _playlists.value = _playlists.value + p
                _uiState.value = UiState.Success
            } catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Ошибка") }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            try { groupRepository.deletePlaylist(playlistId); _playlists.value = _playlists.value.filter { it.id != playlistId } } catch (_: Exception) {}
        }
    }

    fun updatePlaylist(playlistId: Int, name: String?) {
        viewModelScope.launch {
            try { groupRepository.updatePlaylist(playlistId, name); loadGroupData() } catch (_: Exception) {}
        }
    }

    fun addTrack(playlistId: Int, trackId: String, trackName: String, artistName: String, trackViewUrl: String, artworkUrl100: String?, previewUrl: String?) {
        viewModelScope.launch {
            try {
                val t = groupRepository.addTrack(playlistId, trackId, trackName, artistName, trackViewUrl, artworkUrl100, previewUrl)
                _tracks.value = _tracks.value + (playlistId to (_tracks.value[playlistId] ?: emptyList()) + t)
            } catch (_: Exception) {}
        }
    }

    fun removeTrack(playlistId: Int, trackId: Int) {
        viewModelScope.launch {
            try { groupRepository.removeTrack(playlistId, trackId); loadTracks(playlistId) } catch (_: Exception) {}
        }
    }

    fun reorderTracks(playlistId: Int, trackIds: List<Int>) {
        viewModelScope.launch {
            try { groupRepository.reorderTracks(playlistId, trackIds); loadTracks(playlistId) } catch (_: Exception) {}
        }
    }

    // ─── iTunes ───────────────────────────────────────────────────────────
    fun searchItunes(term: String) {
        viewModelScope.launch {
            try { _itunesResults.value = groupRepository.searchItunes(term) } catch (_: Exception) { _itunesResults.value = "[]" }
        }
    }

    fun getTrackInfo(trackId: String) {
        viewModelScope.launch {
            try { _itunesResults.value = groupRepository.getItunesTrack(trackId) } catch (_: Exception) { _itunesResults.value = "{}" }
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
    private val groupId: Int,
    private val currentUserId: Int = -1
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GroupViewModel(groupRepository, groupId, currentUserId) as T
    }
}
