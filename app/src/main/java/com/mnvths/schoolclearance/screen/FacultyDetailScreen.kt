package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.mnvths.schoolclearance.data.AssignedSignatory
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.FacultyViewModel

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
    val context = LocalContext.current

    // State for deleting a whole signatory assignment
    var showDeleteSignatoryDialog by remember { mutableStateOf(false) }
    var signatoryToDelete by remember { mutableStateOf<AssignedSignatory?>(null) }

    // State for deleting a single section assignment
    var showDeleteSectionDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<ClassSection?>(null) }
    var signatoryOfSection by remember { mutableStateOf<AssignedSignatory?>(null) }


    var expandedSignatoryId by remember { mutableStateOf<Int?>(null) }
    val sectionsLoading = remember { mutableStateOf(mapOf<Int, Boolean>()) }

    // Fetches signatories when the screen is first loaded
    LaunchedEffect(facultyId) {
        viewModel.fetchAssignedSignatories(facultyId)
    }

    // Refreshes signatories when returning to this screen (e.g., from "Assign Signatory")
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        if (backStackEntry?.destination?.route?.startsWith("facultyDetails") == true) {
            viewModel.fetchAssignedSignatories(facultyId)
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
                        Card(modifier = Modifier.fillMaxWidth()) {
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
                                        IconButton(onClick = { navController.navigate("assignClassesToSignatory/$facultyId/$facultyName/${signatory.signatoryId}/${signatory.signatoryName}") }) {
                                            Icon(Icons.Default.Add, "Assign Class")
                                        }
                                        IconButton(onClick = {
                                            signatoryToDelete = signatory
                                            showDeleteSignatoryDialog = true
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete Signatory")
                                        }
                                        IconButton(onClick = {
                                            if (expandedSignatoryId == signatory.signatoryId) {
                                                expandedSignatoryId = null
                                            } else {
                                                if (!assignedSections.containsKey(signatory.signatoryId)) {
                                                    viewModel.fetchAssignedSections(facultyId, signatory.signatoryId)
                                                }
                                                expandedSignatoryId = signatory.signatoryId
                                            }
                                        }) {
                                            Icon(if (expandedSignatoryId == signatory.signatoryId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Expand")
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = expandedSignatoryId == signatory.signatoryId) {
                                    val sectionsForSignatory = assignedSections[signatory.signatoryId]
                                    when {
                                        sectionsForSignatory == null -> {
                                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        sectionsForSignatory.isEmpty() -> {
                                            Text("No classes assigned yet.", modifier = Modifier.padding(top = 8.dp))
                                        }
                                        else -> {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sectionsForSignatory.forEach { section ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                navController.navigate("clearanceScreen/${section.sectionId}/${signatory.signatoryId}/${section.gradeLevel}/${section.sectionName}/${signatory.signatoryName}")
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
                                                            signatoryOfSection = signatory // Remember which signatory it belongs to
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

    // Dialog for deleting a whole signatory
    if (showDeleteSignatoryDialog && signatoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSignatoryDialog = false },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete the assignment for ${signatoryToDelete?.signatoryName}? This will un-assign all sections for this subject.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        signatoryToDelete?.let { signatory ->
                            viewModel.deleteAssignedSignatory(facultyId, signatory.signatoryId)
                        }
                        showDeleteSignatoryDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSignatoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for deleting a single section assignment
    if (showDeleteSectionDialog && sectionToDelete != null && signatoryOfSection != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSectionDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to un-assign section '${sectionToDelete?.sectionName}' from '${signatoryOfSection?.signatoryName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAssignedSection(
                            facultyId = facultyId,
                            signatoryId = signatoryOfSection!!.signatoryId,
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