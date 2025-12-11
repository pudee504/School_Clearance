package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.viewmodel.StudentInSection
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

// Theme Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        viewModel.fetchStudentsBySection(sectionId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentsInSection()
        }
    }

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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sectionName, fontWeight = FontWeight.Bold)
                        Text(gradeLevel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("assignStudent/$sectionId") },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Assign Student") }
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Name or LRN") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
            }

            // List Content
            if (isLoading && students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (filteredAndSortedStudents.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (students.isEmpty()) Icons.Outlined.Person else Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (students.isEmpty()) "No students in this section yet." else "No matches found.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    if (students.isEmpty()) {
                        Text(
                            text = "Tap 'Assign Student' to populate this class.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAndSortedStudents, key = { it.id }) { student ->
                        SectionListStudentRow(
                            student = student,
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
                    item { Spacer(modifier = Modifier.height(64.dp)) } // Space for FAB
                }
            }
        }
    }

    // Removal Dialog
    if (studentToRemove != null) {
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            icon = { Icon(Icons.Default.PersonRemove, null, tint = SchoolRed) },
            title = { Text("Remove from Section?") },
            text = {
                Text(
                    "Are you sure you want to remove ${studentToRemove!!.firstName} ${studentToRemove!!.lastName} from $sectionName? \n\nThey will be marked as unassigned.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unassignStudentFromSection(
                            studentId = studentToRemove!!.id,
                            onSuccess = {
                                Toast.makeText(context, "Student removed.", Toast.LENGTH_SHORT).show()
                                viewModel.fetchStudentsBySection(sectionId)
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                        studentToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { studentToRemove = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }
}

// Renamed to avoid conflict
@Composable
private fun SectionListStudentRow(
    student: StudentInSection,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${student.firstName.take(1)}${student.lastName.take(1)}",
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.lastName}, ${student.firstName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LRN: ${student.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Student") },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Remove from Section", color = SchoolRed) },
                        onClick = {
                            onRemove()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null, tint = SchoolRed) }
                    )
                }
            }
        }
    }
}