// RENAME THIS FILE to DashboardNavGraph.kt
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
import com.mnvths.schoolclearance.screen.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardNavGraph(
    innerNavController: NavHostController,
    rootNavController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = innerNavController, // Use the INNER controller for this NavHost
        startDestination = "students_graph",
        modifier = modifier
    ) {
        // --- Students Graph ---
        navigation(
            startDestination = "studentManagement",
            route = "students_graph"
        ) {
            composable("studentManagement") {
                // This screen needs to open fullscreen pages, so it gets the ROOT controller
                StudentManagementScreen(navController = rootNavController)
            }
            composable(
                "adminStudentDetail/{studentId}",
                arguments = listOf(navArgument("studentId") { type = NavType.StringType })
            ) { backStackEntry ->
                AdminStudentDetailScreen(
                    navController = innerNavController, // ✅ FIXED: Pass innerNavController
                    studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                )
            }
        }

        // --- Sections Graph ---
        navigation(
            startDestination = "sectionManagement",
            route = "sections_graph"
        ) {
            composable("sectionManagement") { SectionManagementScreen(navController = innerNavController) } // ✅ FIXED
            composable("addSection") { AddSectionScreen(navController = innerNavController) } // ✅ FIXED
            composable(
                "editSection/{sectionId}/{gradeLevel}/{sectionName}",
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.IntType },
                    navArgument("gradeLevel") { type = NavType.StringType },
                    navArgument("sectionName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                EditSectionScreen(
                    navController = innerNavController, // ✅ FIXED
                    sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                    initialGradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                    initialSectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
                )
            }
        }

        // --- Signatories Graph ---
        navigation(
            startDestination = "signatoryList",
            route = "signatories_graph"
        ) {
            composable("signatoryList") { SignatoryListScreen(navController = innerNavController) } // ✅ FIXED
            composable("addSignatory") { AddSignatoryScreen(navController = innerNavController) } // ✅ FIXED
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
                    navController = innerNavController, // ✅ FIXED
                    signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                    signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: "",
                    firstName = backStackEntry.arguments?.getString("firstName") ?: "",
                    lastName = backStackEntry.arguments?.getString("lastName") ?: "",
                    middleName = backStackEntry.arguments?.getString("middleName"),
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
                    navController = innerNavController, // ✅ FIXED
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
                    navController = innerNavController, // ✅ FIXED
                    signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                    signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: ""
                )
            }
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
                    navController = innerNavController, // ✅ FIXED
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
                    navController = innerNavController, // ✅ FIXED
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
            composable("subjectList") { SubjectListScreen(navController = innerNavController) } // ✅ FIXED
            composable("addEditSubject") {
                AddEditSubjectScreen(navController = innerNavController, subjectId = null, initialName = null) // ✅ FIXED
            }
            composable(
                "addEditSubject/{subjectId}/{subjectName}",
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AddEditSubjectScreen(
                    navController = innerNavController, // ✅ FIXED
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
            composable("accountList") { AccountListScreen(navController = innerNavController) } // ✅ FIXED
        }
    }
}