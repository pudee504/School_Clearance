package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubjectToSignatoryScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val allSubjects by viewModel.subjects
    val assignedSubjects by viewModel.assignedSubjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    val selectedSubjects = remember { mutableStateListOf<Subject>() }
    // ✅ ADD state for the search query
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchSubjects()
        viewModel.fetchAssignedSubjectsForSignatory(signatoryId)
    }

    // First, get the list of subjects that are not yet assigned
    val unassignedSubjects = remember(allSubjects, assignedSubjects) {
        allSubjects.filter { allSub ->
            assignedSubjects.none { it.subjectId == allSub.id }
        }.sortedBy { it.name }
    }

    // ✅ THEN, filter that list based on the search query
    val filteredSubjects = remember(searchQuery, unassignedSubjects) {
        if (searchQuery.isBlank()) {
            unassignedSubjects
        } else {
            unassignedSubjects.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Subjects to $signatoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedSubjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.assignMultipleSubjectsToSignatory(
                            signatoryId = signatoryId,
                            subjects = selectedSubjects.toList(),
                            onSuccess = {
                                Toast.makeText(context, "Subjects assigned!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Assign Selected Subjects")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ✅ ADD the search bar UI
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Subjects") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text("Error: $error")
                else -> {
                    // ✅ UPDATE the LazyColumn to use the filtered list
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSubjects, key = { it.id }) { subject ->
                            val isSelected = selectedSubjects.contains(subject)
                            Card(
                                onClick = {
                                    if (isSelected) selectedSubjects.remove(subject)
                                    else selectedSubjects.add(subject)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}