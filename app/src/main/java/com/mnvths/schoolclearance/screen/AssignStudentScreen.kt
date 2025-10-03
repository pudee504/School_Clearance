package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignStudentScreen(
    navController: NavController,
    sectionId: Int,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    // ✅ 1. Use the correct state variable for unassigned students
    val unassignedStudents by viewModel.unassignedStudents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // ✅ 2. Call the specific function to fetch only the students we need
    LaunchedEffect(Unit) {
        viewModel.fetchUnassignedStudents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Students") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.assignStudentsToSection(
                        sectionId = sectionId,
                        studentIds = selectedStudentIds.toList(),
                        onSuccess = {
                            Toast.makeText(context, "Students assigned successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedStudentIds.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Assign ${selectedStudentIds.size} Student(s)")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ✅ 3. Add a loading indicator for a better user experience
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (unassignedStudents.isEmpty()) {
                Text(
                    text = "There are no students without a section and grade level.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(unassignedStudents, key = { it.id }) { student ->
                        SelectableStudentRow(
                            student = student,
                            isSelected = student.id in selectedStudentIds,
                            onSelect = {
                                selectedStudentIds = if (it) {
                                    selectedStudentIds + student.id
                                } else {
                                    selectedStudentIds - student.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Your SelectableStudentRow composable is already correct and needs no changes.
@Composable
fun SelectableStudentRow(
    student: StudentListItem,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null // Click is handled by the row
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${student.lastName}, ${student.firstName}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}