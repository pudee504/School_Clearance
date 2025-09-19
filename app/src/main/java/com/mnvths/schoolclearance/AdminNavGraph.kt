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

        // ✅ NEW GRAPH 2: All screens related to Sections
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

        // ✅ GRAPH 2: All screens related to Faculty
        navigation(
            startDestination = "facultyList",
            route = "faculty_graph"
        ) {
            composable("facultyList") { FacultyListScreen(navController = navController) }
            composable("addFaculty") { AddFacultyScreen(navController = navController) }
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
                FacultyDetailsScreen(
                    navController = navController,
                    facultyId = backStackEntry.arguments?.getInt("facultyId") ?: 0,
                    facultyName = backStackEntry.arguments?.getString("facultyName") ?: "",
                    firstName = backStackEntry.arguments?.getString("firstName") ?: "",
                    lastName = backStackEntry.arguments?.getString("lastName") ?: "",
                    middleName = backStackEntry.arguments?.getString("middleName"),
                    username = backStackEntry.arguments?.getString("username") ?: ""
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
                EditFacultyScreen(
                    navController = navController,
                    facultyId = backStackEntry.arguments?.getInt("facultyId") ?: 0,
                    facultyName = backStackEntry.arguments?.getString("facultyName") ?: "",
                    firstName = backStackEntry.arguments?.getString("firstName") ?: "",
                    lastName = backStackEntry.arguments?.getString("lastName") ?: "",
                    middleName = backStackEntry.arguments?.getString("middleName"),
                    username = backStackEntry.arguments?.getString("username") ?: ""
                )
            }
            composable(
                route = "assignSignatoryToFaculty/{facultyId}/{facultyName}",
                arguments = listOf(
                    navArgument("facultyId") { type = NavType.IntType },
                    navArgument("facultyName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AssignSignatoryToFacultyScreen(
                    navController = navController,
                    facultyId = backStackEntry.arguments?.getInt("facultyId") ?: 0,
                    facultyName = backStackEntry.arguments?.getString("facultyName") ?: ""
                )
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
                AssignClassesToSignatoryScreen(
                    navController = navController,
                    facultyId = backStackEntry.arguments?.getInt("facultyId") ?: 0,
                    signatoryId = backStackEntry.arguments?.getInt("signatoryId") ?: 0,
                    signatoryName = backStackEntry.arguments?.getString("signatoryName") ?: ""
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

        // ✅ GRAPH 3: All screens related to Signatories
        navigation(
            startDestination = "signatoryList",
            route = "signatories_graph"
        ) {
            composable("signatoryList") { SignatoryListScreen(navController = navController) }
            composable("addEditSignatory") {
                AddEditSignatoryScreen(navController = navController, signatoryId = null, initialName = null)
            }
            composable(
                "addEditSignatory/{signatoryId}/{signatoryName}",
                arguments = listOf(
                    navArgument("signatoryId") { type = NavType.IntType },
                    navArgument("signatoryName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                AddEditSignatoryScreen(
                    navController = navController,
                    signatoryId = backStackEntry.arguments?.getInt("signatoryId"),
                    initialName = backStackEntry.arguments?.getString("signatoryName")
                )
            }
        }
    }
}