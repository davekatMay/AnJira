package com.anjira.taskplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.remote.dto.NotificationPreferencesRequest
import com.anjira.taskplanner.data.remote.dto.NotificationPreferencesResponse
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    groupRepository: GroupRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var preferences by remember { mutableStateOf<Map<Int, NotificationPreferencesResponse>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                groups = groupRepository.getUserGroups()
                groups.forEach { group ->
                    val api = RetrofitInstance.apiService
                    val resp = api.getNotificationPreferences(group.id).execute()
                    if (resp.isSuccessful) {
                        resp.body()?.let { prefs ->
                            preferences = preferences + (group.id to prefs)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки уведомлений") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups.size) { i ->
                    val group = groups[i]
                    val prefs = preferences[group.id]
                    GroupNotificationCard(
                        groupName = group.name,
                        preferences = prefs,
                        onToggle = { key, value ->
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        val api = RetrofitInstance.apiService
                                        val request = NotificationPreferencesRequest(
                                            taskAssigned = if (key == "taskAssigned") value else null,
                                            taskStatusChanged = if (key == "taskStatusChanged") value else null,
                                            meetingCreated = if (key == "meetingCreated") value else null,
                                            meetingReminder = if (key == "meetingReminder") value else null,
                                            announcementPosted = if (key == "announcementPosted") value else null,
                                            groupInvite = if (key == "groupInvite") value else null
                                        )
                                        api.updateNotificationPreferences(group.id, request).execute()
                                    }
                                    if (result.isSuccessful) {
                                        preferences[group.id]?.let { old ->
                                            val updated = when (key) {
                                                "taskAssigned" -> old.copy(taskAssigned = value)
                                                "taskStatusChanged" -> old.copy(taskStatusChanged = value)
                                                "meetingCreated" -> old.copy(meetingCreated = value)
                                                "meetingReminder" -> old.copy(meetingReminder = value)
                                                "announcementPosted" -> old.copy(announcementPosted = value)
                                                "groupInvite" -> old.copy(groupInvite = value)
                                                else -> old
                                            }
                                            preferences = preferences + (group.id to updated)
                                        }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupNotificationCard(
    groupName: String,
    preferences: NotificationPreferencesResponse?,
    onToggle: (key: String, value: Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(groupName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (preferences != null) {
                ToggleItem("Назначение задач", preferences.taskAssigned) { onToggle("taskAssigned", it) }
                ToggleItem("Изменение статуса", preferences.taskStatusChanged) { onToggle("taskStatusChanged", it) }
                ToggleItem("Создание встреч", preferences.meetingCreated) { onToggle("meetingCreated", it) }
                ToggleItem("Напоминание о встрече", preferences.meetingReminder) { onToggle("meetingReminder", it) }
                ToggleItem("Объявления", preferences.announcementPosted) { onToggle("announcementPosted", it) }
                ToggleItem("Приглашения", preferences.groupInvite) { onToggle("groupInvite", it) }
            } else {
                Text("Не удалось загрузить настройки", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ToggleItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
