package com.mnvths.schoolclearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.screen.AdminDashboard
import com.mnvths.schoolclearance.screen.AssignClassesToSignatoryScreen
import com.mnvths.schoolclearance.screen.AssignSignatoryToFacultyScreen
import com.mnvths.schoolclearance.screen.EditFacultyScreen
import com.mnvths.schoolclearance.screen.FacultyDashboard
import com.mnvths.schoolclearance.screen.FacultyDetailsScreen
import com.mnvths.schoolclearance.screen.LoginScreen
import com.mnvths.schoolclearance.screen.StudentDetailScreen
import com.mnvths.schoolclearance.screen.AddFacultyScreen
import com.mnvths.schoolclearance.screen.AddSectionScreen
import com.mnvths.schoolclearance.screen.AddStudentScreen
import com.mnvths.schoolclearance.screen.StudentListScreen
import com.mnvths.schoolclearance.screen.StudentManagementScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val loggedInUser by authViewModel.loggedInUser
    val isUserLoggedIn by authViewModel.isUserLoggedIn

    val startDestination = if (isUserLoggedIn) {
        when (loggedInUser) {
            is LoggedInUser.StudentUser -> "studentDetail"
            is LoggedInUser.FacultyAdminUser -> {
                when ((loggedInUser as LoggedInUser.FacultyAdminUser).user.role) {
                    "faculty" -> "facultyDashboard"
                    "admin" -> "adminDashboard"
                    else -> "login"
                }
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

        composable("studentManagement") {
            StudentManagementScreen(navController = navController)
        }
        composable("addSection") {
            AddSectionScreen(navController = navController)
        }
        composable(
            route = "studentList/{sectionId}/{gradeLevel}/{sectionName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: return@composable
            val gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: ""
            val sectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
            StudentListScreen(
                navController = navController,
                sectionId = sectionId,
                gradeLevel = gradeLevel,
                sectionName = sectionName
            )
        }
        composable(
            route = "addStudent/{sectionId}/{sectionName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("sectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: return@composable
            val sectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
            AddStudentScreen(
                navController = navController,
                sectionId = sectionId,
                sectionName = sectionName
            )
        }

        composable("addFaculty") {
            AddFacultyScreen(navController = navController)
        }

        composable(
            route = "facultyDetails/{facultyId}/{facultyName}/{firstName}/{lastName}/{middleName}/{username}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val middleName = backStackEntry.arguments?.getString("middleName")
            val username = backStackEntry.arguments?.getString("username") ?: ""
            FacultyDetailsScreen(
                navController = navController,
                facultyId = id,
                facultyName = name,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                username = username
            )
        }
        composable(
            route = "editFaculty/{facultyId}/{facultyName}/{firstName}/{lastName}/{middleName}/{username}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val middleName = backStackEntry.arguments?.getString("middleName")
            val username = backStackEntry.arguments?.getString("username") ?: ""
            EditFacultyScreen(
                navController = navController,
                facultyId = id,
                facultyName = name,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                username = username
            )
        }
        composable(
            route = "assignSignatoryToFaculty/{facultyId}/{facultyName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            AssignSignatoryToFacultyScreen(navController = navController, facultyId = id, facultyName = name)
        }
        composable(
            route = "assignClassesToSignatory/{facultyId}/{facultyName}/{signatoryId}/{signatoryName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("signatoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val facultyId = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val facultyName = backStackEntry.arguments?.getString("facultyName") ?: ""
            val signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: return@composable
            val signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: ""
            AssignClassesToSignatoryScreen(
                navController = navController,
                facultyId = facultyId,
                signatoryId = signatoryId,
                signatoryName = signatoryName
            )
        }
    }
}