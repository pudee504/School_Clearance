package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel

// This screen remains the same.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumHomeScreen(
    navController: NavController,
    viewModel: SubjectViewModel = viewModel()
) {
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchGradeLevels()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Curriculum Management") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(gradeLevels, key = { it.id }) { gradeLevel ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("curriculumManagement/${gradeLevel.id}/${gradeLevel.name}")
                                }
                        ) {
                            Text(
                                text = gradeLevel.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ✅ MODIFIED: This screen now includes the semester switcher.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumManagementScreen(
    navController: NavController,
    gradeLevelId: Int,
    gradeLevelName: String,
    viewModel: SubjectViewModel = viewModel()
) {
    val subjects by viewModel.managementSubjects.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var selectedSemester by remember { mutableStateOf(1) }

    // ✅ NEW: State to track which subject's menu is expanded
    var expandedMenuRequirementId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(gradeLevelId, selectedSemester) {
        val semesterToFetch = if (gradeLevelId > 4) selectedSemester else 1
        viewModel.fetchSubjectsForGradeLevel(gradeLevelId, semesterToFetch)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gradeLevelName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (gradeLevelId > 4) {
                val semesters = listOf("Semester 1", "Semester 2")
                TabRow(selectedTabIndex = selectedSemester - 1) {
                    semesters.forEachIndexed { index, title ->
                        Tab(
                            selected = (selectedSemester - 1) == index,
                            onClick = { selectedSemester = index + 1 },
                            text = { Text(title) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text("Error: $error", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                } else if (subjects.isEmpty()) {
                    Text("No active subjects found for this semester.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ✅ --- MODIFICATION START ---
                        items(subjects, key = { it.requirementId }) { subject ->
                            // Define the list of special, non-removable subject IDs
                            val nonRemovableSubjectIds = listOf(87, 88, 89) // Library, Adviser, Principal

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subject.subjectName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    // Conditionally show the options menu if the subject is not in the non-removable list
                                    if (subject.subjectId !in nonRemovableSubjectIds) {
                                        Box {
                                            // This is the '...' button
                                            IconButton(onClick = {
                                                expandedMenuRequirementId = subject.requirementId
                                            }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "More options for ${subject.subjectName}"
                                                )
                                            }
                                            // This is the dropdown menu that appears
                                            DropdownMenu(
                                                expanded = expandedMenuRequirementId == subject.requirementId,
                                                onDismissRequest = { expandedMenuRequirementId = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Remove") },
                                                    onClick = {
                                                        showRemoveDialog = subject.requirementId
                                                        expandedMenuRequirementId = null // Close the menu after clicking
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Remove",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    // If subjectId is one of the special ones, nothing is rendered here.
                                }
                            }
                        }
                        // ✅ --- MODIFICATION END ---
                    }
                }
            }
        }
    }

    // --- Dialogs (Now pass the current semester to the viewmodel actions) ---
    // --- Dialogs ---
    if (showAddDialog) {
        var newSubjectName by remember { mutableStateOf("") }
        var newSubjectCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subject to $gradeLevelName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubjectCode,
                        onValueChange = { newSubjectCode = it },
                        // ✅ CHANGED: Label no longer says "(Optional)"
                        label = { Text("Subject Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // ✅ CHANGED: Guard clause now checks both fields
                        if (newSubjectName.isNotBlank() && newSubjectCode.isNotBlank()) {
                            isSubmitting = true
                            viewModel.addSubjectToCurriculum(
                                name = newSubjectName,
                                gradeLevelId = gradeLevelId,
                                semester = selectedSemester,
                                // ✅ CHANGED: Pass the code directly, it's guaranteed not to be blank
                                subjectCode = newSubjectCode,
                                onSuccess = {
                                    Toast.makeText(context, "Subject added!", Toast.LENGTH_SHORT).show()
                                    isSubmitting = false
                                    showAddDialog = false
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                }
                            )
                        }
                    },
                    // ✅ CHANGED: Button is only enabled when BOTH fields are filled
                    enabled = !isSubmitting && newSubjectName.isNotBlank() && newSubjectCode.isNotBlank()
                ) {
                    if(isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Add")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRemoveDialog != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Confirm Removal") },
            text = { Text("This will make the subject inactive for this grade level. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.setRequirementStatus(
                            requirementId = showRemoveDialog!!,
                            status = "inactive",
                            gradeLevelId = gradeLevelId,
                            semester = selectedSemester,
                            onSuccess = {
                                Toast.makeText(context, "Subject removed from curriculum.", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                showRemoveDialog = null
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if(isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Remove")
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") } }
        )
    }
}