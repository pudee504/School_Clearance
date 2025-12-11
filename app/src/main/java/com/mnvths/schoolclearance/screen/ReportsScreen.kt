package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.FullyClearedStudent
import com.mnvths.schoolclearance.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel()
) {
    val reportData by viewModel.reportData
    val gradeLevels by viewModel.gradeLevels
    val sections by viewModel.sections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var selectedGradeLevel by remember { mutableStateOf<Int?>(null) }
    var selectedSection by remember { mutableStateOf<Int?>(null) }
    var gradeLevelExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchFullyClearedStudents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fully Cleared Students Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Cleared: ${reportData?.totalCount ?: 0}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = gradeLevelExpanded,
                    onExpandedChange = { gradeLevelExpanded = !gradeLevelExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = gradeLevels.find { it.id == selectedGradeLevel }?.name ?: "All Grade Levels",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeLevelExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = gradeLevelExpanded,
                        onDismissRequest = { gradeLevelExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Grade Levels") },
                            onClick = {
                                selectedGradeLevel = null
                                selectedSection = null
                                viewModel.fetchFullyClearedStudents()
                                gradeLevelExpanded = false
                            }
                        )
                        gradeLevels.forEach { gradeLevel ->
                            DropdownMenuItem(
                                text = { Text(gradeLevel.name) },
                                onClick = {
                                    selectedGradeLevel = gradeLevel.id
                                    selectedSection = null
                                    viewModel.fetchSectionsForGradeLevel(gradeLevel.id)
                                    viewModel.fetchFullyClearedStudents(gradeLevelId = gradeLevel.id)
                                    gradeLevelExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = sections.find { it.sectionId == selectedSection }?.sectionName ?: "All Sections",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Section") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                        modifier = Modifier.menuAnchor(),
                        enabled = selectedGradeLevel != null
                    )
                    ExposedDropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Sections") },
                            onClick = {
                                selectedSection = null
                                viewModel.fetchFullyClearedStudents(gradeLevelId = selectedGradeLevel)
                                sectionExpanded = false
                            }
                        )
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.sectionName) },
                                onClick = {
                                    selectedSection = section.sectionId
                                    viewModel.fetchFullyClearedStudents(gradeLevelId = selectedGradeLevel, sectionId = section.sectionId)
                                    sectionExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                }
                reportData?.students?.isEmpty() == true -> {
                    Text("No fully cleared students found.")
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reportData?.students ?: emptyList()) { student ->
                            StudentReportItem(student = student)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentReportItem(student: FullyClearedStudent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        // Optional: Add a little elevation or border if it feels too flat
        // elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Keeps the padding for breathing room
        ) {
            Text(
                text = "${student.lastName}, ${student.firstName} ${student.middleName?.first()?.plus(".") ?: ""}",
                style = MaterialTheme.typography.titleMedium, // Slightly larger text for the name
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp)) // Small gap between name and ID
            Text(
                text = "ID: ${student.studentId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly lighter color for ID
            )
        }
    }
}
