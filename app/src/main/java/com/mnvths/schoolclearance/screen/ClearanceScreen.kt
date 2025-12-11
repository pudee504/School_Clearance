package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.StudentClearanceStatus
import com.mnvths.schoolclearance.viewmodel.ClearanceViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val SuccessGreen = Color(0xFF2E7D32)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceScreen(
    navController: NavController,
    sectionId: Int,
    subjectId: Int, // Can be accountId if isAccountClearance is true
    gradeLevel: String,
    sectionName: String,
    subjectName: String,
    isAccountClearance: Boolean = false,
    showFab: Boolean = true,
    viewModel: ClearanceViewModel = viewModel()
) {
    val students by viewModel.students
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showStudentConfirmationDialog by remember { mutableStateOf(false) }
    var pendingStudentChange by remember { mutableStateOf<Pair<StudentClearanceStatus, Boolean>?>(null) }

    // Sorting: Pending First, then Alphabetical
    val processedStudents = remember(students, searchText) {
        students
            .filter { student ->
                val fullName = "${student.lastName}, ${student.firstName}"
                fullName.contains(searchText, ignoreCase = true) || student.studentId.contains(searchText, ignoreCase = true)
            }
            .sortedWith(
                compareBy<StudentClearanceStatus> { it.isCleared } // False (Pending) comes before True (Cleared)
                    .thenBy { it.lastName }
                    .thenBy { it.firstName }
            )
    }

    LaunchedEffect(Unit) {
        if (isAccountClearance) {
            viewModel.fetchStudentClearanceStatusForAccount(sectionId, subjectId)
        } else {
            viewModel.fetchStudentClearanceStatus(sectionId, subjectId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(subjectName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "$gradeLevel - $sectionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
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
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate("assignStudent/${sectionId}") },
                    containerColor = SchoolBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Student")
                }
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Control Panel ---
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by Name or ID") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Batch Action
                Button(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = students.any { !it.isCleared && it.isClearable },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Eligible Students")
                }
            }

            // --- List Content ---
            if (isLoading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = error!!, color = SchoolRed)
                }
            } else if (students.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No students enrolled in this section.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header for stats
                    item {
                        val clearedCount = students.count { it.isCleared }
                        Text(
                            text = "$clearedCount / ${students.size} CLEARED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(processedStudents, key = { it.userId }) { student ->
                        StudentClearanceChecklistItem(
                            student = student,
                            onStatusChange = { newStatus ->
                                pendingStudentChange = student to newStatus
                                showStudentConfirmationDialog = true
                            }
                        )
                    }

                    // Space for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen) },
            title = { Text("Clear All?") },
            text = { Text("This will mark all eligible pending students as CLEARED. Locked students will remain unchanged.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotClearedStudents(
                            onSuccess = { Toast.makeText(context, "All cleared!", Toast.LENGTH_SHORT).show() },
                            onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                        )
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) { Text("Confirm Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }

    if (showStudentConfirmationDialog && pendingStudentChange != null) {
        val (student, newStatus) = pendingStudentChange!!
        val actionColor = if (newStatus) SuccessGreen else SchoolRed

        AlertDialog(
            onDismissRequest = {
                showStudentConfirmationDialog = false
                pendingStudentChange = null
            },
            icon = {
                Icon(
                    if(newStatus) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    null,
                    tint = actionColor
                )
            },
            title = { Text(if (newStatus) "Clear Student?" else "Revoke Clearance?") },
            text = { Text("Update status for ${student.firstName} ${student.lastName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStudentClearance(
                            userId = student.userId,
                            isCleared = newStatus,
                            onSuccess = {
                                val statusText = if (newStatus) "cleared" else "revoked"
                                Toast.makeText(context, "${student.lastName} $statusText.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg -> Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show() }
                        )
                        showStudentConfirmationDialog = false
                        pendingStudentChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor)
                ) { Text(if (newStatus) "Confirm Clear" else "Confirm Revoke") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStudentConfirmationDialog = false
                    pendingStudentChange = null
                }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }
}

// Renamed Helper Component
@Composable
fun StudentClearanceChecklistItem(
    student: StudentClearanceStatus,
    onStatusChange: (Boolean) -> Unit
) {
    val isEnabled = student.isClearable || student.isCleared
    val textColor by animateColorAsState(if (student.isCleared) Color.Gray else Color.Black)
    val cardAlpha by animateFloatAsState(if (isEnabled) 1f else 0.6f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.lastName}, ${student.firstName}",
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textDecoration = if (student.isCleared) TextDecoration.LineThrough else TextDecoration.None
                )

                if (!isEnabled && !student.isCleared) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prerequisites not met",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Text(
                        text = "ID: ${student.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Switch(
                checked = student.isCleared,
                onCheckedChange = onStatusChange,
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SuccessGreen,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f),
                    disabledCheckedTrackColor = SuccessGreen.copy(alpha = 0.5f),
                    disabledUncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            )
        }
    }
}