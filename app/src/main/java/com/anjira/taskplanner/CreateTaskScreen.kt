package com.anjira.taskplanner.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anjira.taskplanner.ui.theme.TaskPlannerTheme
import com.anjira.taskplanner.domain.repository.GroupRepository
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import com.anjira.taskplanner.ui.viewmodel.GroupViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
class CreateTaskScreen : ComponentActivity() {

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
                            title = { Text("Create Task") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    CreateTaskContent(
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
fun CreateTaskContent(
    viewModel: GroupViewModel,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAssignedTo by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is GroupViewModel.UiState.Success -> {}
            is GroupViewModel.UiState.Error -> {
                errorMessage = (uiState as GroupViewModel.UiState.Error).message
            }
            else -> {
                errorMessage = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TextField(
            label = { Text("Title") },
            value = title,
            onValueChange = { title = it },
            isError = errorMessage != null && errorMessage!!.contains("title", ignoreCase = true),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            label = { Text("Description") },
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showAssignedTo,
                onCheckedChange = { showAssignedTo = it }
            )
            Text("Assign to member", modifier = Modifier.weight(1f))
        }

        if (showAssignedTo) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                label = { Text("Assigned To (User ID)") },
                value = assignedTo.toString(),
                onValueChange = {
                    assignedTo = if (it.isBlank()) null else it.toIntOrNull()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (title.isBlank()) {
                    errorMessage = "Title is required"
                    return@Button
                }
                isLoading = true
                viewModel.createTask(title, if (description.isBlank()) null else description, null, assignedTo)
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Create Task")
            }
        }
    }
}
