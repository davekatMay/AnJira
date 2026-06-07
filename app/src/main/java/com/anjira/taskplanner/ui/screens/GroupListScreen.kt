package com.anjira.taskplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import com.anjira.taskplanner.domain.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groups: List<Group>,
    isLoading: Boolean,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onGroupClick: (Int) -> Unit,
    onCreateGroup: (String, String?, String?) -> Unit,
    onJoinGroup: (String) -> Unit,
    onProfileClick: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }
    var newGroupAvatar by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") },
                actions = {
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("Присоединиться")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Профиль")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Создать группу")
            }
        }
    ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()

        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                onRefresh()
            }
        }

        LaunchedEffect(isRefreshing) {
            if (!isRefreshing) {
                pullRefreshState.endRefresh()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                groups.isEmpty() -> {
                    Text(
                        text = "Нет групп. Создайте или присоединитесь.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups) { group ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGroupClick(group.id) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (group.avatar != null && group.avatar.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = group.name.first().uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = group.name.first().uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        group.description?.let {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${group.members.size} участников",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Создать группу") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Название группы") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        label = { Text("Описание (необязательно)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupAvatar,
                        onValueChange = { newGroupAvatar = it },
                        label = { Text("Аватар (URL, необязательно)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(
                                newGroupName,
                                newGroupDesc.ifBlank { null },
                                newGroupAvatar.ifBlank { null }
                            )
                            showCreateDialog = false
                            newGroupName = ""
                            newGroupDesc = ""
                            newGroupAvatar = ""
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Присоединиться к группе") },
            text = {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    label = { Text("Код приглашения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: ABC12345") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (joinCode.isNotBlank()) {
                            onJoinGroup(joinCode.trim())
                            showJoinDialog = false
                            joinCode = ""
                        }
                    },
                    enabled = joinCode.isNotBlank()
                ) {
                    Text("Присоединиться")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
