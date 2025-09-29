package com.mnvths.schoolclearance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.screen.* // Make sure all your screens are imported
import com.mnvths.schoolclearance.viewmodel.AuthViewModel
import com.mnvths.schoolclearance.viewmodel.SettingsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel(),
                  settingsViewModel: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    val loggedInUser by authViewModel.loggedInUser
    val isUserLoggedIn by authViewModel.isUserLoggedIn
    val appSettings by settingsViewModel.settings.collectAsState()

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
                SignatoryDashboard(user = user, onSignOut = {
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
                AdminDashboard(
                    rootNavController = navController,
                    user = user,
                    onSignOut = {
                        authViewModel.logout()
                        navController.navigate("login") { popUpTo("login") { inclusive = true } }
                    },
                    // ✅ 3. Pass the ViewModel down to the AdminDashboard
                    settingsViewModel = settingsViewModel
                )
            }
        }

        composable("addStudent") {
            AddStudentScreen(navController = navController)
        }

        composable(
            route = "editStudent/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            EditStudentScreen(
                navController = navController,
                studentId = backStackEntry.arguments?.getString("studentId")!!
            )
        }

        // ✅ ADDED: Route for the admin student detail screen at the top level.
        composable(
            "adminStudentDetail/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            AdminStudentDetailScreen(
                navController = navController,
                // ✅ 4. Pass the appSettings state to the detail screen
                appSettings = appSettings,
                studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            )
        }

        composable("addSection") {
            AddSectionScreen(navController = navController)
        }

        composable(
            "editSection/{sectionId}/{gradeLevel}/{sectionName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditSectionScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                initialGradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                initialSectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
            )
        }
    }
}