package com.anjira.taskplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.remote.dto.FcmUnregisterRequest
import com.anjira.taskplanner.domain.model.UserStats
import com.anjira.taskplanner.domain.repository.GroupRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepository,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var completedTaskCount by remember { mutableStateOf(0) }
    var attendedMeetingCount by remember { mutableStateOf(0) }
    var isLoggingOut by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        username = dataStoreManager.getUsername() ?: ""
        email = dataStoreManager.getEmail() ?: ""
        try {
            val stats = groupRepository.getUserStats()
            completedTaskCount = stats.completedTasks
            attendedMeetingCount = stats.attendedMeetings
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("Настройки") }
                }
            )
        }
    ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()

        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                onRefresh()
                refreshTrigger++
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
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.take(2).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(count = completedTaskCount.toString(), label = "Завершено задач")
                    StatItem(count = attendedMeetingCount.toString(), label = "Посещено встреч")
                }

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        if (isLoggingOut) return@Button
                        isLoggingOut = true
                        scope.launch {
                            try {
                                val fcmToken = withContext(Dispatchers.IO) {
                                    FirebaseMessaging.getInstance().token.await()
                                }
                                withContext(Dispatchers.IO) {
                                    RetrofitInstance.apiService.unregisterFcmToken(FcmUnregisterRequest(fcmToken)).execute()
                                }
                            } catch (_: Exception) {}
                            withContext(Dispatchers.IO) { dataStoreManager.clearAll() }
                            onLogout()
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Выйти", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
