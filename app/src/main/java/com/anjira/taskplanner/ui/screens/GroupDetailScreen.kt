package com.anjira.taskplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anjira.taskplanner.domain.model.*
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupViewModel: GroupViewModel,
    groupId: Int,
    onBack: () -> Unit
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val group by groupViewModel.group.collectAsState()
    val tasks by groupViewModel.tasks.collectAsState()
    val meetings by groupViewModel.meetings.collectAsState()
    val announcements by groupViewModel.announcements.collectAsState()
    val playlists by groupViewModel.playlists.collectAsState()
    val tracksMap by groupViewModel.tracks.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is GroupViewModel.UiState.Error) {
            scope.launch { snackbarHostState.showSnackbar((uiState as GroupViewModel.UiState.Error).message) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Группа #$groupId") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Задачи", "Встречи", "Объявления", "Плейлисты").forEachIndexed { i, label ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) })
                }
            }
            when (selectedTab) {
                0 -> TasksTab(groupViewModel, tasks, group?.members ?: emptyList())
                1 -> MeetingsTab(groupViewModel, meetings)
                2 -> AnnouncementsTab(groupViewModel, announcements)
                3 -> PlaylistsTab(groupViewModel, playlists, tracksMap)
            }
        }
    }
}

// ─── TASKS TAB ────────────────────────────────────────────────────────────
@Composable
private fun TasksTab(vm: GroupViewModel, tasks: List<Task>, members: List<GroupMember>) {
    var filter by remember { mutableStateOf("all") }
    var showCreate by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Все", "mine" to "Мои", "done" to "Готовые").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = {
                            filter = key
                            vm.loadTasks(
                                filter = if (key == "done") null else key,
                                status = if (key == "done") "done" else null
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }

            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tasks.isEmpty()) {
                    item { Text("Нет задач", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(tasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.status == "done",
                                onCheckedChange = { vm.toggleTaskStatus(task.id, task.status) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                task.deadline?.let { Text("Дедлайн: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                Text("Статус: ${task.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.deleteTask(task.id) }) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreate = true }
        ) { Icon(Icons.Default.Add, "Создать задачу") }
    }

    if (showCreate) CreateTaskDialog(vm, members) { showCreate = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(vm: GroupViewModel, members: List<GroupMember>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var deadlineTime by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = deadlineDate, onValueChange = { deadlineDate = it }, label = { Text("Дедлайн дата (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = deadlineTime, onValueChange = { deadlineTime = it }, label = { Text("Дедлайн время (ЧЧ:ММ)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = members.find { it.userId == assignedTo }?.username ?: "Не назначен",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Исполнитель") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        members.forEach { m ->
                            DropdownMenuItem(text = { Text(m.username) }, onClick = { assignedTo = m.userId; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dl = if (deadlineDate.isNotBlank() && deadlineTime.isNotBlank()) "${deadlineDate}T${deadlineTime}:00"
                        else if (deadlineDate.isNotBlank()) "${deadlineDate}T23:59:00" else null
                    vm.createTask(title, desc.ifBlank { null }, dl, assignedTo)
                    onDismiss()
                },
                enabled = title.isNotBlank()
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── MEETINGS TAB ─────────────────────────────────────────────────────────
@Composable
private fun MeetingsTab(vm: GroupViewModel, meetings: List<Meeting>) {
    var showCreate by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SimpleCalendar(
                initialMonth = YearMonth.now(),
                selected = selectedDate,
                meetings = meetings,
                onDayClick = { selectedDate = it }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Встречи на ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val dayMeetings = meetings.filter { it.dateTime.startsWith(selectedDate.toString()) }
                if (dayMeetings.isEmpty()) {
                    item { Text("Нет встреч", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(dayMeetings) { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(m.title, style = MaterialTheme.typography.titleMedium)
                            Text(m.dateTime.substringAfter("T").take(5), style = MaterialTheme.typography.bodySmall)
                            m.location?.let { Text("Место: $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreate = true }
        ) { Icon(Icons.Default.Add, "Создать встречу") }
    }

                if (showCreate) CreateMeetingDialog(vm) { showCreate = false }
}

@Composable
private fun SimpleCalendar(initialMonth: YearMonth, selected: LocalDate, meetings: List<Meeting>, onDayClick: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(initialMonth) }
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startDayOfWeek = firstDay.dayOfWeek.value % 7
    val totalCells = startDayOfWeek + daysInMonth

    Column(Modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Пред") }
            Text(currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy")), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "След") }
        }
        val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayNames.forEach { Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center) }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(220.dp)) {
            items(totalCells) { idx ->
                val day = idx - startDayOfWeek + 1
                if (day in 1..daysInMonth) {
                    val date = currentMonth.atDay(day)
                    val hasEvent = meetings.any { it.dateTime.startsWith(date.toString()) }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onDayClick(date) }
                            .then(if (date == selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$day",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (date == selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (hasEvent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateMeetingDialog(vm: GroupViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая встреча") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Начало (ЧЧ:ММ)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Конец (ЧЧ:ММ, опционально)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Место (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dt = "${date}T${startTime}:00"
                    val edt = if (endTime.isNotBlank()) "${date}T${endTime}:00" else null
                    vm.createMeeting(title, desc.ifBlank { null }, dt, edt, location.ifBlank { null })
                    onDismiss()
                },
                enabled = title.isNotBlank() && date.isNotBlank() && startTime.isNotBlank()
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── ANNOUNCEMENTS TAB ────────────────────────────────────────────────────
@Composable
private fun AnnouncementsTab(vm: GroupViewModel, announcements: List<Announcement>) {
    var showCreate by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (announcements.isEmpty()) {
                item { Text("Нет объявлений", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(announcements, key = { it.id }) { a ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = if (a.isPinned) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                             else CardDefaults.cardColors()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (a.isPinned) {
                                Icon(Icons.Default.Star, "Закреплено", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(a.createdByUsername, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { vm.togglePin(a.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Star, "Закрепить", modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { vm.deleteAnnouncement(a.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(a.text, style = MaterialTheme.typography.bodyMedium)
                        if (a.attachments != "[]") {
                            Spacer(Modifier.height(4.dp))
                            Text("📎 ${JSONArray(a.attachments).length()} вложений", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreate = true }
        ) { Icon(Icons.Default.Add, "Создать объявление") }
    }

    if (showCreate) CreateAnnouncementDialog(vm) { showCreate = false }
}

@Composable
private fun CreateAnnouncementDialog(vm: GroupViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое объявление") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        },
        confirmButton = {
            TextButton(onClick = { vm.createAnnouncement(text, "[]"); onDismiss() }, enabled = text.isNotBlank()) { Text("Опубликовать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── PLAYLISTS TAB ────────────────────────────────────────────────────────
@Composable
private fun PlaylistsTab(vm: GroupViewModel, playlists: List<Playlist>, tracksMap: Map<Int, List<PlaylistTrack>>) {
    var showCreate by remember { mutableStateOf(false) }
    var expandedPlaylist by remember { mutableStateOf<Int?>(null) }
    var showAddTrack by remember { mutableStateOf<Int?>(null) }
    val itunesJson by vm.itunesResults.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (playlists.isEmpty()) {
                item { Text("Нет плейлистов", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(playlists, key = { it.id }) { pl ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                expandedPlaylist = if (expandedPlaylist == pl.id) null else pl.id
                                if (expandedPlaylist == pl.id) vm.loadTracks(pl.id)
                            }) {
                                Icon(if (expandedPlaylist == pl.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Треки")
                            }
                            IconButton(onClick = { vm.deletePlaylist(pl.id) }) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error) }
                        }
                        Text("Тип: ${pl.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (expandedPlaylist == pl.id) {
                            val tracks = tracksMap[pl.id] ?: emptyList()
                            tracks.forEach { t ->
                                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.trackName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(t.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { vm.removeTrack(pl.id, t.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(16.dp)) }
                                }
                            }
                            TextButton(onClick = { showAddTrack = pl.id }) { Text("+ Добавить трек") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreate = true }
        ) { Icon(Icons.Default.Add, "Создать плейлист") }
    }

    if (showCreate) CreatePlaylistDialog(vm) { showCreate = false }

    showAddTrack?.let { plId ->
        AddTrackDialog(vm, plId, itunesJson, onDismiss = { showAddTrack = null })
    }
}

@Composable
private fun AddTrackDialog(vm: GroupViewModel, playlistId: Int, itunesJson: String, onDismiss: () -> Unit) {
    var localQuery by remember { mutableStateOf("") }
    val parsedResults = remember(itunesJson) {
        try {
            val arr = JSONArray(itunesJson)
            (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                TrackResult(
                    name = item.optString("trackName", ""),
                    artist = item.optString("artistName", ""),
                    trackId = item.optString("trackId", ""),
                    url = item.optString("trackViewUrl", ""),
                    art = item.optString("artworkUrl100", ""),
                    prev = item.optString("previewUrl", "").ifEmpty { null }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поиск в iTunes") },
        text = {
            Column {
                OutlinedTextField(value = localQuery, onValueChange = { localQuery = it }, label = { Text("Название трека") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.searchItunes(localQuery) }, modifier = Modifier.fillMaxWidth()) { Text("Поиск") }
                Spacer(Modifier.height(8.dp))
                if (itunesJson == "[]" || itunesJson == "{}") {
                    Text("Введите запрос и нажмите Поиск", style = MaterialTheme.typography.bodySmall)
                } else if (parsedResults.isEmpty()) {
                    Text("Ошибка загрузки результатов", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(Modifier.height(300.dp)) {
                        items(parsedResults) { result ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                vm.addTrack(playlistId, result.trackId, result.name, result.artist, result.url, result.art, result.prev)
                                onDismiss()
                            }) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(result.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(result.artist, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

private data class TrackResult(
    val name: String,
    val artist: String,
    val trackId: String,
    val url: String,
    val art: String?,
    val prev: String?
)

@Composable
private fun CreatePlaylistDialog(vm: GroupViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { vm.createPlaylist(name, "group", null); onDismiss() }, enabled = name.isNotBlank()) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
