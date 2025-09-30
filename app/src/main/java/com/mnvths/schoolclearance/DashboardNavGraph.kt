package com.mnvths.schoolclearance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.screen.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardNavGraph(
    innerNavController: NavHostController,
    rootNavController: NavHostController,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = innerNavController,
        startDestination = "students_graph",
        modifier = modifier
    ) {
        // --- Students Graph ---
        navigation(
            startDestination = "studentManagement",
            route = "students_graph"
        ) {
            composable("studentManagement") {
                // This screen uses the root controller to navigate to top-level screens like "Add" or "Edit"
                StudentManagementScreen(navController = rootNavController)
            }
            // ✅ REMOVED: The "adminStudentDetail" composable was removed from this nested graph.
        }

        // --- Sections Graph ---
        navigation(
            startDestination = "sectionManagement",
            route = "sections_graph"
        ) {
            composable("sectionManagement") {
                SectionManagementScreen(rootNavController = rootNavController)
            }
        }

        // --- Signatories Graph ---
        navigation(
            startDestination = "signatoryList",
            route = "signatories_graph"
        ) {
            composable("signatoryList") {
                SignatoryListScreen(navController = rootNavController)
            }
            composable("addSignatory") { AddSignatoryScreen(navController = innerNavController) }

            composable(
                route = "assignClassesToSubject/{signatoryId}/{signatoryName}/{subjectId}/{subjectName}",
                arguments = listOf(
                    navArgument("signatoryId") { type = NavType.IntType },
                    navArgument("signatoryName") { type = NavType.StringType },
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AssignClassesToSubjectScreen(
                    navController = innerNavController,
                    signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                    subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                    subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                )
            }
            composable(
                "clearanceScreen/{sectionId}/{subjectId}/{gradeLevel}/{sectionName}/{subjectName}",
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.IntType },
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("gradeLevel") { type = NavType.StringType },
                    navArgument("sectionName") { type = NavType.StringType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                ClearanceScreen(
                    navController = innerNavController,
                    sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                    subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                    gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                    sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                    subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                )
            }
        }

        // --- Subjects Graph ---
        navigation(
            startDestination = "subjectList",
            route = "subjects_graph"
        ) {
            composable("subjectList") { SubjectListScreen(navController = innerNavController, appSettings = appSettings) }
            composable("addEditSubject") {
                AddEditSubjectScreen(navController = innerNavController, subjectId = null, initialName = null)
            }
            composable(
                "addEditSubject/{subjectId}/{subjectName}",
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AddEditSubjectScreen(
                    navController = innerNavController,
                    subjectId = backStackEntry.arguments?.getInt("subjectId"),
                    initialName = backStackEntry.arguments?.getString("subjectName")
                )
            }
        }

        // --- Accounts Graph ---
        navigation(
            startDestination = "accountList",
            route = "accounts_graph"
        ) {
            composable("accountList") { AccountListScreen(navController = innerNavController) }
        }
    }
}