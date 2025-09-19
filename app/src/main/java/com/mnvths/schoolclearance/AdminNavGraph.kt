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
fun AdminNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "students_graph",
        modifier = modifier
    ) {
        // Students graph (unchanged)
        navigation(
            startDestination = "studentManagement",
            route = "students_graph"
        ) {
            composable("studentManagement") { StudentManagementScreen(navController = navController) }
            composable(
                "adminStudentDetail/{studentId}",
                arguments = listOf(navArgument("studentId") { type = NavType.StringType })
            ) { backStackEntry ->
                AdminStudentDetailScreen(
                    navController = navController,
                    studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                )
            }
            composable("addStudent") { AddStudentScreen(navController = navController) }
            composable(
                "editStudent/{studentId}",
                arguments = listOf(navArgument("studentId") { type = NavType.StringType })
            ) { backStackEntry ->
                EditStudentScreen(
                    navController = navController,
                    studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                )
            }
        }

        // Sections graph (unchanged)
        navigation(
            startDestination = "sectionManagement",
            route = "sections_graph"
        ) {
            composable("sectionManagement") { SectionManagementScreen(navController = navController) }
            composable("addSection") { AddSectionScreen(navController = navController) }
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

        // Signatories graph (unchanged)
        navigation(
            startDestination = "signatoryList",
            route = "signatories_graph"
        ) {
            composable("signatoryList") { SignatoryListScreen(navController = navController) }
            composable("addSignatory") { AddSignatoryScreen(navController = navController) }
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
                route = "assignClassesToSubject/{signatoryId}/{signatoryName}/{subjectId}/{subjectName}",
                arguments = listOf(
                    navArgument("signatoryId") { type = NavType.IntType },
                    navArgument("signatoryName") { type = NavType.StringType },
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AssignClassesToSubjectScreen(
                    navController = navController,
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
                    navController = navController,
                    sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                    subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                    gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                    sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                    subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                )
            }
        }

        // Subjects graph (unchanged)
        navigation(
            startDestination = "subjectList",
            route = "subjects_graph"
        ) {
            composable("subjectList") { SubjectListScreen(navController = navController) }
            composable("addEditSubject") {
                AddEditSubjectScreen(navController = navController, subjectId = null, initialName = null)
            }
            composable(
                "addEditSubject/{subjectId}/{subjectName}",
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.IntType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AddEditSubjectScreen(
                    navController = navController,
                    subjectId = backStackEntry.arguments?.getInt("subjectId"),
                    initialName = backStackEntry.arguments?.getString("subjectName")
                )
            }
        }

        // ✅ ADD THIS NEW NAVIGATION GRAPH FOR ACCOUNTS
        navigation(
            startDestination = "accountList",
            route = "accounts_graph"
        ) {
            composable("accountList") { AccountListScreen(navController = navController) }
            // Add routes for "addEditAccount" here later if you need them.
        }
    }
}