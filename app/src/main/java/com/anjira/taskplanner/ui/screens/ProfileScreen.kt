package com.anjira.taskplanner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil.compose.AsyncImage
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
import org.json.JSONObject
import java.io.File
import android.content.Context

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
    var avatar by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactsJson by remember { mutableStateOf("{}") }
    var completedTaskCount by remember { mutableStateOf(0) }
    var attendedMeetingCount by remember { mutableStateOf(0) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editUsername by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editTelegram by remember { mutableStateOf("") }
    var editVk by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var editAvatar by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            avatarUri = it
            editAvatar = copyImageToInternalStorage(context, it) ?: it.toString()
        }
    }

    LaunchedEffect(refreshTrigger) {
        username = dataStoreManager.getUsername() ?: ""
        email = dataStoreManager.getEmail() ?: ""
        try {
            val stats = groupRepository.getUserStats()
            completedTaskCount = stats.completedTasks
            attendedMeetingCount = stats.attendedMeetings
        } catch (_: Exception) {}
        try {
            val response = RetrofitInstance.apiService.getUserProfile().execute()
            if (response.isSuccessful) {
                val data = response.body() ?: return@LaunchedEffect
                avatar = data.avatar ?: ""
                description = data.description ?: ""
                contactsJson = data.contacts ?: "{}"
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Редактировать профиль" else "Профиль") },
                navigationIcon = {
                    TextButton(onClick = { if (isEditing) isEditing = false else onBack() }) {
                        Text(if (isEditing) "Отмена" else "Назад")
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    val contacts = JSONObject()
                                    contacts.put("phone", editPhone)
                                    contacts.put("email", editEmail)
                                    contacts.put("telegram", editTelegram)
                                    contacts.put("vk", editVk)
                                    val body = mapOf(
                                        "username" to editUsername,
                                        "description" to editDescription,
                                        "contacts" to contacts.toString(),
                                        "avatar" to editAvatar
                                    )
                                    RetrofitInstance.apiService.updateUserProfile(body).execute()
                                    dataStoreManager.saveUserInfo(
                                        dataStoreManager.getUserId() ?: -1,
                                        editUsername,
                                        dataStoreManager.getEmail() ?: ""
                                    )
                                } catch (_: Exception) {}
                                isEditing = false
                                refreshTrigger++
                            }
                        }) { Text("Сохранить") }
                    } else {
                        TextButton(onClick = {
                            editUsername = username
                            editDescription = description
                            editAvatar = avatar
                            try {
                                val c = JSONObject(contactsJson)
                                editPhone = c.optString("phone", "")
                                editEmail = c.optString("email", "")
                                editTelegram = c.optString("telegram", "")
                                editVk = c.optString("vk", "")
                            } catch (_: Exception) {}
                            isEditing = true
                        }) { Text("Редактировать") }
                        TextButton(onClick = onOpenSettings) { Text("Настройки") }
                    }
                }
            )
        }
    ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()

        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(Unit) { onRefresh(); refreshTrigger++ }
        }

        LaunchedEffect(isRefreshing) {
            if (!isRefreshing) pullRefreshState.endRefresh()
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (avatar.isNotBlank() && !isEditing) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = "Аватар",
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isEditing && avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Аватар",
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.take(2).uppercase(),
                                fontSize = 32.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (isEditing) {
                        FloatingActionButton(
                            onClick = { avatarPicker.launch("image/*") },
                            modifier = Modifier.size(32.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) { Icon(Icons.Default.Edit, "Изменить", modifier = Modifier.size(16.dp)) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isEditing) {
                    OutlinedTextField(value = editUsername, onValueChange = { editUsername = it }, label = { Text("Ник") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("О себе") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(12.dp))
                    Text("Контакты", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Телефон") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Phone, null) })
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, null) })
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = editTelegram, onValueChange = { editTelegram = it }, label = { Text("Telegram") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Send, null) })
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = editVk, onValueChange = { editVk = it }, label = { Text("VK") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) })
                } else {
                    Text(username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(count = completedTaskCount.toString(), label = "Завершено задач")
                        StatItem(count = attendedMeetingCount.toString(), label = "Посещено встреч")
                    }
                }

                Spacer(Modifier.height(48.dp))

                if (!isEditing) {
                    Button(
                        onClick = {
                            if (isLoggingOut) return@Button
                            isLoggingOut = true
                            scope.launch {
                                try {
                                    val fcmToken = withContext(Dispatchers.IO) { FirebaseMessaging.getInstance().token.await() }
                                    withContext(Dispatchers.IO) { RetrofitInstance.apiService.unregisterFcmToken(FcmUnregisterRequest(fcmToken)).execute() }
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
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                        } else {
                            Text("Выйти", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
            PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
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

private fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "avatars")
        if (!file.exists()) file.mkdirs()
        val avatarFile = File(file, "avatar_${System.currentTimeMillis()}.jpg")
        avatarFile.outputStream().use { output -> inputStream.copyTo(output) }
        inputStream.close()
        avatarFile.absolutePath
    } catch (e: Exception) { null }
}
