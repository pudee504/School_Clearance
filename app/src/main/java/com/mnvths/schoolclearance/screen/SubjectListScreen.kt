package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert // ✅ Import MoreVert icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.CurriculumSubject
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    navController: NavController,
    viewModel: SubjectViewModel = viewModel(),
    appSettings: AppSettings
) {
    val subjectGroups by viewModel.groupedSubjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<CurriculumSubject?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    var expandedGroupTitle by remember { mutableStateOf<String?>(null) }

    // ✅ State to track which subject's menu is open
    var expandedMenuSubjectId by remember { mutableStateOf<Int?>(null) }


    LaunchedEffect(appSettings) {
        viewModel.fetchGroupedSubjects()
    }

    val filteredGroups = remember(searchQuery, subjectGroups) {
        if (searchQuery.isBlank()) {
            subjectGroups
        } else {
            subjectGroups.mapNotNull { group ->
                val filtered = group.subjects.filter {
                    it.subjectName.contains(searchQuery, ignoreCase = true)
                }
                if (filtered.isNotEmpty()) group.copy(subjects = filtered) else null
            }.also {
                if (it.isNotEmpty()) expandedGroupTitle = it.first().title
            }
        }
    }

    // ✅ Replaced Column with Scaffold for FAB and TopAppBar support
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Subjects") })
        },
        floatingActionButton = {
            // ✅ Replaced the "Add New Subject" button with a FAB
            FloatingActionButton(onClick = { navController.navigate("addEditSubject") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Subject")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Subjects") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredGroups, key = { it.title }) { group ->
                        val isExpanded = expandedGroupTitle == group.title

                        Card(
                            onClick = {
                                expandedGroupTitle = if (isExpanded) null else group.title
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.title,
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

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.subjects.forEach { subject ->
                                    val subjectNameDisplay = if (subject.strandName != null) {
                                        "${subject.subjectName} (${subject.strandName})"
                                    } else {
                                        subject.subjectName
                                    }

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = subjectNameDisplay,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // ✅ START: Dropdown Menu implementation
                                            Box {
                                                IconButton(onClick = { expandedMenuSubjectId = subject.subjectId }) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                                }
                                                DropdownMenu(
                                                    expanded = expandedMenuSubjectId == subject.subjectId,
                                                    onDismissRequest = { expandedMenuSubjectId = null }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit") },
                                                        onClick = {
                                                            navController.navigate("addEditSubject/${subject.subjectId}/${subject.subjectName}")
                                                            expandedMenuSubjectId = null
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            subjectToDelete = subject
                                                            showDeleteDialog = true
                                                            expandedMenuSubjectId = null
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                                                    )
                                                }
                                            }
                                            // ✅ END: Dropdown Menu implementation
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Add space at the bottom so content isn't hidden by FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }


    if (showDeleteDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete '${subjectToDelete?.subjectName}'? This may affect existing assignments.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        subjectToDelete?.let { subject ->
                            viewModel.deleteSubject(
                                id = subject.subjectId,
                                onSuccess = {
                                    Toast.makeText(context, "Subject deleted!", Toast.LENGTH_SHORT).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                }
                            )
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}