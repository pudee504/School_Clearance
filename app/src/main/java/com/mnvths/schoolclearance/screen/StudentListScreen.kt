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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
// ❗ IMPORTANT: Ensure this import points to your central model, NOT the viewmodel package
import com.mnvths.schoolclearance.viewmodel.StudentListItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    navController: NavController,
    sectionId: Int,
    gradeLevel: String,
    sectionName: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val students by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<StudentListItem?>(null) }
    var searchText by remember { mutableStateOf("") }

    val filteredStudents = remember(students, searchText) {
        if (searchText.isBlank()) {
            students
        } else {
            students.filter { student ->
                val fullName = "${student.lastName}, ${student.firstName} ${student.middleName ?: ""}"
                fullName.contains(searchText, ignoreCase = true) || student.id.contains(searchText, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(sectionId) {
        viewModel.fetchStudentsForSection(sectionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students in $gradeLevel - $sectionName") },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by Name or ID") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("addStudent/$sectionId/$sectionName") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Student")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Student")
            }
            Spacer(modifier = Modifier.height(16.dp))

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
                    items(filteredStudents) { student ->
                        StudentItem(
                            student = student,
                            onEditClick = {
                                navController.navigate("editStudent/${student.id}")
                            },
                            onDeleteClick = {
                                studentToDelete = student
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            // FIX #1: Define studentName before it's used
            val studentName = "${studentToDelete?.lastName}, ${studentToDelete?.firstName}"
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Student") },
                text = { Text("Are you sure you want to delete $studentName?") },
                confirmButton = {
                    Button(
                        onClick = {
                            studentToDelete?.let { student ->
                                val formattedName = "${student.lastName}, ${student.firstName}"
                                viewModel.deleteStudent(
                                    studentId = student.id,
                                    sectionId = sectionId,
                                    onSuccess = {
                                        Toast.makeText(context, "$formattedName deleted.", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StudentItem(
    student: StudentListItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // FIX #2: Define formattedName before it's used
    val formattedName = "${student.lastName}, ${student.firstName}" +
            (student.middleName?.takeIf { it.isNotBlank() }?.let { " ${it.first()}." } ?: "")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${student.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Student")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Student")
                }
            }
        }
    }
}