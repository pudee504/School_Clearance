package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AssignedSubject
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
    val context = LocalContext.current
    val assignedSubjects by viewModel.assignedSubjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // ✅ State for managing the confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    var subjectToUnassign by remember { mutableStateOf<AssignedSubject?>(null) }


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
            Text(signatoryName, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

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
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else if (assignedSubjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects have been assigned yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(assignedSubjects) { subject ->
                        SubjectListItem(
                            subject = subject,
                            onUnassignClicked = {
                                subjectToUnassign = subject
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ✅ Confirmation Dialog Composable
    if (showConfirmDialog && subjectToUnassign != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                subjectToUnassign = null
            },
            title = { Text("Confirm Unassignment") },
            text = { Text("Are you sure you want to unassign '${subjectToUnassign?.subjectName}' from this signatory?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToUnassign?.let {
                            viewModel.unassignSubject(
                                signatoryId = signatoryId,
                                subjectId = it.subjectId,
                                onSuccess = {
                                    Toast.makeText(context, "Subject unassigned", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                        showConfirmDialog = false
                        subjectToUnassign = null
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        subjectToUnassign = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ✅ Extracted list item into its own composable for clarity
@Composable
fun SubjectListItem(subject: AssignedSubject, onUnassignClicked: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subject.subjectName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassign") },
                        onClick = {
                            onUnassignClicked()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}