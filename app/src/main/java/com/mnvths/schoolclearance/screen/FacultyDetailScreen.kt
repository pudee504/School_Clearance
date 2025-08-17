package com.mnvths.schoolclearance.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.FacultyViewModel
import com.mnvths.schoolclearance.AssignedSignatory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailsScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    username: String?,
    viewModel: FacultyViewModel = viewModel()
) {
    val assignedSignatories by viewModel.assignedSignatories
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var showDeleteDialog by remember { mutableStateOf(false) }
    var signatoryToDelete by remember { mutableStateOf<AssignedSignatory?>(null) }

    var expandedSignatoryId by remember { mutableStateOf<Int?>(null) }
    val sectionsLoading = remember { mutableStateOf(mapOf<Int, Boolean>()) }

    LaunchedEffect(facultyId) {
        viewModel.fetchAssignedSignatories(facultyId)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        if (backStackEntry?.destination?.route?.startsWith("facultyDetails") == true) {
            viewModel.fetchAssignedSignatories(facultyId)
        }
    }

    LaunchedEffect(assignedSections) {
        assignedSections.forEach { (signatoryId, sections) ->
            if (sectionsLoading.value[signatoryId] == true) {
                sectionsLoading.value = sectionsLoading.value.toMutableMap().apply {
                    this[signatoryId] = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details for $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Text(text = "Name: $facultyName", style = MaterialTheme.typography.headlineSmall)
            Text(text = "ID: $facultyId", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("assignSignatoryToFaculty/$facultyId/$facultyName") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign New Signatory")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Assigned Signatories:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignedSignatories) { signatory ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = signatory.signatoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                navController.navigate("assignClassesToSignatory/$facultyId/$facultyName/${signatory.signatoryId}/${signatory.signatoryName}")
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Assign Class"
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                signatoryToDelete = signatory
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Signatory"
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (expandedSignatoryId == signatory.signatoryId) {
                                                    expandedSignatoryId = null
                                                } else {
                                                    if (!assignedSections.containsKey(signatory.signatoryId)) {
                                                        sectionsLoading.value = sectionsLoading.value.toMutableMap().apply {
                                                            this[signatory.signatoryId] = true
                                                        }
                                                        viewModel.fetchAssignedSections(
                                                            facultyId,
                                                            signatory.signatoryId
                                                        )
                                                    }
                                                    expandedSignatoryId = signatory.signatoryId
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (expandedSignatoryId == signatory.signatoryId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand or collapse"
                                            )
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = expandedSignatoryId == signatory.signatoryId) {
                                    val sectionsForSignatory = assignedSections[signatory.signatoryId]

                                    when {
                                        sectionsLoading.value[signatory.signatoryId] == true -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.padding(
                                                    top = 8.dp
                                                ).align(Alignment.CenterHorizontally)
                                            )
                                        }

                                        sectionsForSignatory.isNullOrEmpty() -> {
                                            Text(
                                                text = "No classes assigned yet.",
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }

                                        else -> {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sectionsForSignatory.forEach { section ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${section.gradeLevel} - ${section.sectionName}",
                                                            modifier = Modifier.padding(start = 16.dp)
                                                        )
                                                        IconButton(onClick = { /* TODO: Implement delete section logic */ }) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete Section"
                                                            )
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

    if (showDeleteDialog && signatoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                signatoryToDelete = null
            },
            title = {
                Text(text = "Confirm Deletion")
            },
            text = {
                Text(text = "Are you sure you want to delete the assignment for ${signatoryToDelete?.signatoryName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        signatoryToDelete?.let { signatory ->
                            viewModel.deleteAssignedSignatory(facultyId, signatory.signatoryId)
                        }
                        showDeleteDialog = false
                        signatoryToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        signatoryToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}