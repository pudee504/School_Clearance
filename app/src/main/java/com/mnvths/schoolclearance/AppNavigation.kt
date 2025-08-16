package com.mnvths.schoolclearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.screen.AdminDashboard
import com.mnvths.schoolclearance.screen.AssignClassesToSubjectScreen
import com.mnvths.schoolclearance.screen.AssignSubjectToFacultyScreen
import com.mnvths.schoolclearance.screen.EditFacultyScreen
import com.mnvths.schoolclearance.screen.FacultyDashboard
import com.mnvths.schoolclearance.screen.FacultyDetailsScreen
import com.mnvths.schoolclearance.screen.LoginScreen
import com.mnvths.schoolclearance.screen.StudentDetailScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    LaunchedEffect(authViewModel.loggedInUser.value) {
        authViewModel.loggedInUser.value?.let { user ->
            when (user) {
                is LoggedInUser.StudentUser -> navController.navigate("studentDetail") {
                    popUpTo("login") { inclusive = true }
                }
                is LoggedInUser.FacultyAdminUser -> {
                    when (user.user.role) {
                        "faculty" -> navController.navigate("facultyDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        "admin" -> navController.navigate("adminDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        else -> {
                            navController.navigate("login")
                        }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
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
                })
            }
        }

        composable("adminDashboard") {
            val user = (authViewModel.loggedInUser.value as? LoggedInUser.FacultyAdminUser)?.user
            if (user != null) {
                AdminDashboard(
                    user = user,
                    onSignOut = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    navController = navController
                )
            }
        }

        // NEW navigation routes for managing faculty
        composable(
            route = "facultyDetails/{facultyId}/{facultyName}/{firstName}/{lastName}/{middleName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val middleName = backStackEntry.arguments?.getString("middleName")
            FacultyDetailsScreen(
                navController = navController,
                facultyId = id,
                facultyName = name,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName
            )
        }
        composable(
            route = "editFaculty/{facultyId}/{facultyName}/{firstName}/{lastName}/{middleName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val middleName = backStackEntry.arguments?.getString("middleName")
            EditFacultyScreen(
                navController = navController,
                facultyId = id,
                facultyName = name,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName
            )
        }
        composable(
            route = "assignSubjectToFaculty/{facultyId}/{facultyName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            AssignSubjectToFacultyScreen(navController = navController, facultyId = id, facultyName = name)
        }
        composable(
            route = "assignClassesToSubject/{facultyId}/{facultyName}/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val facultyId = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val facultyName = backStackEntry.arguments?.getString("facultyName") ?: ""
            val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: return@composable
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            AssignClassesToSubjectScreen(
                navController = navController,
                facultyId = facultyId,
                subjectId = subjectId,
                subjectName = subjectName
            )
        }
    }
}
