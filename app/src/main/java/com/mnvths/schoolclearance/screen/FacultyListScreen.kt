package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.FacultyMember
import com.mnvths.schoolclearance.viewmodel.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyListScreen(
    viewModel: FacultyViewModel = viewModel(),
    navController: NavController
) {
    val facultyList by viewModel.facultyList
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    // State for the search query
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var facultyToDelete by remember { mutableStateOf<FacultyMember?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchFacultyList()
    }

    // Filter the list based on the search query
    val filteredFacultyList = facultyList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.lastName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // "Add New Faculty" Button
        Button(
            onClick = { navController.navigate("addFaculty") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add New Faculty")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Faculty")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Faculty") },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Faculty list (conditional on data state)
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $error\nPlease check your server and network connection.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFacultyList) { faculty ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val middleName = faculty.middleName ?: "null"
                                        val username = faculty.username ?: "null"
                                        navController.navigate("facultyDetails/${faculty.id}/${faculty.name}/${faculty.firstName}/${faculty.lastName}/${middleName}/${username}")
                                    }
                            ) {
                                Text(text = "ID: ${faculty.id}", style = MaterialTheme.typography.bodySmall)
                                Text(text = faculty.name, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // Action buttons: Edit and Delete
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val middleName = faculty.middleName ?: "null"
                                    val username = faculty.username ?: "null"
                                    navController.navigate("editFaculty/${faculty.id}/${faculty.name}/${faculty.firstName}/${faculty.lastName}/${middleName}/${username}")
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Faculty")
                                }
                                IconButton(onClick = {
                                    facultyToDelete = faculty
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Faculty", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && facultyToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                facultyToDelete = null
            },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete ${facultyToDelete?.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        facultyToDelete?.let { faculty ->
                            viewModel.deleteFaculty(
                                id = faculty.id,
                                onSuccess = {
                                    Toast.makeText(context, "Faculty deleted successfully!", Toast.LENGTH_SHORT).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                    facultyToDelete = null
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                    facultyToDelete = null
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
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        facultyToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}