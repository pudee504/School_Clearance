package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    navController: NavController,
    sectionId: Int,
    gradeLevel: String,
    sectionName: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val allStudents by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<StudentListItem?>(null) }

    val studentsInSection = remember(allStudents, sectionId) {
        allStudents.filter { it.sectionId == sectionId }
    }

    val filteredStudents = remember(studentsInSection, searchText) {
        if (searchText.isBlank()) {
            studentsInSection
        } else {
            studentsInSection.filter { student ->
                val fullName = "${student.lastName}, ${student.firstName} ${student.middleName ?: ""}"
                fullName.contains(searchText, ignoreCase = true) || (student.id.contains(searchText, ignoreCase = true))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students in $gradeLevel - $sectionName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        // ✅ ADDED: Floating Action Button for consistency
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addStudent") },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Student")
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
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ✅ REMOVED: Old "Add New Student" button from here

            if (isLoading && allStudents.isEmpty()) {
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
                    items(filteredStudents, key = { it.id }) { student ->
                        StudentItem(
                            student = student,
                            onClick = {
                                navController.navigate("adminStudentDetail/${student.id}")
                            },
                            // ✅ FIX: Added the missing onEdit parameter to fix the error
                            onEdit = {
                                navController.navigate("editStudent/${student.id}")
                            },
                            onDelete = {
                                studentToDelete = student
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            DeleteStudentConfirmationDialog(
                studentName = "${studentToDelete?.firstName} ${studentToDelete?.lastName}",
                onConfirm = {
                    studentToDelete?.let { student ->
                        viewModel.deleteStudent(
                            studentId = student.id,
                            onSuccess = {
                                Toast.makeText(context, "${student.lastName} deleted.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                        )
                    }
                    showDeleteDialog = false
                    studentToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    studentToDelete = null
                }
            )
        }
    }
}