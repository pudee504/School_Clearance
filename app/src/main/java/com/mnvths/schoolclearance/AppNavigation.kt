package com.mnvths.schoolclearance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.screen.AdminDashboard
import com.mnvths.schoolclearance.screen.FacultyDashboard
import com.mnvths.schoolclearance.screen.LoginScreen
import com.mnvths.schoolclearance.screen.StudentDetailScreen
import com.mnvths.schoolclearance.viewmodel.AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val loggedInUser by authViewModel.loggedInUser
    val isUserLoggedIn by authViewModel.isUserLoggedIn

    val startDestination = if (isUserLoggedIn) {
        when (val user = loggedInUser) {
            is LoggedInUser.StudentUser -> "studentDetail"
            is LoggedInUser.FacultyAdminUser -> when (user.user.role) {
                "faculty" -> "facultyDashboard"
                "admin" -> "adminDashboard"
                else -> "login"
            }
            null -> "login"
        }
    } else {
        "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLogin = { loginId, password ->
                    authViewModel.login(loginId, password)
                }
            )
        }

        composable("studentDetail") {
            val student = (authViewModel.loggedInUser.value as? LoggedInUser.StudentUser)?.student
            if (student != null) {
                StudentDetailScreen(student = student, onSignOut = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
        }

        composable("facultyDashboard") {
            val user = (authViewModel.loggedInUser.value as? LoggedInUser.FacultyAdminUser)?.user
            if (user != null) {
                FacultyDashboard(user = user, onSignOut = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }, navController = navController)
            }
        }

        composable("adminDashboard") {
            val user = (authViewModel.loggedInUser.value as? LoggedInUser.FacultyAdminUser)?.user
            if (user != null) {
                // ✅ FIX: Removed the navController parameter from the AdminDashboard call.
                AdminDashboard(
                    user = user,
                    onSignOut = {
                        authViewModel.logout()
                        navController.navigate("login") { popUpTo("login") { inclusive = true } }
                    }
                )
            }
        }
    }
}