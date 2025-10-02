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
        /*
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
*/
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
                    settingsViewModel = settingsViewModel
                )
            }
        }

        // --- FULLSCREEN STUDENT ROUTES ---
        composable("addStudent") {
            AddStudentScreen(navController = navController)
        }

        composable(
            "assignStudent/{sectionId}",
            arguments = listOf(navArgument("sectionId") { type = NavType.IntType })
        ) { backStackEntry ->
            AssignStudentScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getInt("sectionId")!!
            )
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

        composable(
            "adminStudentDetail/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            AdminStudentDetailScreen(
                navController = navController,
                appSettings = appSettings,
                studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            )
        }

        composable(
            "sectionStudents/{sectionId}/{sectionName}/{gradeLevel}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("sectionName") { type = NavType.StringType },
                navArgument("gradeLevel") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            SectionStudentListScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getInt("sectionId")!!,
                sectionName = backStackEntry.arguments?.getString("sectionName")!!,
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel")!!
            )
        }

        // --- FULLSCREEN SECTION ROUTES ---
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

        // --- FULLSCREEN SIGNATORY ROUTES ---

        // ✅ ADD THIS ROUTE FOR ADDING A SIGNATORY
        composable("addSignatory") {
            AddSignatoryScreen(navController = navController)
        }

        composable(
            route = "signatoryDetails/{signatoryId}/{signatoryName}/{firstName}/{lastName}/{middleName}/{username}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("signatoryName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            SignatoryDetailsScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: "",
                username = backStackEntry.arguments?.getString("username") ?: ""
            )
        }

        composable(
            route = "editSignatory/{signatoryId}/{signatoryName}/{firstName}/{lastName}/{middleName}/{username}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("signatoryName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditSignatoryScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: "",
                firstName = backStackEntry.arguments?.getString("firstName") ?: "",
                lastName = backStackEntry.arguments?.getString("lastName") ?: "",
                middleName = backStackEntry.arguments?.getString("middleName"),
                username = backStackEntry.arguments?.getString("username") ?: ""
            )
        }

        composable(
            route = "assignSubjectToSignatory/{signatoryId}/{signatoryName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("signatoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignSubjectToSignatoryScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: ""
            )
        }

        composable(
            route = "assignAccountToSignatory/{signatoryId}/{signatoryName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("signatoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignAccountToSignatoryScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: ""
            )
        }
        // ✅ ADD THE NEW ROUTE for the assigned sections screen
        composable(
            route = "assignedSections/{signatoryId}/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignedSectionsScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            )
        }
        // ✅ ADD THE NEW ROUTE for assigning sections to a subject
        composable(
            route = "assignSectionsToSubject/{signatoryId}/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignSectionsToSubjectScreen(
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            )
        }
        composable(
            route = "assignedSectionsForAccount/{signatoryId}/{accountId}/{accountName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("accountId") { type = NavType.IntType },
                navArgument("accountName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignedSectionsForAccountScreen( // New screen
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                accountId = backStackEntry.arguments?.getInt("accountId") ?: 0,
                accountName = backStackEntry.arguments?.getString("accountName") ?: ""
            )
        }

        // ✅ ADD THE NEW ROUTE for assigning sections to an account
        composable(
            route = "assignSectionsToAccount/{signatoryId}/{accountId}/{accountName}",
            arguments = listOf(
                navArgument("signatoryId") { type = NavType.IntType },
                navArgument("accountId") { type = NavType.IntType },
                navArgument("accountName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignSectionsToAccountScreen( // New screen
                navController = navController,
                signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                accountId = backStackEntry.arguments?.getInt("accountId") ?: 0,
                accountName = backStackEntry.arguments?.getString("accountName") ?: ""
            )
        }
        // ✅ MODIFIED: This route now includes the gradeLevel parameter
        composable(
            route = "clearanceScreenAccount/{sectionId}/{accountId}/{sectionName}/{accountName}/{gradeLevel}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("accountId") { type = NavType.IntType },
                navArgument("sectionName") { type = NavType.StringType },
                navArgument("accountName") { type = NavType.StringType },
                navArgument("gradeLevel") { type = NavType.StringType } // ✅ ADD THIS
            )
        ) { backStackEntry ->
            ClearanceScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("accountId") ?: 0,
                sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                subjectName = backStackEntry.arguments?.getString("accountName") ?: "",
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "", // ✅ ADD THIS
                isAccountClearance = true
            )
        }
        // ✅ ADD THIS MISSING ROUTE DEFINITION FOR SUBJECTS
        composable(
            route = "clearanceScreen/{sectionId}/{subjectId}/{gradeLevel}/{sectionName}/{subjectName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ClearanceScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                // We don't pass 'isAccountClearance', so it correctly defaults to 'false'
            )
        }
    }
}