// com/mnvths/schoolclearance/screen/AssignedSectionsScreen.kt
package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedSectionsScreen(
    navController: NavController,
    signatoryId: Int,
    subjectId: Int,
    subjectName: String,
    viewModel: SignatoryViewModel = viewModel()
) {
    val sections by viewModel.sectionsForSubject
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(signatoryId, subjectId) {
        viewModel.fetchSectionsForSubject(signatoryId, subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        // ✅ ADD THE FLOATING ACTION BUTTON
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to the new screen, passing the necessary arguments
                    navController.navigate("assignSectionsToSubject/${signatoryId}/${subjectId}/${subjectName}")
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Assign Section")
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
                        Text("No sections have been assigned to this subject yet.")
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections, key = { it.sectionId }) { section ->
                            SectionListItem(section = section)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionListItem(section: ClassSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${section.gradeLevel} - ${section.sectionName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}