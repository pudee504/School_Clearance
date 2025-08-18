package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.ClassSection
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel
import java.time.Year

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchSections()
    }

    val sections by viewModel.sections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var startYear by remember { mutableStateOf(Year.now().value) }
    var quarter by remember { mutableIntStateOf(1) }
    var isEditing by remember { mutableStateOf(false) }

    // State for the delete confirmation dialog
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<ClassSection?>(null) }

    val sortedSections = remember(sections) {
        sections.sortedWith(
            compareBy(
                // First, sort by the numeric part of the grade level
                { it.gradeLevel.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 },
                // Then, sort by the section name alphabetically
                { it.sectionName }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SchoolYearAndQuarterSelectors(
            startYear = startYear,
            onYearChange = { newYear -> startYear = newYear },
            quarter = quarter,
            onQuarterChange = { newQuarter -> quarter = newQuarter },
            isEditing = isEditing,
            onEditClick = { isEditing = true },
            onSaveChanges = {
                isEditing = false
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("addSection") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Section")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Section")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sections:", style = MaterialTheme.typography.titleLarge)
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
                items(sortedSections) { section ->
                    SectionItem(
                        section = section,
                        onClick = {
                            navController.navigate("studentList/${section.sectionId}/${section.gradeLevel}/${section.sectionName}")
                        },
                        onEdit = {
                            // Navigate to a new edit screen
                            navController.navigate("editSection/${section.sectionId}/${section.gradeLevel}/${section.sectionName}")
                        },
                        onDelete = {
                            // Show the confirmation dialog
                            sectionToDelete = section
                            showDeleteConfirmationDialog = true
                        }
                    )
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            sectionName = sectionToDelete?.sectionName ?: "",
            onConfirm = {
                sectionToDelete?.let {
                    viewModel.deleteSection(
                        sectionId = it.sectionId,
                        onSuccess = { /* Handle success if needed */ },
                        onError = { /* Handle error if needed */ }
                    )
                }
                showDeleteConfirmationDialog = false
                sectionToDelete = null
            },
            onDismiss = {
                showDeleteConfirmationDialog = false
                sectionToDelete = null
            }
        )
    }
}

@Composable
fun SchoolYearAndQuarterSelectors(
    startYear: Int,
    onYearChange: (Int) -> Unit,
    quarter: Int,
    onQuarterChange: (Int) -> Unit,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSaveChanges: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "School Year: ${startYear}-${startYear + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Quarter: $quarter",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = {
                    if (isEditing) {
                        onSaveChanges()
                    } else {
                        onEditClick()
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditing) "Save Changes" else "Edit"
                    )
                }
            }
            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onYearChange(startYear - 1) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrement year")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onYearChange(startYear + 1) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increment year")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (q in 1..4) {
                        FilterChip(
                            selected = quarter == q,
                            onClick = { onQuarterChange(q) },
                            label = { Text(q.toString(), textAlign = TextAlign.Center) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionItem(
    section: ClassSection,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // The whole card is clickable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grade ${section.gradeLevel} - ${section.sectionName}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit button now calls onEdit
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Section")
                }
                // Delete button now calls onDelete
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Section")
                }
            }
        }
    }
}

// --- NEW COMPOSABLE FOR THE DIALOG ---
@Composable
fun DeleteConfirmationDialog(
    sectionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Section") },
        text = { Text("Are you sure you want to delete section \"$sectionName\"? This action cannot be undone.") },
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