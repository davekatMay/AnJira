package com.anjira.taskplanner.ui.viewmodel

import com.anjira.taskplanner.domain.model.*
import com.anjira.taskplanner.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GroupViewModelTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var viewModel: GroupViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val groupId = 1
    private val userId = 42

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads group data`() = runTest(testDispatcher) {
        val group = Group(groupId, "Test Group", "Desc", null, "ABC123", "creator", emptyList(), "now", "now")
        coEvery { groupRepository.getGroupDetail(groupId) } returns group
        coEvery { groupRepository.getGroupTasks(groupId) } returns emptyList()
        coEvery { groupRepository.getGroupMeetings(groupId) } returns emptyList()
        coEvery { groupRepository.getAnnouncements(groupId) } returns emptyList()
        coEvery { groupRepository.getPlaylists(groupId) } returns emptyList()
        coEvery { groupRepository.getNotifications() } returns emptyList()

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()

        assertEquals(group, viewModel.group.value)
        assertTrue(viewModel.uiState.value is GroupViewModel.UiState.Success)
    }

    @Test
    fun `init sets error on failure`() = runTest(testDispatcher) {
        coEvery { groupRepository.getGroupDetail(groupId) } throws Exception("Network error")

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GroupViewModel.UiState.Error)
        assertEquals("Network error", (viewModel.uiState.value as GroupViewModel.UiState.Error).message)
    }

    @Test
    fun `createTask adds task to list`() = runTest(testDispatcher) {
        val task = Task(10, "New Task", null, null, "to_do", userId, null, "now", "now")
        coEvery { groupRepository.getGroupDetail(groupId) } returns mockk()
        coEvery { groupRepository.getGroupTasks(groupId) } returns emptyList()
        coEvery { groupRepository.getGroupMeetings(groupId) } returns emptyList()
        coEvery { groupRepository.getAnnouncements(groupId) } returns emptyList()
        coEvery { groupRepository.getPlaylists(groupId) } returns emptyList()
        coEvery { groupRepository.getNotifications() } returns emptyList()
        coEvery { groupRepository.createTask(groupId, "New Task", null, null, null) } returns task

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()
        viewModel.createTask("New Task", null, null, null)
        advanceUntilIdle()

        assertTrue(viewModel.tasks.value.any { it.title == "New Task" })
    }

    @Test
    fun `deleteTask removes task from list`() = runTest(testDispatcher) {
        val task = Task(10, "Delete Me", null, null, "to_do", userId, null, "now", "now")
        coEvery { groupRepository.getGroupDetail(groupId) } returns mockk()
        coEvery { groupRepository.getGroupTasks(groupId) } returns listOf(task)
        coEvery { groupRepository.getGroupMeetings(groupId) } returns emptyList()
        coEvery { groupRepository.getAnnouncements(groupId) } returns emptyList()
        coEvery { groupRepository.getPlaylists(groupId) } returns emptyList()
        coEvery { groupRepository.getNotifications() } returns emptyList()
        coEvery { groupRepository.deleteTask(10) } returns Unit

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()
        assertEquals(1, viewModel.tasks.value.size)

        viewModel.deleteTask(10)
        advanceUntilIdle()

        assertTrue(viewModel.tasks.value.isEmpty())
    }

    @Test
    fun `toggleTaskStatus cycles through statuses`() = runTest(testDispatcher) {
        val task = Task(10, "Toggle", null, null, "to_do", userId, null, "now", "now")
        val updatedTask = task.copy(status = "in_progress")
        coEvery { groupRepository.getGroupDetail(groupId) } returns mockk()
        coEvery { groupRepository.getGroupTasks(groupId) } returns listOf(task)
        coEvery { groupRepository.getGroupMeetings(groupId) } returns emptyList()
        coEvery { groupRepository.getAnnouncements(groupId) } returns emptyList()
        coEvery { groupRepository.getPlaylists(groupId) } returns emptyList()
        coEvery { groupRepository.getNotifications() } returns emptyList()
        coEvery { groupRepository.updateTask(10, null, null, null, "in_progress", null) } returns updatedTask

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()

        viewModel.toggleTaskStatus(10, "to_do")
        advanceUntilIdle()

        assertEquals("in_progress", viewModel.tasks.value.first().status)
    }

    @Test
    fun `loadTracks populates tracks map`() = runTest(testDispatcher) {
        val tracks = listOf(PlaylistTrack(1, 100, "123", "Song", "Artist", "url", null, null, 1, "now"))
        coEvery { groupRepository.getGroupDetail(groupId) } returns mockk()
        coEvery { groupRepository.getGroupTasks(groupId) } returns emptyList()
        coEvery { groupRepository.getGroupMeetings(groupId) } returns emptyList()
        coEvery { groupRepository.getAnnouncements(groupId) } returns emptyList()
        coEvery { groupRepository.getPlaylists(groupId) } returns emptyList()
        coEvery { groupRepository.getNotifications() } returns emptyList()
        coEvery { groupRepository.getTracks(100) } returns tracks

        viewModel = GroupViewModel(groupRepository, groupId, userId)
        advanceUntilIdle()

        viewModel.loadTracks(100)
        advanceUntilIdle()

        assertEquals(tracks, viewModel.tracks.value[100])
    }
}
