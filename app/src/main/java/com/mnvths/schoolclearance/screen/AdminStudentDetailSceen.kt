package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.ClearanceStatusItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

// Theme Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val SuccessGreen = Color(0xFF2E7D32)
private val BackgroundGray = Color(0xFFF5F5F5)

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
            CenterAlignedTopAppBar(
                title = { Text("Student Clearance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    profile?.let { p ->
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Student") },
                                    onClick = {
                                        navController.navigate("editStudent/${p.id}")
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Delete Student", color = SchoolRed) },
                                    onClick = {
                                        showDeleteDialog = true
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = SchoolRed) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = SchoolBlue,
                    actionIconContentColor = SchoolBlue
                )
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && profile == null) {
                CircularProgressIndicator(color = SchoolBlue)
            } else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = SchoolRed, modifier = Modifier.size(48.dp))
                    Text(text = "Error: $error", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                profile?.let { p ->
                    val subjects = remember(p.clearanceStatus) { p.clearanceStatus.filter { it.requirementType == "subject" } }
                    val accounts = remember(p.clearanceStatus) { p.clearanceStatus.filter { it.requirementType == "account" } }

                    // Logic for prerequisites (Principal/Adviser)
                    val (adviserItem, isAdviserClearable) = remember(p.clearanceStatus) {
                        val adviser = p.clearanceStatus.find { it.signatoryName == "Class Adviser" }
                        val principal = p.clearanceStatus.find { it.signatoryName == "Principal" }
                        val prerequisites = p.clearanceStatus.filter { it.requirementId != adviser?.requirementId && it.requirementId != principal?.requirementId }
                        adviser to prerequisites.all { it.isCleared }
                    }

                    val (principalItem, isPrincipalClearable) = remember(p.clearanceStatus) {
                        val principal = p.clearanceStatus.find { it.signatoryName == "Principal" }
                        val prerequisites = p.clearanceStatus.filter { it.requirementId != principal?.requirementId }
                        principal to prerequisites.all { it.isCleared }
                    }

                    // Progress Calculation
                    val totalItems = p.clearanceStatus.size
                    val clearedItems = p.clearanceStatus.count { it.isCleared }
                    val progress = if (totalItems > 0) clearedItems.toFloat() / totalItems else 0f

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Student Digital ID Card
                        item {
                            StudentProfileCard(profile = p)
                        }

                        // 2. Progress Bar
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Overall Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = SchoolBlue, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = SchoolBlue,
                                        trackColor = SchoolBlue.copy(alpha = 0.1f),
                                    )
                                }
                            }
                        }

                        // 3. Subjects List
                        if (subjects.isNotEmpty()) {
                            item { SectionHeader("Academic Subjects") }
                            items(subjects, key = { "subject-${it.requirementId}" }) { item ->
                                val isEnabled = when (item.requirementId) {
                                    adviserItem?.requirementId -> isAdviserClearable || item.isCleared
                                    principalItem?.requirementId -> isPrincipalClearable || item.isCleared
                                    else -> true
                                }
                                ClearanceItemCard(
                                    item = item,
                                    isEnabled = isEnabled,
                                    onStatusChange = { newStatus ->
                                        pendingClearanceChange = item to newStatus
                                        showConfirmationDialog = true
                                    }
                                )
                            }
                        }

                        // 4. Accounts List
                        if (accounts.isNotEmpty()) {
                            item { SectionHeader("Administrative Accounts") }
                            items(accounts, key = { "account-${it.requirementId}" }) { item ->
                                ClearanceItemCard(
                                    item = item,
                                    isEnabled = true,
                                    onStatusChange = { newStatus ->
                                        pendingClearanceChange = item to newStatus
                                        showConfirmationDialog = true
                                    }
                                )
                            }
                        }

                        // 5. Success Message
                        if (progress == 1f) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, null, tint = SuccessGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Student is fully cleared!", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showDeleteDialog) {
        profile?.let { p ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Warning, null, tint = SchoolRed) },
                title = { Text("Delete Student?") },
                text = { Text("This will permanently remove ${p.name} and all their records.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStudent(
                                studentId = p.id,
                                onSuccess = {
                                    Toast.makeText(context, "Student deleted.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                            )
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                },
                containerColor = Color.White
            )
        }
    }

    if (showConfirmationDialog && pendingClearanceChange != null) {
        val (item, newStatus) = pendingClearanceChange!!
        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                pendingClearanceChange = null
            },
            icon = {
                Icon(
                    if(newStatus) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if(newStatus) SchoolBlue else SchoolRed
                )
            },
            title = { Text(if (newStatus) "Mark as Cleared?" else "Revoke Clearance?") },
            text = { Text("Are you sure you want to update the status for '${item.requirementName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        profile?.let { p ->
                            viewModel.updateClearanceStatus(
                                isCleared = newStatus, item = item, profile = p,
                                onSuccess = {
                                    val action = if (newStatus) "cleared" else "revoked"
                                    Toast.makeText(context, "${item.requirementName} $action.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                            )
                        }
                        showConfirmationDialog = false
                        pendingClearanceChange = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (newStatus) SchoolBlue else SchoolRed
                    )
                ) {
                    Text(if (newStatus) "Confirm Clear" else "Confirm Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    pendingClearanceChange = null
                }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }
}

// --- Composable Components ---

@Composable
fun StudentProfileCard(profile: com.mnvths.schoolclearance.data.AdminStudentProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SchoolBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${profile.name.split(" ").firstOrNull()?.take(1) ?: ""}${profile.name.split(" ").lastOrNull()?.take(1) ?: ""}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text("LRN: ${profile.id}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat("Grade", profile.gradeLevel ?: "N/A")
                ProfileStat("Section", profile.section ?: "N/A")
                ProfileStat("Term", "${profile.activeTerm.termNumber}")
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun ClearanceItemCard(
    item: ClearanceStatusItem,
    isEnabled: Boolean,
    onStatusChange: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(if (item.isCleared) SuccessGreen else Color.Transparent)
    val cardAlpha by animateFloatAsState(if (isEnabled) 1f else 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if(item.isCleared) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
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
            // Status Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (item.isCleared) SuccessGreen.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCleared) Icons.Default.Check else Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = if (item.isCleared) SuccessGreen else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.requirementName ?: "Unknown Requirement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) Color.Black else Color.Gray
                )
                if (item.signatoryName != null) {
                    Text(
                        text = item.signatoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) SchoolBlue else Color.Gray
                    )
                }
            }

            Switch(
                checked = item.isCleared,
                onCheckedChange = onStatusChange,
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SuccessGreen,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            )
        }
    }
}