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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.repository.AuthRepositoryImpl
import com.anjira.taskplanner.data.repository.GroupRepositoryImpl
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.ui.navigation.Routes
import com.anjira.taskplanner.ui.screens.*
import com.anjira.taskplanner.ui.theme.TaskPlannerTheme
import com.anjira.taskplanner.ui.viewmodel.AuthViewModel
import com.anjira.taskplanner.ui.viewmodel.AuthViewModelFactory
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import com.anjira.taskplanner.ui.viewmodel.GroupViewModelFactory
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
        RetrofitInstance.init(dataStoreManager)
        val apiService = RetrofitInstance.apiService
        val authRepository = AuthRepositoryImpl(apiService, dataStoreManager)
        val groupRepository = GroupRepositoryImpl(apiService, dataStoreManager)
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
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var isLoadingStart by remember { mutableStateOf(true) }
                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        val token = dataStoreManager.getAccessToken()
                        startDestination = if (token != null) Routes.GROUP_LIST else Routes.LOGIN
                        isLoadingStart = false
                    }

                    if (isLoadingStart) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        startDestination?.let { start ->
                            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = start,
                                modifier = Modifier.padding(innerPadding)
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

                                    fun loadGroups() {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            try {
                                                groups = groupRepository.getUserGroups()
                                            } catch (e: Exception) {
                                                if (e is HttpException && e.code() == 401) {
                                                    val refreshed = authRepository.refreshToken()
                                                    if (refreshed) {
                                                        try {
                                                            groups = groupRepository.getUserGroups()
                                                        } catch (e2: Exception) {
                                                            errorMessage = e2.message
                                                            navController.navigate(Routes.LOGIN) {
                                                                popUpTo(0) { inclusive = true }
                                                            }
                                                        }
                                                    } else {
                                                        navController.navigate(Routes.LOGIN) {
                                                            popUpTo(0) { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    errorMessage = e.message
                                                }
                                            }
                                            isLoading = false
                                        }
                                    }

                                    LaunchedEffect(Unit) { loadGroups() }

                                    GroupListScreen(
                                        groups = groups,
                                        isLoading = isLoading,
                                        errorMessage = errorMessage,
                                        onGroupClick = { groupId ->
                                            navController.navigate(Routes.groupDetail(groupId))
                                        },
                                        onCreateGroup = { name, desc, avatar ->
                                            scope.launch {
                                                try {
                                                    val group = groupRepository.createGroup(name, desc, avatar)
                                                    groups = groups + group
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar(e.message ?: "Failed to create group")
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
                                                    snackbarHostState.showSnackbar(e.message ?: "Failed to join group")
                                                }
                                            }
                                        },
                                        onProfileClick = {
                                            navController.navigate(Routes.PROFILE)
                                        }
                                    )
                                }

                                composable(Routes.GROUP_DETAIL) { backStackEntry ->
                                    val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: return@composable
                                    val currentUserId by dataStoreManager.observeUserId().collectAsState(-1)
                                    val groupViewModel = remember(groupId, currentUserId) { GroupViewModel(groupRepository, groupId, currentUserId) }

                                    GroupDetailScreen(
                                        groupViewModel = groupViewModel,
                                        groupId = groupId,
                                        currentUserId = currentUserId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(Routes.PROFILE) {
                                    ProfileScreen(
                                        dataStoreManager = dataStoreManager,
                                        groupRepository = groupRepository,
                                        onBack = { navController.popBackStack() },
                                        onLogout = {
                                            authViewModel.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onOpenSettings = {
                                            navController.navigate(Routes.NOTIFICATION_SETTINGS)
                                        }
                                    )
                                }

                                composable(Routes.NOTIFICATION_SETTINGS) {
                                    NotificationSettingsScreen(
                                        groupRepository = groupRepository,
                                        onBack = { navController.popBackStack() }
                                    )
                             }
                         }
                     }
                 }
             }
         }
     }
 }
 }
}
