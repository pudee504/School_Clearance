package com.mnvths.schoolclearance.screen

import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@Composable
fun StudentManagementScreen(
    navController: NavController,
    studentViewModel: StudentManagementViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        studentViewModel.fetchAllStudents()
        studentViewModel.fetchAllGradeLevels()
        studentViewModel.fetchSections()
    }

    val allStudents by studentViewModel.students.collectAsState()
    val gradeLevels by studentViewModel.gradeLevels.collectAsState()
    val sections by studentViewModel.sections.collectAsState()
    val isLoading by studentViewModel.isLoading.collectAsState()
    val error by studentViewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf<String?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<StudentListItem?>(null) }

    val filteredStudents = remember(allStudents, searchQuery, selectedGrade, selectedSectionId) {
        allStudents
            .filter { student ->
                val fullName = "${student.firstName} ${student.lastName}".lowercase()
                fullName.contains(searchQuery.lowercase())
            }
            .filter { student ->
                selectedGrade == null || student.gradeLevel == "Unassigned" || student.gradeLevel == selectedGrade
            }
            .filter { student ->
                selectedSectionId == null || student.sectionId == selectedSectionId
            }
            .sortedBy { it.lastName }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { navController.navigate("addStudent") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Student")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Student")
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdown(
                label = "Grade",
                options = gradeLevels,
                selectedValue = selectedGrade,
                onValueChange = {
                    selectedGrade = it
                    selectedSectionId = null
                },
                modifier = Modifier.weight(1f)
            )

            FilterDropdown(
                label = "Section",
                options = sections.filter { it.gradeLevel == selectedGrade }.map { it.sectionName },
                selectedValue = sections.find { it.sectionId == selectedSectionId }?.sectionName,
                onValueChange = { newSectionName ->
                    selectedSectionId = sections.find { it.sectionName == newSectionName && it.gradeLevel == selectedGrade }?.sectionId
                },
                modifier = Modifier.weight(1f),
                enabled = selectedGrade != null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Students:", style = MaterialTheme.typography.titleLarge)
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
                items(filteredStudents, key = { it.id }) { student ->
                    StudentItem(
                        student = student,
                        onClick = {
                            navController.navigate("adminStudentDetail/${student.id}")
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
        val context = LocalContext.current
        DeleteStudentConfirmationDialog(
            studentName = "${studentToDelete?.firstName} ${studentToDelete?.lastName}",
            onConfirm = {
                studentToDelete?.let { student ->
                    studentViewModel.deleteStudent(
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

@Composable
fun StudentItem(
    student: StudentListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.lastName}, ${student.firstName} ${student.middleName ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LRN/ID: ${student.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedValue: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onValueChange(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

@Composable
fun DeleteStudentConfirmationDialog(
    studentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Student") },
        text = { Text("Are you sure you want to delete $studentName? This will remove all their records and cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}