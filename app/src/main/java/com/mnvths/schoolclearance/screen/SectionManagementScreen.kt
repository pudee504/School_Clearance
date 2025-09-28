package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel
// ✅ ADD THIS IMPORT for the refresh logic
import androidx.navigation.compose.currentBackStackEntryAsState

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionManagementScreen(
    // ✅ MODIFIED: Now accepts the root NavController for full-screen navigation
    rootNavController: NavController,
    viewModel: SectionManagementViewModel = viewModel()
) {
    // ✅ START: This block fixes the disappearing sections bug by refreshing the data
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // When we return to this screen, the back stack changes, triggering this refresh
        viewModel.fetchClassSections()
    }
    // ✅ END: Refresh logic

    val sections by viewModel.classSections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<ClassSection?>(null) }

    var expandedGradeLevel by remember { mutableStateOf<String?>(null) }

    val groupedSections = remember(sections) {
        sections.groupBy { it.gradeLevel }
            .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                // ✅ Use the rootNavController to navigate to the full-screen page
                onClick = { rootNavController.navigate("addSection") },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Section")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
                    groupedSections.forEach { (gradeLevel, sectionsInGroup) ->
                        val isExpanded = expandedGradeLevel == gradeLevel

                        item {
                            Card(
                                onClick = {
                                    expandedGradeLevel = if (isExpanded) null else gradeLevel
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = gradeLevel,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        modifier = Modifier.rotate(rotationAngle)
                                    )
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sectionsInGroup.sortedBy { it.sectionName }.forEach { section ->
                                        SectionItem(
                                            section = section,
                                            onEdit = {
                                                // ✅ Use the rootNavController for full-screen navigation
                                                rootNavController.navigate("editSection/${section.sectionId}/${section.gradeLevel}/${section.sectionName}")
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
                    }
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
                        onSuccess = {
                            Toast.makeText(context, "Section deleted successfully.", Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.sectionName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Box {
                var menuExpanded by remember { mutableStateOf(false) }

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
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    )
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