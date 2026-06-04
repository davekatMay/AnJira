package com.anjira.taskplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anjira.taskplanner.domain.model.*
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ─── Shared Date/Time Pickers ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(label: String, value: String, onSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.DateRange, "Выбрать дату") },
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    )
    if (showDialog) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onSelected(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))) }; showDialog = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(label: String, value: String, onSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    )
    if (showDialog) {
        val state = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { onSelected(String.format("%02d:%02d", state.hour, state.minute)); showDialog = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } }
        )
    }
}

// ─── MAIN SCREEN ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupViewModel: GroupViewModel,
    groupId: Int,
    currentUserId: Int = -1,
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
        if (uiState is GroupViewModel.UiState.Error) scope.launch { snackbarHostState.showSnackbar((uiState as GroupViewModel.UiState.Error).message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(group?.name ?: "Группа #$groupId") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Задачи", "Встречи", "Объявления", "Плейлисты").forEachIndexed { i, label -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) }) }
            }
            when (selectedTab) {
                0 -> TasksTab(groupViewModel, tasks, group?.members ?: emptyList())
                1 -> MeetingsTab(groupViewModel, meetings, group?.members ?: emptyList(), currentUserId)
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
    var editTask by remember { mutableStateOf<Task?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Все", "mine" to "Мои", "done" to "Готовые").forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key; vm.loadTasks(filter = if (key == "done") null else key, status = if (key == "done") "done" else null) }, label = { Text(label) })
                }
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tasks.isEmpty()) item { Text("Нет задач", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(tasks, key = { it.id }) { task ->
                    Card(onClick = { editTask = task }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = task.status == "done", onCheckedChange = { vm.toggleTaskStatus(task.id, task.status) })
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
        FloatingActionButton(onClick = { showCreate = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Создать задачу") }
    }
    if (showCreate) CreateTaskDialog(vm, members) { showCreate = false }
    editTask?.let { EditTaskDialog(vm, it, members) { editTask = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(vm: GroupViewModel, members: List<GroupMember>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }; var deadlineTime by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf<Int?>(null) }; var expanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новая задача") }, text = {
        Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); DatePickerField("Дедлайн дата", deadlineDate) { deadlineDate = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Дедлайн время", deadlineTime) { deadlineTime = it }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(value = members.find { it.userId == assignedTo }?.username ?: "Не назначен", onValueChange = {}, readOnly = true, label = { Text("Исполнитель") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { members.forEach { m -> DropdownMenuItem(text = { Text(m.username) }, onClick = { assignedTo = m.userId; expanded = false }) } }
            }
        }
    }, confirmButton = {
        TextButton(onClick = { val dl = if (deadlineDate.isNotBlank() && deadlineTime.isNotBlank()) "${deadlineDate}T${deadlineTime}:00" else if (deadlineDate.isNotBlank()) "${deadlineDate}T23:59:00" else null; vm.createTask(title, desc.ifBlank { null }, dl, assignedTo); onDismiss() }, enabled = title.isNotBlank()) { Text("Создать") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskDialog(vm: GroupViewModel, task: Task, members: List<GroupMember>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description ?: "") }
    var deadlineDate by remember { mutableStateOf(task.deadline?.take(10) ?: "") }
    var deadlineTime by remember { mutableStateOf(task.deadline?.drop(11)?.take(5) ?: "") }
    var status by remember { mutableStateOf(task.status) }
    var assignedTo by remember { mutableStateOf(task.assignedTo) }
    var expandedAssignee by remember { mutableStateOf(false) }
    var expandedStatus by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Редактировать задачу") }, text = {
        Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); DatePickerField("Дедлайн дата", deadlineDate) { deadlineDate = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Дедлайн время", deadlineTime) { deadlineTime = it }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expandedStatus, onExpandedChange = { expandedStatus = it }) {
                OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text("Статус") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedStatus) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) { listOf("to_do", "in_progress", "done").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expandedStatus = false }) } }
            }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expandedAssignee, onExpandedChange = { expandedAssignee = it }) {
                OutlinedTextField(value = members.find { it.userId == assignedTo }?.username ?: "Не назначен", onValueChange = {}, readOnly = true, label = { Text("Исполнитель") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedAssignee) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                DropdownMenu(expanded = expandedAssignee, onDismissRequest = { expandedAssignee = false }) { members.forEach { m -> DropdownMenuItem(text = { Text(m.username) }, onClick = { assignedTo = m.userId; expandedAssignee = false }) } }
            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            val dl = if (deadlineDate.isNotBlank() && deadlineTime.isNotBlank()) "${deadlineDate}T${deadlineTime}:00" else if (deadlineDate.isNotBlank()) "${deadlineDate}T23:59:00" else null
            vm.updateTask(task.id, title, desc.ifBlank { null }, dl, status, assignedTo); onDismiss()
        }, enabled = title.isNotBlank()) { Text("Сохранить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

// ─── MEETINGS TAB ─────────────────────────────────────────────────────────
@Composable
private fun MeetingsTab(vm: GroupViewModel, meetings: List<Meeting>, members: List<GroupMember>, currentUserId: Int) {
    var showCreate by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editMeeting by remember { mutableStateOf<Meeting?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SimpleCalendar(YearMonth.now(), selectedDate, meetings) { selectedDate = it }
            Spacer(Modifier.height(8.dp))
            Text("Встречи на ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val dayMeetings = meetings.filter { it.dateTime.startsWith(selectedDate.toString()) }
                if (dayMeetings.isEmpty()) item { Text("Нет встреч", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(dayMeetings, key = { it.id }) { m ->
                    MeetingCard(m, onRsvp = { status -> vm.updateRsvp(m.id, currentUserId, status) }, onEdit = { editMeeting = m }, onDelete = { vm.deleteMeeting(m.id) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
        FloatingActionButton(onClick = { showCreate = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Создать встречу") }
    }
    if (showCreate) CreateMeetingDialog(vm, members) { showCreate = false }
    editMeeting?.let { EditMeetingDialog(vm, it, members) { editMeeting = null } }
}

@Composable
private fun MeetingCard(meeting: Meeting, onRsvp: (String) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = { onEdit() }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(meeting.title, style = MaterialTheme.typography.titleMedium)
                    Text(meeting.dateTime.substringAfter("T").take(5), style = MaterialTheme.typography.bodySmall)
                    meeting.location?.let { Text("Место: $it", style = MaterialTheme.typography.bodySmall) }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Мой статус:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("going" to "Пойду", "maybe" to "Возможно", "declined" to "Отказ").forEach { (key, label) ->
                    FilterChip(selected = meeting.myRsvp == key, onClick = { onRsvp(key) }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}

@Composable
private fun SimpleCalendar(initialMonth: YearMonth, selected: LocalDate, meetings: List<Meeting>, onDayClick: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(initialMonth) }
    val firstDay = currentMonth.atDay(1); val daysInMonth = currentMonth.lengthOfMonth()
    val startDayOfWeek = firstDay.dayOfWeek.value % 7; val totalCells = startDayOfWeek + daysInMonth
    Column(Modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Пред") }
            Text(currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy")), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "След") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб").forEach { Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center) }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(220.dp)) {
            items(totalCells) { idx ->
                val day = idx - startDayOfWeek + 1
                if (day in 1..daysInMonth) { val date = currentMonth.atDay(day); val hasEvent = meetings.any { it.dateTime.startsWith(date.toString()) }
                    Box(Modifier.size(32.dp).clip(CircleShape).clickable { onDayClick(date) }.then(if (date == selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier), contentAlignment = Alignment.Center) {
                        Text("$day", style = MaterialTheme.typography.bodySmall, color = if (date == selected) MaterialTheme.colorScheme.onPrimaryContainer else if (hasEvent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                } else { Spacer(Modifier.size(32.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMeetingDialog(vm: GroupViewModel, members: List<GroupMember>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }; var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }
    var invited by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новая встреча") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); DatePickerField("Дата", date) { date = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Начало", startTime) { startTime = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Конец", endTime) { endTime = it }
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Место (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); Text("Пригласить участников:", style = MaterialTheme.typography.labelLarge)
            members.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { invited = if (m.userId in invited) invited - m.userId else invited + m.userId }) {
                    Checkbox(checked = m.userId in invited, onCheckedChange = { invited = if (m.userId in invited) invited - m.userId else invited + m.userId })
                    Spacer(Modifier.width(8.dp)); Text(m.username, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = { val dt = "${date}T${startTime}:00"; val edt = if (endTime.isNotBlank()) "${date}T${endTime}:00" else null; vm.createMeeting(title, desc.ifBlank { null }, dt, edt, location.ifBlank { null }, invited.toList()); onDismiss() }, enabled = title.isNotBlank() && date.isNotBlank() && startTime.isNotBlank()) { Text("Создать") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMeetingDialog(vm: GroupViewModel, meeting: Meeting, members: List<GroupMember>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(meeting.title) }
    var desc by remember { mutableStateOf(meeting.description ?: "") }
    var date by remember { mutableStateOf(meeting.dateTime.take(10)) }
    var startTime by remember { mutableStateOf(meeting.dateTime.substringAfter("T").take(5)) }
    var endTime by remember { mutableStateOf(meeting.endDateTime?.substringAfter("T")?.take(5) ?: "") }
    var location by remember { mutableStateOf(meeting.location ?: "") }
    var invited by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Редактировать встречу") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); DatePickerField("Дата", date) { date = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Начало", startTime) { startTime = it }
            Spacer(Modifier.height(4.dp)); TimePickerField("Конец", endTime) { endTime = it }
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Место (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); Text("Участники:", style = MaterialTheme.typography.labelLarge)
            members.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { invited = if (m.userId in invited) invited - m.userId else invited + m.userId }) {
                    Checkbox(checked = m.userId in invited, onCheckedChange = { invited = if (m.userId in invited) invited - m.userId else invited + m.userId })
                    Spacer(Modifier.width(8.dp)); Text(m.username, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = { val dt = "${date}T${startTime}:00"; val edt = if (endTime.isNotBlank()) "${date}T${endTime}:00" else null; vm.updateMeeting(meeting.id, title, desc.ifBlank { null }, dt, edt, location.ifBlank { null }); onDismiss() }, enabled = title.isNotBlank()) { Text("Сохранить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

// ─── ANNOUNCEMENTS TAB ────────────────────────────────────────────────────
@Composable
private fun AnnouncementsTab(vm: GroupViewModel, announcements: List<Announcement>) {
    var showCreate by remember { mutableStateOf(false) }
    var editAnn by remember { mutableStateOf<Announcement?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (announcements.isEmpty()) item { Text("Нет объявлений", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(announcements, key = { it.id }) { a ->
                Card(Modifier.fillMaxWidth(), colors = if (a.isPinned) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else CardDefaults.cardColors()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (a.isPinned) { Icon(Icons.Default.Star, "Закреплено", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)) }
                            Text(a.createdByUsername, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { vm.togglePin(a.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Star, "Закрепить", modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { editAnn = a }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { vm.deleteAnnouncement(a.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                        }
                        Spacer(Modifier.height(4.dp)); Text(a.text, style = MaterialTheme.typography.bodyMedium)
                        if (a.attachments != "[]") {
                            Spacer(Modifier.height(4.dp))
                            val attUrls = try { JSONArray(a.attachments).let { arr -> (0 until arr.length()).map { arr.getString(it) } } } catch (_: Exception) { emptyList() }
                            attUrls.forEach { url -> Text("🔗 $url", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(onClick = { showCreate = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Создать объявление") }
    }
    if (showCreate) CreateAnnouncementDialog(vm) { showCreate = false }
    editAnn?.let { EditAnnouncementDialog(vm, it) { editAnn = null } }
}

@Composable
private fun CreateAnnouncementDialog(vm: GroupViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }; var attachmentUrl by remember { mutableStateOf("") }; var attachmentUrls by remember { mutableStateOf(listOf<String>()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новое объявление") }, text = {
        Column {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = attachmentUrl, onValueChange = { attachmentUrl = it }, label = { Text("URL вложения") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (attachmentUrl.isNotBlank()) { attachmentUrls = attachmentUrls + attachmentUrl; attachmentUrl = "" } }) { Icon(Icons.Default.Add, "Добавить") }
            }
            attachmentUrls.forEach { url -> Text("📎 $url", style = MaterialTheme.typography.bodySmall) }
        }
    }, confirmButton = { TextButton(onClick = { vm.createAnnouncement(text, JSONArray(attachmentUrls.toTypedArray()).toString()); onDismiss() }, enabled = text.isNotBlank()) { Text("Опубликовать") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
private fun EditAnnouncementDialog(vm: GroupViewModel, announcement: Announcement, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(announcement.text) }
    var attachmentUrl by remember { mutableStateOf("") }
    var attachmentUrls by remember { mutableStateOf<List<String>>(try { JSONArray(announcement.attachments).let { arr -> (0 until arr.length()).map { arr.getString(it) } } } catch (_: Exception) { emptyList() }) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Редактировать объявление") }, text = {
        Column {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = attachmentUrl, onValueChange = { attachmentUrl = it }, label = { Text("URL вложения") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (attachmentUrl.isNotBlank()) { attachmentUrls = attachmentUrls + attachmentUrl; attachmentUrl = "" } }) { Icon(Icons.Default.Add, "Добавить") }
            }
            attachmentUrls.forEach { url -> Text("📎 $url", style = MaterialTheme.typography.bodySmall) }
        }
    }, confirmButton = { TextButton(onClick = { vm.updateAnnouncement(announcement.id, text, JSONArray(attachmentUrls.toTypedArray()).toString()); onDismiss() }, enabled = text.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

// ─── PLAYLISTS TAB ────────────────────────────────────────────────────────
@Composable
private fun PlaylistsTab(vm: GroupViewModel, playlists: List<Playlist>, tracksMap: Map<Int, List<PlaylistTrack>>) {
    var showCreate by remember { mutableStateOf(false) }
    var expandedPlaylist by remember { mutableStateOf<Int?>(null) }
    var showAddTrack by remember { mutableStateOf<Int?>(null) }
    var renamePlaylist by remember { mutableStateOf<Playlist?>(null) }
    val itunesJson by vm.itunesResults.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (playlists.isEmpty()) item { Text("Нет плейлистов", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(playlists, key = { it.id }) { pl ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { expandedPlaylist = if (expandedPlaylist == pl.id) null else pl.id; if (expandedPlaylist == pl.id) vm.loadTracks(pl.id) }) { Icon(if (expandedPlaylist == pl.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Треки") }
                            IconButton(onClick = { renamePlaylist = pl }) { Icon(Icons.Default.Edit, "Переименовать", modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = { vm.deletePlaylist(pl.id) }) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error) }
                        }
                        Text("Тип: ${pl.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (expandedPlaylist == pl.id) {
                            val tracks = tracksMap[pl.id] ?: emptyList()
                            var draggedIndex by remember { mutableStateOf<Int?>(null) }
                            var dragOffset by remember { mutableStateOf(0f) }

                            tracks.forEachIndexed { index, t ->
                                val isDragging = draggedIndex == index
                                Box(Modifier.fillMaxWidth().zIndex(if (isDragging) 1f else 0f).graphicsLayer { translationY = if (isDragging) dragOffset else 0f }.pointerInput(tracks.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedIndex = index; dragOffset = 0f },
                                        onDrag = { change, dragAmount -> change.consume(); dragOffset += dragAmount.y },
                                        onDragEnd = {
                                            val swapIndex = (index + (dragOffset / 60f).roundToInt()).coerceIn(0, tracks.size - 1)
                                            if (swapIndex != index) {
                                                val mutable = tracks.toMutableList()
                                                val temp = mutable[index]; mutable[index] = mutable[swapIndex]; mutable[swapIndex] = temp
                                                vm.reorderTracks(pl.id, mutable.map { it.id })
                                            }
                                            draggedIndex = null; dragOffset = 0f
                                        },
                                        onDragCancel = { draggedIndex = null; dragOffset = 0f }
                                    )
                                }) {
                                    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Menu, "Перетащить", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(4.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(t.trackName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(t.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { vm.removeTrack(pl.id, t.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(16.dp)) }
                                    }
                                }
                            }
                            TextButton(onClick = { showAddTrack = pl.id }) { Text("+ Добавить трек") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(onClick = { showCreate = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Создать плейлист") }
    }
    if (showCreate) CreatePlaylistDialog(vm) { showCreate = false }
    showAddTrack?.let { plId -> AddTrackDialog(vm, plId, itunesJson, onDismiss = { showAddTrack = null }) }
    renamePlaylist?.let { RenamePlaylistDialog(vm, it) { renamePlaylist = null } }
}

@Composable
private fun RenamePlaylistDialog(vm: GroupViewModel, playlist: Playlist, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(playlist.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Переименовать плейлист") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { vm.updatePlaylist(playlist.id, name); onDismiss() }, enabled = name.isNotBlank()) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
private fun AddTrackDialog(vm: GroupViewModel, playlistId: Int, itunesJson: String, onDismiss: () -> Unit) {
    var localQuery by remember { mutableStateOf("") }
    val parsedResults = remember(itunesJson) {
        try { val arr = JSONArray(itunesJson); (0 until arr.length()).map { i -> val item = arr.getJSONObject(i); TrackResult(item.optString("trackName", ""), item.optString("artistName", ""), item.optString("trackId", ""), item.optString("trackViewUrl", ""), item.optString("artworkUrl100", ""), item.optString("previewUrl", "").ifEmpty { null }) } } catch (_: Exception) { emptyList() }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Поиск в iTunes") }, text = {
        Column {
            OutlinedTextField(value = localQuery, onValueChange = { localQuery = it }, label = { Text("Название трека") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); Button(onClick = { vm.searchItunes(localQuery) }, modifier = Modifier.fillMaxWidth()) { Text("Поиск") }
            Spacer(Modifier.height(8.dp))
            if (itunesJson == "[]" || itunesJson == "{}") { Text("Введите запрос и нажмите Поиск", style = MaterialTheme.typography.bodySmall) }
            else if (parsedResults.isEmpty()) { Text("Нет результатов", style = MaterialTheme.typography.bodySmall) }
            else {
                LazyColumn(Modifier.height(300.dp)) {
                    items(parsedResults) { result ->
                        Card(onClick = { vm.addTrack(playlistId, result.trackId, result.name, result.artist, result.url, result.art, result.prev); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(result.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(result.artist, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } })
}

private data class TrackResult(val name: String, val artist: String, val trackId: String, val url: String, val art: String?, val prev: String?)

@Composable
private fun CreatePlaylistDialog(vm: GroupViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новый плейлист") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { vm.createPlaylist(name, "group", null); onDismiss() }, enabled = name.isNotBlank()) { Text("Создать") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
