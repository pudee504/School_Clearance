package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AdminStudentProfile
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.ClearanceStatusItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentDetailScreen(
    navController: NavController,
    studentId: String,
    viewModel: StudentManagementViewModel = viewModel(),
    appSettings: AppSettings
) {
    val profile by viewModel.adminStudentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingClearanceChange by remember { mutableStateOf<Pair<ClearanceStatusItem, Boolean>?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    LaunchedEffect(studentId, appSettings) {
        viewModel.fetchAdminStudentProfile(studentId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAdminStudentProfile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    profile?.let { p ->
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        navController.navigate("editStudent/${p.id}")
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showDeleteDialog = true
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && profile == null) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(
                    text = "An error occurred: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                profile?.let { p ->
                    val subjects = remember(p.clearanceStatus) {
                        p.clearanceStatus.filter { it.requirementType == "subject" }
                    }
                    val accounts = remember(p.clearanceStatus) {
                        p.clearanceStatus.filter { it.requirementType == "account" }
                    }

                    val allCleared = remember(p.clearanceStatus) {
                        p.clearanceStatus.isNotEmpty() && p.clearanceStatus.all { it.isCleared }
                    }

                    val (adviserItem, isAdviserClearable) = remember(p.clearanceStatus) {
                        val adviser = p.clearanceStatus.find { it.signatoryName == "Class Adviser" }
                        val principal = p.clearanceStatus.find { it.signatoryName == "Principal" }
                        val prerequisites = p.clearanceStatus.filter { it.requirementId != adviser?.requirementId && it.requirementId != principal?.requirementId }
                        val allPrerequisitesCleared = prerequisites.all { it.isCleared }
                        adviser to allPrerequisitesCleared
                    }

                    val (principalItem, isPrincipalClearable) = remember(p.clearanceStatus) {
                        val principal = p.clearanceStatus.find { it.signatoryName == "Principal" }
                        val prerequisites = p.clearanceStatus.filter { it.requirementId != principal?.requirementId }
                        val allPrerequisitesCleared = prerequisites.all { it.isCleared }
                        principal to allPrerequisitesCleared
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StudentInfo(label = "Name", value = p.name)
                        StudentInfo(label = "LRN/ID", value = p.id)
                        StudentInfo(label = "Grade Level", value = p.gradeLevel ?: "N/A")
                        StudentInfo(label = "Section", value = p.section ?: "N/A")
                        StudentInfo(label = "School Year", value = p.activeTerm.schoolYear)
                        StudentInfo(label = "Term", value = "${p.activeTerm.termName} ${p.activeTerm.termNumber}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()

                        if (p.clearanceStatus.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("No clearance requirements found for this student.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                item {
                                    Text("Subjects", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                items(subjects, key = { "subject-${it.requirementId}" }) { item ->
                                    val isEnabled = when (item.requirementId) {
                                        adviserItem?.requirementId -> isAdviserClearable || item.isCleared
                                        principalItem?.requirementId -> isPrincipalClearable || item.isCleared
                                        else -> true
                                    }

                                    ClearanceRow(
                                        item = item,
                                        onStatusChange = { newStatus ->
                                            pendingClearanceChange = item to newStatus
                                            showConfirmationDialog = true
                                        },
                                        enabled = isEnabled
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(16.dp)); Divider()
                                    Text("Accounts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                items(accounts, key = { "account-${it.requirementId}" }) { item ->
                                    ClearanceRow(
                                        item = item,
                                        onStatusChange = { newStatus ->
                                            pendingClearanceChange = item to newStatus
                                            showConfirmationDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        if (allCleared) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All Requirements Cleared",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF008000)
                                )
                            }
                        }
                    }
                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Could not load student profile.")
                }
            }

            if (showDeleteDialog) {
                profile?.let { p ->
                    DeleteStudentConfirmationDialog(
                        studentName = p.name,
                        onConfirm = {
                            viewModel.deleteStudent(
                                studentId = p.id,
                                onSuccess = {
                                    Toast.makeText(context, "${p.name} deleted.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                            )
                            showDeleteDialog = false
                        },
                        onDismiss = { showDeleteDialog = false }
                    )
                }
            }

            if (showConfirmationDialog && pendingClearanceChange != null) {
                val (item, newStatus) = pendingClearanceChange!!
                ConfirmationDialog(
                    // ✅ FIX IS HERE: Use requirementName instead of signatoryName
                    itemName = item.requirementName ?: "this requirement",
                    newStatus = newStatus,
                    onConfirm = {
                        profile?.let { p ->
                            viewModel.updateClearanceStatus(
                                isCleared = newStatus, item = item, profile = p,
                                onSuccess = {
                                    val action = if (newStatus) "cleared" else "marked as not cleared"
                                    // Use requirementName here as well for a clearer message
                                    Toast.makeText(context, "${item.requirementName ?: "Item"} has been $action.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                            )
                        }
                        showConfirmationDialog = false
                        pendingClearanceChange = null
                    },
                    onDismiss = {
                        showConfirmationDialog = false
                        pendingClearanceChange = null
                    }
                )
            }
        }
    }
}

@Composable
fun StudentInfo(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ClearanceRow(
    item: ClearanceStatusItem,
    onStatusChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(
                imageVector = if (item.isCleared) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                contentDescription = if (item.isCleared) "Cleared" else "Not Cleared",
                tint = if (item.isCleared) Color(0xFF008000) else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(16.dp))

            Column {
                Text(item.requirementName ?: "Invalid Requirement", style = MaterialTheme.typography.bodyLarge)

                // ✅ MODIFIED: Removed the "Handled by: " prefix
                Text(
                    text = item.signatoryName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = item.isCleared,
            onCheckedChange = onStatusChange,
            enabled = enabled
        )
    }
}

@Composable
fun ConfirmationDialog(
    itemName: String,
    newStatus: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionText = if (newStatus) "clear" else "mark as not cleared"
    val titleText = if (newStatus) "Confirm Clearance" else "Confirm Undo"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = { Text("Are you sure you want to $actionText '${itemName}'?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (!newStatus) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer) else ButtonDefaults.buttonColors()
            ) {
                Text("Confirm", color = if (!newStatus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}