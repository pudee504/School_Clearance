package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.viewmodel.FacultyViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FacultyDashboard(user: OtherUser, onSignOut: () -> Unit, navController: NavController) {
    // This NavController is for the faculty's internal navigation
    val facultyNavController = rememberNavController()

    NavHost(navController = facultyNavController, startDestination = "faculty_main") {
        composable("faculty_main") {
            FacultyMainScreen(
                user = user,
                onSignOut = onSignOut,
                navController = facultyNavController
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
                navController = facultyNavController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FacultyMainScreen(
    user: OtherUser,
    onSignOut: () -> Unit,
    navController: NavHostController,
    viewModel: FacultyViewModel = viewModel()
) {
    // ✅ FIX: Changed from .collectAsState() to direct delegation 'by'
    // This now correctly matches the implementation in your other screens.
    val assignedSignatories by viewModel.assignedSignatories
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading

    var expandedSignatoryId by remember { mutableStateOf<Int?>(null) }

    // Fetch the faculty's assigned signatories when the screen loads
    LaunchedEffect(user.id) {
        viewModel.fetchAssignedSignatories(user.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faculty Dashboard") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Name: ${user.name}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Faculty ID: ${user.id}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "My Assigned Signatories:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (assignedSignatories.isEmpty()) {
                Text("You have no signatories assigned to you yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignedSignatories) { signatory ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = signatory.signatoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        expandedSignatoryId = if (expandedSignatoryId == signatory.signatoryId) {
                                            null
                                        } else {
                                            viewModel.fetchAssignedSections(user.id, signatory.signatoryId)
                                            signatory.signatoryId
                                        }
                                    }) {
                                        Icon(
                                            if (expandedSignatoryId == signatory.signatoryId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand"
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = expandedSignatoryId == signatory.signatoryId) {
                                    val sections = assignedSections[signatory.signatoryId]
                                    when {
                                        sections == null -> {
                                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        sections.isEmpty() -> {
                                            Text("No sections assigned.", modifier = Modifier.padding(top = 8.dp))
                                        }
                                        else -> {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sections.forEach { section ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                navController.navigate("clearanceScreen/${section.sectionId}/${signatory.signatoryId}/${section.gradeLevel}/${section.sectionName}/${signatory.signatoryName}")
                                                            }
                                                            .padding(vertical = 8.dp, horizontal = 16.dp),
                                                    ) {
                                                        Text(text = "${section.gradeLevel} - ${section.sectionName}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}