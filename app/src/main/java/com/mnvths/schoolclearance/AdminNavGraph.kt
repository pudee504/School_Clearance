// AdminNavGraph.kt
package com.mnvths.schoolclearance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.screen.AddFacultyScreen
import com.mnvths.schoolclearance.screen.AddSectionScreen
import com.mnvths.schoolclearance.screen.AddStudentScreen
import com.mnvths.schoolclearance.screen.AssignClassesToSignatoryScreen
import com.mnvths.schoolclearance.screen.AssignSignatoryToFacultyScreen
import com.mnvths.schoolclearance.screen.EditFacultyScreen
import com.mnvths.schoolclearance.screen.EditSectionScreen
import com.mnvths.schoolclearance.screen.EditStudentScreen
import com.mnvths.schoolclearance.screen.FacultyDetailsScreen
import com.mnvths.schoolclearance.screen.FacultyListScreen
import com.mnvths.schoolclearance.screen.SignatoryListScreen
import com.mnvths.schoolclearance.screen.StudentListScreen
import com.mnvths.schoolclearance.screen.StudentManagementScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "studentManagement"
    ) {
        // Main tabs
        composable("studentManagement") {
            StudentManagementScreen(navController = navController)
        }
        composable("facultyList") {
            FacultyListScreen(navController = navController)
        }
        composable("signatories") {
            SignatoryListScreen(navController = navController)
        }

        // All sub-screens for the admin flow
        composable("addSection") {
            AddSectionScreen(navController = navController)
        }
        composable("addFaculty") {
            AddFacultyScreen(navController = navController)
        }

        // In your NavHost
        composable(
            "editStudent/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            EditStudentScreen(
                navController = navController,
                studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            )
        }

        composable(
            "editSection/{sectionId}/{gradeLevel}/{sectionName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0
            val gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: ""
            val sectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
            EditSectionScreen(
                navController = navController,
                sectionId = sectionId,
                initialGradeLevel = gradeLevel,
                initialSectionName = sectionName
            )
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