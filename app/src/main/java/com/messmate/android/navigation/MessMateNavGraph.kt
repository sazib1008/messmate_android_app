package com.messmate.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.messmate.android.ui.auth.AuthViewModel
import com.messmate.android.ui.auth.LoginScreen
import com.messmate.android.ui.auth.RegisterScreen
import com.messmate.android.ui.auth.JoinMessScreen
import com.messmate.android.ui.common.SplashScreen
import com.messmate.android.ui.student.StudentMainScreen
import com.messmate.android.ui.common.ComingSoonScreen

import com.messmate.android.ui.manager.ManagerMainScreen
import com.messmate.android.ui.chef.ChefPrepStationScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val JOIN_MESS = "join_mess"
    const val STUDENT_HOME = "student_home"
    const val MANAGER_HOME = "manager_home"
    const val CHEF_TERMINAL = "chef_terminal"
    const val MANAGER_COMING_SOON = "manager_coming_soon"
}

@Composable
fun MessMateNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    initialTargetRoute: String? = null
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(initialTargetRoute) {
        if (!initialTargetRoute.isNullOrBlank()) {
            val destination = when {
                initialTargetRoute.startsWith("student_home") -> Routes.STUDENT_HOME
                initialTargetRoute.startsWith("manager_home") -> Routes.MANAGER_HOME
                initialTargetRoute.startsWith("chef_terminal") -> Routes.CHEF_TERMINAL
                else -> initialTargetRoute
            }
            try {
                navController.navigate(destination)
            } catch (e: Exception) {
                // Route navigation fallback
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role, hasMembership ->
                    val destination = when {
                        role == "OWNER" || role == "MANAGER" || role == "PRIMARY_MANAGER" ->
                            Routes.MANAGER_HOME
                        role == "CHEF" ->
                            Routes.CHEF_TERMINAL
                        hasMembership -> Routes.STUDENT_HOME
                        else -> Routes.JOIN_MESS
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.JOIN_MESS) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.JOIN_MESS) {
            JoinMessScreen(
                onJoinSubmitted = {
                    // After join request submitted, navigate to student home
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.JOIN_MESS) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STUDENT_HOME) {
            StudentMainScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSwitchToManager = {
                    navController.navigate(Routes.MANAGER_HOME)
                }
            )
        }

        composable(Routes.MANAGER_HOME) {
            ManagerMainScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSwitchToStudentView = {
                    navController.navigate(Routes.STUDENT_HOME)
                },
                onNavigateToChefTerminal = {
                    navController.navigate(Routes.CHEF_TERMINAL)
                }
            )
        }

        composable(Routes.CHEF_TERMINAL) {
            ChefPrepStationScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBackToManager = {
                    navController.navigate(Routes.MANAGER_HOME) {
                        popUpTo(Routes.CHEF_TERMINAL) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MANAGER_COMING_SOON) {
            ComingSoonScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
