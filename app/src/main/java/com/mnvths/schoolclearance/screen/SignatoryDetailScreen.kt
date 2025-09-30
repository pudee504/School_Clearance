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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatoryDetailsScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    username: String,
    viewModel: SignatoryViewModel = viewModel()
) {
    val assignedSubjects by viewModel.assignedSubjects
    val isLoading by viewModel.isLoading

    LaunchedEffect(signatoryId) {
        viewModel.fetchAssignedSubjects(signatoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signatory Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("assignSubjectToSignatory/$signatoryId/$signatoryName")
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Assign New Subject")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Signatory Information
            Text(signatoryName, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Assigned Subjects List
            Text(
                text = "Assigned Subjects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (assignedSubjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects have been assigned yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(assignedSubjects) { subject ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = subject.subjectName,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}