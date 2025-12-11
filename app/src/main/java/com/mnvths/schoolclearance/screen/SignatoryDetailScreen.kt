package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AssignedAccount
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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
    val assignedAccounts by viewModel.assignedAccounts
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Menu States
    var showAssignMenu by remember { mutableStateOf(false) }

    // Unassign Dialog States
    var showUnassignSubjectDialog by remember { mutableStateOf(false) }
    var subjectToUnassign by remember { mutableStateOf<AssignedSubject?>(null) }
    var showUnassignAccountDialog by remember { mutableStateOf(false) }
    var accountToUnassign by remember { mutableStateOf<AssignedAccount?>(null) }


    LaunchedEffect(signatoryId) {
        viewModel.fetchAssignedSubjects(signatoryId)
        viewModel.fetchAssignedAccounts(signatoryId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Signatory Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = SchoolBlue
                )
            )
        },
        containerColor = BackgroundGray,
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { showAssignMenu = true },
                    containerColor = SchoolBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Assign Duty") }
                )

                DropdownMenu(
                    expanded = showAssignMenu,
                    onDismissRequest = { showAssignMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Assign Subject") },
                        onClick = {
                            navController.navigate("assignSubjectToSignatory/$signatoryId/$signatoryName")
                            showAssignMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Book, null, tint = SchoolBlue) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Assign Account") },
                        onClick = {
                            navController.navigate("assignAccountToSignatory/$signatoryId/$signatoryName")
                            showAssignMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.AccountBalance, null, tint = SchoolBlue) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Profile Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SchoolBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = signatoryName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = signatoryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $error", color = SchoolRed)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Subjects Section ---
                    if (assignedSubjects.isNotEmpty()) {
                        item { HeaderLabel("Academic Subjects") }
                        items(assignedSubjects) { subject ->
                            ResponsibilityCard(
                                title = subject.subjectName,
                                subtitle = "Manage Sections",
                                icon = Icons.Outlined.Book,
                                onClick = {
                                    navController.navigate("assignedSections/${signatoryId}/${subject.subjectId}/${subject.subjectName}")
                                },
                                onUnassign = {
                                    subjectToUnassign = subject
                                    showUnassignSubjectDialog = true
                                }
                            )
                        }
                    }

                    // --- Accounts Section ---
                    if (assignedAccounts.isNotEmpty()) {
                        item { HeaderLabel("Administrative Accounts") }
                        items(assignedAccounts) { account ->
                            ResponsibilityCard(
                                title = account.accountName,
                                subtitle = "Manage Account Clearance",
                                icon = Icons.Outlined.AccountBalance,
                                onClick = {
                                    navController.navigate("assignedSectionsForAccount/${signatoryId}/${account.accountId}/${account.accountName}")
                                },
                                onUnassign = {
                                    accountToUnassign = account
                                    showUnassignAccountDialog = true
                                }
                            )
                        }
                    }

                    // Empty State
                    if (assignedSubjects.isEmpty() && assignedAccounts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No duties assigned yet.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                                Text("Tap 'Assign Duty' to get started.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showUnassignSubjectDialog && subjectToUnassign != null) {
        UnassignDialog(
            title = "Unassign Subject?",
            message = "Remove '${subjectToUnassign?.subjectName}' from this signatory?",
            onConfirm = {
                subjectToUnassign?.let {
                    viewModel.unassignSubject(
                        signatoryId = signatoryId,
                        subjectId = it.subjectId,
                        onSuccess = { Toast.makeText(context, "Subject unassigned", Toast.LENGTH_SHORT).show() },
                        onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                    )
                }
                showUnassignSubjectDialog = false
            },
            onDismiss = { showUnassignSubjectDialog = false }
        )
    }

    if (showUnassignAccountDialog && accountToUnassign != null) {
        UnassignDialog(
            title = "Unassign Account?",
            message = "Remove '${accountToUnassign?.accountName}' from this signatory?",
            onConfirm = {
                accountToUnassign?.let {
                    viewModel.unassignAccount(
                        signatoryId = signatoryId,
                        accountId = it.accountId,
                        onSuccess = { Toast.makeText(context, "Account unassigned", Toast.LENGTH_SHORT).show() },
                        onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                    )
                }
                showUnassignAccountDialog = false
            },
            onDismiss = { showUnassignAccountDialog = false }
        )
    }
}

// --- Helpers ---

@Composable
private fun HeaderLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ResponsibilityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onUnassign: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SchoolBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.Gray)
            }

            // Context Menu inside the card
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Unassign", color = SchoolRed) },
                    onClick = {
                        onUnassign()
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = SchoolRed) }
                )
            }
        }
    }
}

@Composable
private fun UnassignDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, null, tint = SchoolRed) },
        title = { Text(title) },
        text = { Text(message, textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
            ) { Text("Unassign") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )
}