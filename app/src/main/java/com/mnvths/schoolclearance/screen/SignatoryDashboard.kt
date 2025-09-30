package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SignatoryDashboard(user: OtherUser, onSignOut: () -> Unit, navController: NavController) {
    val signatoryNavController = rememberNavController()

    NavHost(navController = signatoryNavController, startDestination = "signatory_main") {
        composable("signatory_main") {
            SignatoryMainScreen(
                user = user,
                onSignOut = onSignOut,
                navController = signatoryNavController
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
                navController = signatoryNavController,
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
private fun SignatoryMainScreen(
    user: OtherUser,
    onSignOut: () -> Unit,
    navController: NavHostController,
    viewModel: SignatoryViewModel = viewModel()
) {
    // ✅ Use the corrected properties from the ViewModel
    val assignedItems by viewModel.assignedItems
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading
    var expandedItemId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(user.id) {
        // ✅ Call the corrected function
        viewModel.fetchAssignedItems(user.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signatory Dashboard") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sign Out")
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
            Text(text = "ID: ${user.id}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "My Assigned Items:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (assignedItems.isEmpty()) {
                Text("You have no items assigned to you yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignedItems, key = { it.assignmentId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // ✅ Use item.name and display item.type
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = item.type,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light
                                        )
                                    }

                                    // ✅ Only show the expand button if the item is a "Subject"
                                    if (item.type == "Subject") {
                                        IconButton(onClick = {
                                            expandedItemId = if (expandedItemId == item.itemId) {
                                                null
                                            } else {
                                                viewModel.fetchAssignedSections(user.id, item.itemId)
                                                item.itemId
                                            }
                                        }) {
                                            Icon(
                                                if (expandedItemId == item.itemId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand"
                                            )
                                        }
                                    }
                                }

                                // ✅ Animated visibility now works for subjects only
                                if (item.type == "Subject") {
                                    AnimatedVisibility(visible = expandedItemId == item.itemId) {
                                        val sections = assignedSections[item.itemId]
                                        when {
                                            sections == null -> Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                            sections.isEmpty() -> Text("No sections assigned.", modifier = Modifier.padding(top = 8.dp))
                                            else -> {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    sections.forEach { section ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    navController.navigate("clearanceScreen/${section.sectionId}/${item.itemId}/${section.gradeLevel}/${section.sectionName}/${item.name}")
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
}