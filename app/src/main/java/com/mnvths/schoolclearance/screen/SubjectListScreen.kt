package com.mnvths.schoolclearance.screen

// ✅ START: Add all of these required imports at the top of your file
import android.widget.Toast
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
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel
// ✅ END: Required imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    navController: NavController,
    viewModel: SubjectViewModel = viewModel()
) {
    val subjects by viewModel.subjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSubjects()
    }

    val filteredSubjectList = subjects.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.navigate("addEditSubject") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add New Subject")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Subject")
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Subjects") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
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
                items(filteredSubjectList) { subject ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    navController.navigate("addEditSubject/${subject.id}/${subject.name}")
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Subject")
                                }
                                IconButton(onClick = {
                                    subjectToDelete = subject
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Subject", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete '${subjectToDelete?.name}'? This may affect existing assignments.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        subjectToDelete?.let { subject ->
                            viewModel.deleteSubject(
                                id = subject.id,
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