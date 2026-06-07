package com.anjira.taskplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.repository.AuthRepositoryImpl
import com.anjira.taskplanner.data.repository.GroupRepositoryImpl
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.ui.navigation.Routes
import com.anjira.taskplanner.ui.screens.*
import com.anjira.taskplanner.ui.theme.TaskPlannerTheme
import com.anjira.taskplanner.ui.viewmodel.AuthViewModel
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val dataStoreManager = DataStoreManager(applicationContext)
        RetrofitInstance.init { dataStoreManager.getAccessToken() }
        val apiService = RetrofitInstance.apiService
        val db = com.anjira.taskplanner.data.local.room.AppDatabase.getInstance(applicationContext)
        val authRepository = AuthRepositoryImpl(apiService, dataStoreManager)
        val groupRepository = GroupRepositoryImpl(apiService, db)
        val authViewModel = AuthViewModel(authRepository)

        setContent {
            val themeMode by dataStoreManager.observeThemeMode().collectAsState(initial = "system")

            TaskPlannerTheme(
                darkTheme = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
                    val windowSizeClass = calculateWindowSizeClass(this)
                    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

                    AppContent(
                        isExpanded = isExpanded,
                        dataStoreManager = dataStoreManager,
                        groupRepository = groupRepository,
                        authRepository = authRepository,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun AppContent(
    isExpanded: Boolean,
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepositoryImpl,
    authRepository: AuthRepositoryImpl,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoadingStart by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedGroupId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            val token = dataStoreManager.getAccessToken()
            startDestination = if (token != null) Routes.GROUP_LIST else Routes.LOGIN
        } catch (_: Exception) {
            startDestination = Routes.LOGIN
        }
        isLoadingStart = false
    }

    if (isLoadingStart) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        startDestination?.let { start ->
            if (isExpanded) {
                DualPaneLayout(
                    startDestination = start,
                    selectedGroupId = selectedGroupId,
                    onSelectGroup = { selectedGroupId = it },
                    dataStoreManager = dataStoreManager,
                    groupRepository = groupRepository,
                    authRepository = authRepository,
                    authViewModel = authViewModel,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            } else {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        addNavRoutes(
                            navController = navController,
                            dataStoreManager = dataStoreManager,
                            groupRepository = groupRepository,
                            authRepository = authRepository,
                            authViewModel = authViewModel,
                            snackbarHostState = snackbarHostState,
                            scope = scope
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualPaneLayout(
    startDestination: String,
    selectedGroupId: Int?,
    onSelectGroup: (Int?) -> Unit,
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepositoryImpl,
    authRepository: AuthRepositoryImpl,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val navController = rememberNavController()

    if (startDestination == Routes.LOGIN) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { onSelectGroup(-1); navController.navigate(Routes.GROUP_LIST) { popUpTo(0) { inclusive = true } } }
        )
        return
    }

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun loadGroups(isRefresh: Boolean = false) {
        scope.launch {
            if (isRefresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                groups = groupRepository.getUserGroups()
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 401) {
                    if (authRepository.refreshToken()) {
                        try { groups = groupRepository.getUserGroups() } catch (e2: Exception) {
                            errorMessage = e2.message
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    } else { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
                } else { errorMessage = e.message }
            }
            if (isRefresh) isRefreshing = false else isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }

    Row(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.width(360.dp).fillMaxSize(),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            GroupListScreen(
                groups = groups,
                isLoading = isLoading,
                errorMessage = errorMessage,
                isRefreshing = isRefreshing,
                onRefresh = { loadGroups(isRefresh = true) },
                onGroupClick = { groupId -> onSelectGroup(groupId) },
                onCreateGroup = { name, desc, avatar ->
                    scope.launch {
                        try {
                            val group = groupRepository.createGroup(name, desc, avatar)
                            groups = groups + group
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Не удалось создать группу")
                        }
                    }
                },
                onJoinGroup = { inviteCode ->
                    scope.launch {
                        try {
                            val group = groupRepository.joinGroupByCode(inviteCode)
                            groups = groups + group
                            snackbarHostState.showSnackbar("Присоединились к группе ${group.name}")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Не удалось присоединиться к группе")
                        }
                    }
                },
                onProfileClick = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.GROUP_LIST,
                modifier = Modifier.padding(innerPadding)
            ) {
                addDualPaneDetailRoutes(
                    navController = navController,
                    selectedGroupId = selectedGroupId,
                    onSelectGroup = onSelectGroup,
                    dataStoreManager = dataStoreManager,
                    groupRepository = groupRepository,
                    authRepository = authRepository,
                    authViewModel = authViewModel,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            }
        }
    }
}

private fun NavGraphBuilder.addNavRoutes(
    navController: NavHostController,
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepositoryImpl,
    authRepository: AuthRepositoryImpl,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    composable(Routes.LOGIN) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = {
                navController.navigate(Routes.GROUP_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        )
    }

    composable(Routes.GROUP_LIST) {
        var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }

        fun loadGroups(isRefresh: Boolean = false) {
            scope.launch {
                if (isRefresh) isRefreshing = true else isLoading = true
                errorMessage = null
                try {
                    groups = groupRepository.getUserGroups()
                } catch (e: Exception) {
                    if (e is HttpException && e.code() == 401) {
                        val refreshed = authRepository.refreshToken()
                        if (refreshed) {
                            try { groups = groupRepository.getUserGroups() } catch (e2: Exception) {
                                errorMessage = e2.message
                                navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                            }
                        } else { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
                    } else { errorMessage = e.message }
                }
                if (isRefresh) isRefreshing = false else isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadGroups() }

        GroupListScreen(
            groups = groups, isLoading = isLoading, errorMessage = errorMessage,
            isRefreshing = isRefreshing, onRefresh = { loadGroups(isRefresh = true) },
            onGroupClick = { groupId -> navController.navigate(Routes.groupDetail(groupId)) },
            onCreateGroup = { name, desc, avatar ->
                scope.launch {
                    try {
                        val group = groupRepository.createGroup(name, desc, avatar)
                        groups = groups + group
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "Не удалось создать группу")
                    }
                }
            },
            onJoinGroup = { inviteCode ->
                scope.launch {
                    try {
                        val group = groupRepository.joinGroupByCode(inviteCode)
                        groups = groups + group
                        snackbarHostState.showSnackbar("Присоединились к группе ${group.name}")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "Не удалось присоединиться к группе")
                    }
                }
            },
            onProfileClick = { navController.navigate(Routes.PROFILE) }
        )
    }

    composable(Routes.GROUP_DETAIL) { backStackEntry ->
        val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: return@composable
        val currentUserId by dataStoreManager.observeUserId().collectAsState(-1)
        val groupViewModel = remember(groupId, currentUserId) { GroupViewModel(groupRepository, groupId, currentUserId) }
        GroupDetailScreen(
            groupViewModel = groupViewModel, groupId = groupId, currentUserId = currentUserId,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.PROFILE) {
        var isRefreshing by remember { mutableStateOf(false) }
        ProfileScreen(
            dataStoreManager = dataStoreManager, groupRepository = groupRepository,
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { isRefreshing = true; kotlinx.coroutines.delay(500); isRefreshing = false } },
            onBack = { navController.popBackStack() },
            onLogout = { authViewModel.logout(); navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
            onOpenSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) }
        )
    }

    composable(Routes.NOTIFICATION_SETTINGS) {
        NotificationSettingsScreen(groupRepository = groupRepository, onBack = { navController.popBackStack() })
    }
}

private fun NavGraphBuilder.addDualPaneDetailRoutes(
    navController: NavHostController,
    selectedGroupId: Int?,
    onSelectGroup: (Int?) -> Unit,
    dataStoreManager: DataStoreManager,
    groupRepository: GroupRepositoryImpl,
    @Suppress("UNUSED_PARAMETER") authRepository: AuthRepositoryImpl,
    authViewModel: AuthViewModel,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    composable(Routes.LOGIN) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { navController.navigate(Routes.GROUP_LIST) { popUpTo(Routes.LOGIN) { inclusive = true } } }
        )
    }

    composable(Routes.GROUP_LIST) {
        val currentUserId by dataStoreManager.observeUserId().collectAsState(-1)
        selectedGroupId?.let { gid ->
            val groupViewModel = remember(gid, currentUserId) { GroupViewModel(groupRepository, gid, currentUserId) }
            GroupDetailScreen(
                groupViewModel = groupViewModel, groupId = gid, currentUserId = currentUserId,
                onBack = { onSelectGroup(null) }
            )
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Выберите группу из списка", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    composable(Routes.PROFILE) {
        var isRefreshing by remember { mutableStateOf(false) }
        ProfileScreen(
            dataStoreManager = dataStoreManager, groupRepository = groupRepository,
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { isRefreshing = true; kotlinx.coroutines.delay(500); isRefreshing = false } },
            onBack = { navController.popBackStack() },
            onLogout = { authViewModel.logout(); navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
            onOpenSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) }
        )
    }

    composable(Routes.NOTIFICATION_SETTINGS) {
        NotificationSettingsScreen(groupRepository = groupRepository, onBack = { navController.popBackStack() })
    }
}
