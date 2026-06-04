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
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.remote.dto.FcmUnregisterRequest
import com.anjira.taskplanner.domain.repository.GroupRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepository,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var groupCount by remember { mutableStateOf(0) }
    var notificationCount by remember { mutableStateOf(0) }
    var isLoggingOut by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        username = dataStoreManager.getUsername() ?: ""
        email = dataStoreManager.getEmail() ?: ""
        try {
            val groups = groupRepository.getUserGroups()
            groupCount = groups.size
            val notifications = groupRepository.getNotifications()
            notificationCount = notifications.count { !it.isRead }
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
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
                StatItem(count = groupCount.toString(), label = "Группы")
                StatItem(count = notificationCount.toString(), label = "Уведомления")
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isLoggingOut) return@Button
                    isLoggingOut = true
                    scope.launch {
                        try {
                            val fcmToken = FirebaseMessaging.getInstance().token.await()
                            RetrofitInstance.apiService.unregisterFcmToken(FcmUnregisterRequest(fcmToken)).execute()
                        } catch (_: Exception) {}
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
    }
}

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
