package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionManagementScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    // ✅ FIXED: Renamed fetchSections() to fetchClassSections()
    LaunchedEffect(Unit) {
        viewModel.fetchClassSections()
    }

    // ✅ FIXED: Renamed viewModel.sections to viewModel.classSections
    val sections by viewModel.classSections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<ClassSection?>(null) }

    val sortedSections = remember(sections) {
        sections.sortedWith(
            compareBy(
                { it.gradeLevel.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 },
                { it.sectionName }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                items(sortedSections, key = { it.sectionId }) { section ->
                    SectionItem(
                        section = section,
                        onEdit = {
                            navController.navigate("editSection/${section.sectionId}/${section.gradeLevel}/${section.sectionName}")
                        },
                        onDelete = {
                            sectionToDelete = section
                            showDeleteConfirmationDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            sectionName = sectionToDelete?.sectionName ?: "",
            onConfirm = {
                sectionToDelete?.let {
                    viewModel.deleteSection(
                        sectionId = it.sectionId,
                        onSuccess = { /* Handle success */ },
                        onError = { /* Handle error */ }
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
fun SectionItem(
    section: ClassSection,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${section.gradeLevel} - ${section.sectionName}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Section")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Section")
                }
            }
        }
    }
}

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
            ) { Text("Delete") }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}