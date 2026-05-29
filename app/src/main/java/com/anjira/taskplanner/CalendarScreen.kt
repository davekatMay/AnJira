package com.anjira.taskplanner.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anjira.taskplanner.domain.repository.GroupRepository
import com.anjira.taskplanner.ui.theme.TaskPlannerTheme
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import com.anjira.taskplanner.ui.viewmodel.GroupViewModelFactory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
class CalendarScreen : ComponentActivity() {

    private val groupId: Int by lazy { intent.getIntExtra("groupId", -1) }
    private lateinit var groupRepository: GroupRepository

    private val viewModel: GroupViewModel by viewModels {
        GroupViewModelFactory(groupRepository, groupId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupRepository = intent.getSerializableExtra("groupRepository") as GroupRepository
        setContent {
            TaskPlannerTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Calendar") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    CalendarContent(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarContent(
    viewModel: GroupViewModel,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonthDisplay by remember { mutableStateOf("${selectedDate.monthValue}/${selectedDate.year}") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val meetings by viewModel.meetings.collectAsState()

    LaunchedEffect(selectedDate) {
        currentMonthDisplay = "${selectedDate.monthValue}/${selectedDate.year}"
    }

    val meetingsForSelectedDate = meetings.filter { meeting ->
        val meetingDate = LocalDate.parse(meeting.dateTime.split("T")[0])
        meetingDate == selectedDate
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            IconButton(
                onClick = { selectedDate = selectedDate.minusMonths(1) }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
            }

            Text(
                text = currentMonthDisplay,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { selectedDate = selectedDate.plusMonths(1) }
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            weekdays.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterVertically)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val daysInMonth = selectedDate.lengthOfMonth()
        val firstDayOfMonth = selectedDate.withDayOfMonth(1)
        val startOffset = (firstDayOfMonth.dayOfWeek.value - 1 + 6) % 7

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items((startOffset + daysInMonth + 6) / 7) { weekIndex ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    (0 until 7).forEach { dayOfWeek ->
                        val dayNumber = weekIndex * 7 + dayOfWeek - startOffset + 1
                        val isCurrentMonth = dayNumber >= 1 && dayNumber <= daysInMonth
                        val isSelected = isCurrentMonth && dayNumber == selectedDate.dayOfMonth
                        val isToday = isCurrentMonth && dayNumber == LocalDate.now().dayOfMonth &&
                                selectedDate.monthValue == LocalDate.now().monthValue &&
                                selectedDate.year == LocalDate.now().year

                        val backgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else if (isToday) {
                            MaterialTheme.colorScheme.secondary
                        } else if (!isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.background
                        }

                        val textColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (isToday) {
                            MaterialTheme.colorScheme.onSecondary
                        } else if (!isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .clickable {
                                    if (isCurrentMonth) {
                                        selectedDate = selectedDate.withDayOfMonth(dayNumber)
                                    }
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        ) {
                            if (isCurrentMonth) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = textColor,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Meetings for ${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (meetingsForSelectedDate.isEmpty()) {
            Text(
                text = "No meetings scheduled for this date",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(meetingsForSelectedDate.size) { index ->
                    val meeting = meetingsForSelectedDate[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = meeting.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            )

                            meeting.description?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .let { mod -> if (desc.length > 100) mod else mod }
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                meeting.location?.let { loc ->
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = meeting.dateTime.substring(11, 16),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloatingActionButton(
            onClick = {},
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add meeting")
        }
    }
}
