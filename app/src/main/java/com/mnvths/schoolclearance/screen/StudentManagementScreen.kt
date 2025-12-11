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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
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
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

// Defined locally to match your theme requirements
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    navController: NavController,
    studentViewModel: StudentManagementViewModel = viewModel(),
    sectionViewModel: SectionManagementViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        studentViewModel.fetchAllStudents()
        sectionViewModel.fetchAllGradeLevels()
        sectionViewModel.fetchClassSections()
    }

    val allStudents by studentViewModel.students.collectAsState()
    val gradeLevels by sectionViewModel.gradeLevels.collectAsState()
    val sections by sectionViewModel.classSections.collectAsState()
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
                fullName.contains(searchQuery.lowercase()) || student.id.contains(searchQuery)
            }
            .filter { student ->
                selectedGrade == null || student.gradeLevel == selectedGrade
            }
            .filter { student ->
                selectedSectionId == null || student.sectionId == selectedSectionId
            }
            .sortedBy { it.lastName }
    }

    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addStudent") },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Student")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Control Panel (Search & Filter) ---
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Student Directory",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SchoolBlue
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Name or LRN") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdown(
                        label = "Grade Level",
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
            }

            // --- Student List ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SchoolBlue
                    )
                } else if (error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SchoolRed, modifier = Modifier.size(48.dp))
                        Text(text = "Error loading data", color = Color.Gray)
                    }
                } else if (filteredStudents.isEmpty()) {
                    // Empty State
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No students found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStudents, key = { it.id }) { student ->
                            StudentItem(
                                student = student,
                                onClick = { navController.navigate("adminStudentDetail/${student.id}") },
                                onEdit = { navController.navigate("editStudent/${student.id}") },
                                onDelete = {
                                    studentToDelete = student
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SchoolRed) },
            title = { Text("Delete Student?") },
            text = { Text("Are you sure you want to delete ${studentToDelete?.firstName} ${studentToDelete?.lastName}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete?.let { student ->
                            studentViewModel.deleteStudent(
                                studentId = student.id,
                                onSuccess = {
                                    Toast.makeText(context, "Student deleted successfully", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                            )
                        }
                        showDeleteDialog = false
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun StudentItem(
    student: StudentListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                // Add Section Info if available, purely visual
                Text(
                    text = "${student.gradeLevel}", // You can append section name here if available in object
                    style = MaterialTheme.typography.labelSmall,
                    color = SchoolBlue
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
                        text = { Text("Edit Details") },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = SchoolRed) },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = SchoolRed) }
                    )
                }
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
        onExpandedChange = { if(enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SchoolBlue,
                unfocusedBorderColor = Color.LightGray
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onValueChange(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}