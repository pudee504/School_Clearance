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
import com.mnvths.schoolclearance.AssignedSubject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailsScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    viewModel: FacultyViewModel = viewModel()
) {
    val assignedSubjects by viewModel.assignedSubjects
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<AssignedSubject?>(null) }


    // This state tracks the currently expanded subject
    var expandedSubjectId by remember { mutableStateOf<Int?>(null) }
    // This map tracks the loading state for sections for each subject
    val sectionsLoading = remember { mutableStateOf(mapOf<Int, Boolean>()) }

    // Use a LaunchedEffect to fetch the initial subjects for the faculty member
    LaunchedEffect(facultyId) {
        viewModel.fetchAssignedSubjects(facultyId)
    }

    // This new LaunchedEffect will trigger a refresh when we navigate back to this screen
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        // We only want to refresh if we're on this screen
        if (backStackEntry?.destination?.route?.startsWith("facultyDetails") == true) {
            viewModel.fetchAssignedSubjects(facultyId)
        }
    }

    // Use a LaunchedEffect to observe changes in the assignedSections map
    // and update the per-subject loading state accordingly.
    LaunchedEffect(assignedSections) {
        assignedSections.forEach { (subjectId, sections) ->
            if (sectionsLoading.value[subjectId] == true) {
                // When sections are loaded, turn off the loading indicator for this subject
                sectionsLoading.value = sectionsLoading.value.toMutableMap().apply {
                    this[subjectId] = false
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
                onClick = { navController.navigate("assignSubjectToFaculty/$facultyId/$facultyName") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign New Subject")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Assigned Subjects:", style = MaterialTheme.typography.titleLarge)
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
                    items(assignedSubjects) { subject ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Subject Name, Assign and Delete Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subject.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Assign Class IconButton
                                        IconButton(
                                            onClick = {
                                                navController.navigate("assignClassesToSubject/$facultyId/$facultyName/${subject.subjectId}/${subject.subjectName}")
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Assign Class"
                                            )
                                        }
                                        // Delete Subject IconButton
                                        IconButton(
                                            onClick = {
                                                subjectToDelete = subject
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Subject"
                                            )
                                        }
                                        // Expansion Icon to show sections
                                        IconButton(
                                            onClick = {
                                                if (expandedSubjectId == subject.subjectId) {
                                                    expandedSubjectId = null // Collapse on second click
                                                } else {
                                                    // This is the core fix: Check if the sections have already been loaded.
                                                    if (!assignedSections.containsKey(subject.subjectId)) {
                                                        // If not, set the loading state and fetch the data.
                                                        sectionsLoading.value = sectionsLoading.value.toMutableMap().apply {
                                                            this[subject.subjectId] = true
                                                        }
                                                        viewModel.fetchAssignedSections(
                                                            facultyId,
                                                            subject.subjectId
                                                        )
                                                    }
                                                    // Always expand the subject.
                                                    expandedSubjectId = subject.subjectId
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (expandedSubjectId == subject.subjectId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand or collapse"
                                            )
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = expandedSubjectId == subject.subjectId) {
                                    val sectionsForSubject = assignedSections[subject.subjectId]

                                    // Display content based on the state of the sections
                                    when {
                                        sectionsLoading.value[subject.subjectId] == true -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.padding(
                                                    top = 8.dp
                                                ).align(Alignment.CenterHorizontally)
                                            )
                                        }

                                        sectionsForSubject.isNullOrEmpty() -> {
                                            Text(
                                                text = "No classes assigned yet.",
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }

                                        else -> {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sectionsForSubject.forEach { section ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${section.gradeLevel} - ${section.sectionName}",
                                                            modifier = Modifier.padding(start = 16.dp)
                                                        )
                                                        // TODO: Add delete button for individual section
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

    // Confirmation dialog for deleting a subject
    if (showDeleteDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside or presses back
                showDeleteDialog = false
                subjectToDelete = null
            },
            title = {
                Text(text = "Confirm Deletion")
            },
            text = {
                Text(text = "Are you sure you want to delete the assignment for ${subjectToDelete?.subjectName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToDelete?.let { subject ->
                            viewModel.deleteAssignedSubject(facultyId, subject.subjectId)
                        }
                        showDeleteDialog = false
                        subjectToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        subjectToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
