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
import com.mnvths.schoolclearance.data.AssignedAccount
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
    // ✅ Get assigned accounts from the view model
    val assignedAccounts by viewModel.assignedAccounts
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // ✅ State for managing the new assignment menu
    var showAssignMenu by remember { mutableStateOf(false) }

    // State for managing unassignment dialogs
    var showUnassignSubjectDialog by remember { mutableStateOf(false) }
    var subjectToUnassign by remember { mutableStateOf<AssignedSubject?>(null) }
    var showUnassignAccountDialog by remember { mutableStateOf(false) }
    var accountToUnassign by remember { mutableStateOf<AssignedAccount?>(null) }


    LaunchedEffect(signatoryId) {
        // ✅ Fetch both subjects and accounts when the screen loads
        viewModel.fetchAssignedSubjects(signatoryId)
        viewModel.fetchAssignedAccounts(signatoryId)
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
            // ✅ Wrap FAB in a Box to anchor the dropdown menu
            Box {
                FloatingActionButton(
                    onClick = { showAssignMenu = true }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Assign New")
                }

                DropdownMenu(
                    expanded = showAssignMenu,
                    onDismissRequest = { showAssignMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Assign Subject") },
                        onClick = {
                            navController.navigate("assignSubjectToSignatory/$signatoryId/$signatoryName")
                            showAssignMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Assign Account") },
                        onClick = {
                            navController.navigate("assignAccountToSignatory/$signatoryId/$signatoryName")
                            showAssignMenu = false
                        }
                    )
                }
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
            } else {
                // Using a Column with two LazyColumns for simplicity
                // For very long lists, a single LazyColumn with different item types would be more performant
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Assigned Subjects
                    item {
                        Text(
                            text = "Assigned Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (assignedSubjects.isEmpty()) {
                        item { Text("No subjects have been assigned yet.", modifier = Modifier.padding(bottom = 16.dp)) }
                    } else {
                        items(assignedSubjects) { subject ->
                            SubjectListItem(
                                subject = subject,
                                onUnassignClicked = {
                                    subjectToUnassign = subject
                                    showUnassignSubjectDialog = true
                                },
                                // ✅ PASS THE NAVIGATION ACTION
                                onItemClicked = {
                                    navController.navigate("assignedSections/${signatoryId}/${subject.subjectId}/${subject.subjectName}")
                                }
                            )
                        }
                    }

                    // Spacer between sections
                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // ✅ Assigned Accounts
                    item {
                        Text(
                            text = "Assigned Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (assignedAccounts.isEmpty()) {
                        item { Text("No accounts have been assigned yet.") }
                    } else {
                        items(assignedAccounts) { account ->
                            AccountListItem(
                                account = account,
                                onUnassignClicked = {
                                    accountToUnassign = account
                                    showUnassignAccountDialog = true
                                },
                                // ✅ REPLACE the Toast with this navigation call
                                onItemClicked = {
                                    navController.navigate("assignedSectionsForAccount/${signatoryId}/${account.accountId}/${account.accountName}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Unassigning a Subject
    if (showUnassignSubjectDialog && subjectToUnassign != null) {
        AlertDialog(
            onDismissRequest = { showUnassignSubjectDialog = false },
            title = { Text("Confirm Unassignment") },
            text = { Text("Are you sure you want to unassign '${subjectToUnassign?.subjectName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToUnassign?.let {
                            viewModel.unassignSubject(
                                signatoryId = signatoryId,
                                subjectId = it.subjectId,
                                onSuccess = { Toast.makeText(context, "Subject unassigned", Toast.LENGTH_SHORT).show() },
                                onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                            )
                        }
                        showUnassignSubjectDialog = false
                    }
                ) { Text("Confirm", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showUnassignSubjectDialog = false }) { Text("Cancel") } }
        )
    }

    // ✅ Confirmation Dialog for Unassigning an Account
    if (showUnassignAccountDialog && accountToUnassign != null) {
        AlertDialog(
            onDismissRequest = { showUnassignAccountDialog = false },
            title = { Text("Confirm Unassignment") },
            text = { Text("Are you sure you want to unassign '${accountToUnassign?.accountName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToUnassign?.let {
                            viewModel.unassignAccount(
                                signatoryId = signatoryId,
                                accountId = it.accountId,
                                onSuccess = { Toast.makeText(context, "Account unassigned", Toast.LENGTH_SHORT).show() },
                                onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                            )
                        }
                        showUnassignAccountDialog = false
                    }
                ) { Text("Confirm", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showUnassignAccountDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SubjectListItem(
    subject: AssignedSubject,
    onUnassignClicked: () -> Unit,
    onItemClicked: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onItemClicked, // ✅ Make the card clickable
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = subject.subjectName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Unassign") }, onClick = { onUnassignClicked(); menuExpanded = false })
                }
            }
        }
    }
}

// ✅ New Composable for displaying an assigned account item
@Composable
fun AccountListItem(
    account: AssignedAccount,
    onUnassignClicked: () -> Unit,
    onItemClicked: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onItemClicked, // ✅ Make the card clickable
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = account.accountName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Unassign") }, onClick = { onUnassignClicked(); menuExpanded = false })
                }
            }
        }
    }
}