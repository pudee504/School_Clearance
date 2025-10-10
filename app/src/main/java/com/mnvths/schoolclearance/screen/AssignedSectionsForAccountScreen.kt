package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedSectionsForAccountScreen(
    navController: NavController,
    signatoryId: Int,
    accountId: Int,
    accountName: String,
    destinationRoute: String,
    showFab: Boolean = true,
    viewModel: SignatoryViewModel = viewModel()
) {
    val sections by viewModel.sectionsForAccount
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(signatoryId, accountId) {
        viewModel.fetchSectionsForAccount(signatoryId, accountId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFab) { // ✅ WRAP THE BUTTON IN THIS IF-STATEMENT
                FloatingActionButton(
                    onClick = {
                        navController.navigate("assignSectionsToAccount/${signatoryId}/${accountId}/${accountName}")
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Assign Section")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Assigned Sections", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                sections.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sections have been assigned to this account yet.")
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections, key = { it.sectionId }) { section ->
                            SectionListItem(
                                section = section,
                                onClick = {
                                    // ✅ USE THE NEW PARAMETER HERE
                                    val route = "$destinationRoute/${section.sectionId}/${accountId}/${section.sectionName}/${accountName}/${section.gradeLevel}"
                                    navController.navigate(route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}