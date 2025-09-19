package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatoryDetailsScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    username: String?,
    viewModel: SignatoryViewModel = viewModel()
) {
    val assignedSubjects by viewModel.assignedSubjects
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var showDeleteSubjectDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<AssignedSubject?>(null) }

    var showDeleteSectionDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<ClassSection?>(null) }
    var subjectOfSection by remember { mutableStateOf<AssignedSubject?>(null) }

    var expandedSubjectId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(signatoryId) {
        viewModel.fetchAssignedSubjects(signatoryId)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        if (backStackEntry?.destination?.route?.startsWith("signatoryDetails") == true) {
            viewModel.fetchAssignedSubjects(signatoryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details for $signatoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text(text = "Name: $signatoryName", style = MaterialTheme.typography.headlineSmall)
            Text(text = "ID: $signatoryId", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("assignSubjectToSignatory/$signatoryId/$signatoryName") },
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
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                        IconButton(onClick = { navController.navigate("assignClassesToSubject/$signatoryId/$signatoryName/${subject.subjectId}/${subject.subjectName}") }) {
                                            Icon(Icons.Default.Add, "Assign Class")
                                        }
                                        IconButton(onClick = {
                                            subjectToDelete = subject
                                            showDeleteSubjectDialog = true
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete Subject Assignment")
                                        }
                                        IconButton(onClick = {
                                            if (expandedSubjectId == subject.subjectId) {
                                                expandedSubjectId = null
                                            } else {
                                                if (!assignedSections.containsKey(subject.subjectId)) {
                                                    viewModel.fetchAssignedSections(signatoryId, subject.subjectId)
                                                }
                                                expandedSubjectId = subject.subjectId
                                            }
                                        }) {
                                            Icon(if (expandedSubjectId == subject.subjectId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Expand")
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = expandedSubjectId == subject.subjectId) {
                                    val sectionsForSubject = assignedSections[subject.subjectId]
                                    when {
                                        sectionsForSubject == null -> {
                                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        sectionsForSubject.isEmpty() -> {
                                            Text("No classes assigned yet.", modifier = Modifier.padding(top = 8.dp))
                                        }
                                        else -> {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sectionsForSubject.forEach { section ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                navController.navigate("clearanceScreen/${section.sectionId}/${subject.subjectId}/${section.gradeLevel}/${section.sectionName}/${subject.subjectName}")
                                                            }
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${section.gradeLevel} - ${section.sectionName}",
                                                            modifier = Modifier.padding(start = 16.dp)
                                                        )
                                                        IconButton(onClick = {
                                                            sectionToDelete = section
                                                            subjectOfSection = subject
                                                            showDeleteSectionDialog = true
                                                        }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete Section Assignment")
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

    if (showDeleteSubjectDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSubjectDialog = false },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete the assignment for ${subjectToDelete?.subjectName}? This will un-assign all sections for this subject.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToDelete?.let { subject ->
                            viewModel.deleteAssignedSubject(signatoryId, subject.subjectId)
                        }
                        showDeleteSubjectDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteSectionDialog && sectionToDelete != null && subjectOfSection != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSectionDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to un-assign section '${sectionToDelete?.sectionName}' from '${subjectOfSection?.subjectName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAssignedSection(
                            signatoryId = signatoryId,
                            subjectId = subjectOfSection!!.subjectId,
                            sectionId = sectionToDelete!!.sectionId,
                            onSuccess = {
                                Toast.makeText(context, "Assignment deleted.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                        showDeleteSectionDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}