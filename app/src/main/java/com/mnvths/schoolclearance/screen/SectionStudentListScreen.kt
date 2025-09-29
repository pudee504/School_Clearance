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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.viewmodel.StudentInSection
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionStudentListScreen(
    navController: NavController,
    sectionId: Int,
    sectionName: String,
    gradeLevel: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val students by viewModel.studentsInSection.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var studentToRemove by remember { mutableStateOf<StudentInSection?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // This allows the list to refresh when we navigate back from the assign screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        viewModel.fetchStudentsBySection(sectionId)
    }

    // Clear the list when the user leaves the screen for good
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentsInSection()
        }
    }

    // Filter and sort the students list
    val filteredAndSortedStudents = remember(students, searchQuery) {
        students
            .filter { student ->
                val fullName = "${student.firstName} ${student.lastName}"
                student.id.contains(searchQuery, ignoreCase = true) ||
                        fullName.contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { it.lastName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$gradeLevel - $sectionName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("assignStudent/$sectionId") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Assign Student")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (isLoading && students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredAndSortedStudents.isEmpty()) {
                Text(
                    text = if (students.isEmpty()) "No students found in this section. Tap the '+' button to assign one."
                    else "No students match your search.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredAndSortedStudents, key = { it.id }) { student ->
                        StudentRow(
                            student = student,
                            // ✅ ADD THIS: Define what happens when the row is clicked
                            onClick = {
                                navController.navigate("adminStudentDetail/${student.id}")
                            },
                            onEdit = {
                                navController.navigate("editStudent/${student.id}")
                            },
                            onRemove = {
                                studentToRemove = student
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for removing a student from the section
    if (studentToRemove != null) {
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            title = { Text("Remove Student") },
            text = { Text("Are you sure you want to remove ${studentToRemove!!.firstName} ${studentToRemove!!.lastName} from this section? They will become unassigned.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unassignStudentFromSection(
                            studentId = studentToRemove!!.id,
                            onSuccess = {
                                Toast.makeText(context, "Student removed from section", Toast.LENGTH_SHORT).show()
                                viewModel.fetchStudentsBySection(sectionId) // Refresh list
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                        studentToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                Button(onClick = { studentToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentRow(
    student: StudentInSection,
    onClick: () -> Unit, // ✅ ADD THIS parameter to handle the click
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        // ✅ ADD THIS modifier to make the entire card clickable
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${student.lastName}, ${student.firstName}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

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
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Section", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onRemove()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}