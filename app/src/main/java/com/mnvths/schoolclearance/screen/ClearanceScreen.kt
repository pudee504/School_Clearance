package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.StudentClearanceStatus
import com.mnvths.schoolclearance.viewmodel.ClearanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceScreen(
    navController: NavController,
    sectionId: Int,
    subjectId: Int,
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


    val processedStudents = remember(students, searchText) {
        students
            .filter { student ->
                val fullName = "${student.lastName}, ${student.firstName}"
                fullName.contains(searchText, ignoreCase = true) || student.studentId.contains(searchText, ignoreCase = true)
            }
            .sortedWith(
                compareBy<StudentClearanceStatus> { it.isCleared }
                    .thenBy { it.lastName }
                    .thenBy { it.firstName }
            )
    }

    LaunchedEffect(Unit) {
        if (isAccountClearance) {
            viewModel.fetchStudentClearanceStatusForAccount(sectionId, subjectId) // subjectId here is actually the accountId
        } else {
            viewModel.fetchStudentClearanceStatus(sectionId, subjectId)
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to clear all remaining uncleared students?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotClearedStudents(
                            onSuccess = {
                                Toast.makeText(context, "All students cleared.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                        showClearAllDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStudentConfirmationDialog && pendingStudentChange != null) {
        val (student, newStatus) = pendingStudentChange!!
        val actionText = if (newStatus) "clear" else "mark as not cleared"
        val studentName = "${student.firstName} ${student.lastName}"

        AlertDialog(
            onDismissRequest = { showStudentConfirmationDialog = false },
            title = { Text("Confirm Change") },
            text = { Text("Are you sure you want to $actionText '$studentName'?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateStudentClearance(
                        userId = student.userId,
                        isCleared = newStatus,
                        onSuccess = {
                            val statusText = if (newStatus) "cleared" else "marked as not cleared"
                            Toast.makeText(context, "${student.lastName} $statusText.", Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                    showStudentConfirmationDialog = false
                    pendingStudentChange = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showStudentConfirmationDialog = false
                    pendingStudentChange = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "$subjectName Clearance")
                        Text(
                            text = "Section: $gradeLevel - $sectionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFab) { // ✅ WRAP THE FAB IN THIS IF-STATEMENT
                FloatingActionButton(
                    onClick = {
                        navController.navigate("assignStudent/${sectionId}")
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Students to Section")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by Name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    showClearAllDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = students.any { !it.isCleared }
            ) {
                Text("Clear All Not Cleared")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            else if (error != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            else if (students.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No students found in this section.")
                }
            }
            else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(processedStudents, key = { it.userId }) { student ->
                        StudentClearanceItem(
                            student = student,
                            onStatusChange = { newStatus ->
                                pendingStudentChange = student to newStatus
                                showStudentConfirmationDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentClearanceItem(
    student: StudentClearanceStatus,
    onStatusChange: (Boolean) -> Unit
) {
    val formattedName = "${student.lastName}, ${student.firstName}" +
            (student.middleName?.takeIf { it.isNotBlank() }?.let { " ${it.first()}." } ?: "")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formattedName, fontWeight = FontWeight.Bold)
                Text(text = "ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = student.isCleared,
                onCheckedChange = onStatusChange
            )
        }
    }
}