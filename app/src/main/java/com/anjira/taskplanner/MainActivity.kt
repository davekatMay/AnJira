package com.anjira.taskplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.data.remote.RetrofitInstance
import com.anjira.taskplanner.data.repository.AuthRepositoryImpl
import com.anjira.taskplanner.data.repository.GroupRepositoryImpl
import com.anjira.taskplanner.domain.model.Group
import com.anjira.taskplanner.ui.navigation.Routes
import com.anjira.taskplanner.ui.screens.GroupDetailScreen
import com.anjira.taskplanner.ui.screens.GroupListScreen
import com.anjira.taskplanner.ui.screens.LoginScreen
import com.anjira.taskplanner.ui.theme.TaskPlannerTheme
import com.anjira.taskplanner.ui.viewmodel.AuthViewModel
import com.anjira.taskplanner.ui.viewmodel.AuthViewModelFactory
import com.anjira.taskplanner.ui.viewmodel.GroupViewModel
import com.anjira.taskplanner.ui.viewmodel.GroupViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStoreManager = DataStoreManager(applicationContext)
        val authRepository = AuthRepositoryImpl(RetrofitInstance.apiService, dataStoreManager)
        val groupRepository = GroupRepositoryImpl(RetrofitInstance.apiService, dataStoreManager)
        val authViewModel = AuthViewModel(authRepository)

        setContent {
            TaskPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val token = dataStoreManager.getToken()
                        startDestination = if (token != null) Routes.GROUP_LIST else Routes.LOGIN
                    }

                    startDestination?.let { start ->
                        NavHost(
                            navController = navController,
                            startDestination = start
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

                                LaunchedEffect(Unit) {
                                    isLoading = true
                                    try {
                                        groups = groupRepository.getUserGroups()
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                    isLoading = false
                                }

                                GroupListScreen(
                                    groups = groups,
                                    isLoading = isLoading,
                                    errorMessage = errorMessage,
                                    onGroupClick = { groupId ->
                                        navController.navigate(Routes.groupDetail(groupId))
                                    },
                                    onCreateGroup = { name, desc ->
                                        scope.launch {
                                            try {
                                                val group = groupRepository.createGroup(name, desc)
                                                groups = groups + group
                                            } catch (e: Exception) {
                                                errorMessage = e.message
                                            }
                                        }
                                    },
                                    onLogout = {
                                        authViewModel.logout()
                                        navController.navigate(Routes.LOGIN) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Routes.GROUP_DETAIL) { backStackEntry ->
                                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: return@composable
                                val groupViewModel = GroupViewModel(groupRepository, groupId)

                                GroupDetailScreen(
                                    groupViewModel = groupViewModel,
                                    groupId = groupId,
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
