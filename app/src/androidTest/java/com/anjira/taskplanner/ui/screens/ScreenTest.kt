package com.anjira.taskplanner.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.model.GroupMember
import com.anjira.taskplanner.domain.model.UserStats
import com.anjira.taskplanner.domain.repository.GroupRepository
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsEmptyStateWhenNoGroups() {
        composeTestRule.setContent {
            GroupListScreen(
                groups = emptyList(),
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = {},
                onCreateGroup = { _, _, _ -> },
                onJoinGroup = {},
                onProfileClick = {}
            )
        }

        composeTestRule.onNodeWithText("Нет групп. Создайте или присоединитесь.").assertIsDisplayed()
    }

    @Test
    fun showsGroupList() {
        val members = listOf(GroupMember(1, "john", "j@e.com", "admin", "now", "now"))
        val groups = listOf(
            Group(1, "Group A", "Description A", null, "ABC", "creator", members, "now", "now"),
            Group(2, "Group B", null, null, "XYZ", "creator", emptyList(), "now", "now")
        )

        composeTestRule.setContent {
            GroupListScreen(
                groups = groups,
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = {},
                onCreateGroup = { _, _, _ -> },
                onJoinGroup = {},
                onProfileClick = {}
            )
        }

        composeTestRule.onNodeWithText("Group A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Group B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description A").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 участников").assertIsDisplayed()
    }

    @Test
    fun createGroupDialogShowsAndDispatches() {
        var createdName: String? = null
        var createdDesc: String? = null

        composeTestRule.setContent {
            GroupListScreen(
                groups = emptyList(),
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = {},
                onCreateGroup = { name, desc, _ ->
                    createdName = name
                    createdDesc = desc
                },
                onJoinGroup = {},
                onProfileClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Создать группу").performClick()
        composeTestRule.onNodeWithText("Создать группу").assertIsDisplayed()

        composeTestRule.onNodeWithText("Название группы").performTextInput("New Group")
        composeTestRule.onNodeWithText("Описание (необязательно)").performTextInput("Group Description")

        composeTestRule.onNodeWithText("Создать").performClick()

        assert(createdName == "New Group")
        assert(createdDesc == "Group Description")
    }

    @Test
    fun joinGroupDialogShowsAndDispatches() {
        var joinedCode: String? = null

        composeTestRule.setContent {
            GroupListScreen(
                groups = emptyList(),
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = {},
                onCreateGroup = { _, _, _ -> },
                onJoinGroup = { code -> joinedCode = code },
                onProfileClick = {}
            )
        }

        composeTestRule.onNodeWithText("Присоединиться").performClick()
        composeTestRule.onNodeWithText("Присоединиться к группе").assertIsDisplayed()

        composeTestRule.onNodeWithText("Код приглашения").performTextInput("ABC12345")

        composeTestRule.onNodeWithText("Присоединиться").performClick()

        assert(joinedCode == "ABC12345")
    }

    @Test
    fun groupClickDispatches() {
        var clickedGroupId: Int? = null
        val members = listOf(GroupMember(1, "john", "j@e.com", "admin", "now", "now"))
        val groups = listOf(Group(1, "Group A", null, null, "ABC", "creator", members, "now", "now"))

        composeTestRule.setContent {
            GroupListScreen(
                groups = groups,
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = { id -> clickedGroupId = id },
                onCreateGroup = { _, _, _ -> },
                onJoinGroup = {},
                onProfileClick = {}
            )
        }

        composeTestRule.onNodeWithText("Group A").performClick()
        assert(clickedGroupId == 1)
    }

    @Test
    fun profileClickDispatches() {
        var profileClicked = false

        composeTestRule.setContent {
            GroupListScreen(
                groups = emptyList(),
                isLoading = false,
                errorMessage = null,
                isRefreshing = false,
                onRefresh = {},
                onGroupClick = {},
                onCreateGroup = { _, _, _ -> },
                onJoinGroup = {},
                onProfileClick = { profileClicked = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Профиль").performClick()
        assert(profileClicked)
    }
}

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsProfileInfo() = runTest {
        val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
        val groupRepository = mockk<GroupRepository>(relaxed = true)

        coEvery { dataStoreManager.getUsername() } returns "john"
        coEvery { dataStoreManager.getEmail() } returns "john@example.com"
        coEvery { groupRepository.getUserStats() } returns UserStats(5, 3)

        composeTestRule.setContent {
            ProfileScreen(
                dataStoreManager = dataStoreManager,
                groupRepository = groupRepository,
                isRefreshing = false,
                onRefresh = {},
                onBack = {},
                onLogout = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("john").assertIsDisplayed()
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun showsLogoutButton() {
        val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
        val groupRepository = mockk<GroupRepository>(relaxed = true)

        composeTestRule.setContent {
            ProfileScreen(
                dataStoreManager = dataStoreManager,
                groupRepository = groupRepository,
                isRefreshing = false,
                onRefresh = {},
                onBack = {},
                onLogout = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Выйти").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class GroupDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTabs() {
        val viewModel = mockk<GroupViewModel>(relaxed = true)

        val uiStateFlow = MutableStateFlow<GroupViewModel.UiState>(GroupViewModel.UiState.Success)
        val groupFlow = MutableStateFlow(Group(1, "Test", null, null, "ABC", "creator", emptyList(), "now", "now"))
        val emptyFlow = MutableStateFlow(emptyList<com.anjira.taskplanner.domain.model.Task>())
        val emptyMeetingFlow = MutableStateFlow(emptyList<com.anjira.taskplanner.domain.model.Meeting>())
        val emptyAnnouncementFlow = MutableStateFlow(emptyList<com.anjira.taskplanner.domain.model.Announcement>())
        val emptyPlaylistFlow = MutableStateFlow(emptyList<com.anjira.taskplanner.domain.model.Playlist>())
        val emptyTracksFlow = MutableStateFlow(emptyMap<Int, List<com.anjira.taskplanner.domain.model.PlaylistTrack>>())
        val emptyNotificationFlow = MutableStateFlow(emptyList<com.anjira.taskplanner.domain.model.AppNotification>())

        every { viewModel.uiState } returns uiStateFlow
        every { viewModel.group } returns groupFlow
        every { viewModel.tasks } returns emptyFlow
        every { viewModel.meetings } returns emptyMeetingFlow
        every { viewModel.announcements } returns emptyAnnouncementFlow
        every { viewModel.playlists } returns emptyPlaylistFlow
        every { viewModel.tracks } returns emptyTracksFlow
        every { viewModel.notifications } returns emptyNotificationFlow

        composeTestRule.setContent {
            GroupDetailScreen(
                groupViewModel = viewModel,
                groupId = 1,
                currentUserId = 1,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Задачи").assertIsDisplayed()
        composeTestRule.onNodeWithText("Встречи").assertIsDisplayed()
        composeTestRule.onNodeWithText("Объявления").assertIsDisplayed()
        composeTestRule.onNodeWithText("Плейлисты").assertIsDisplayed()
    }
}
