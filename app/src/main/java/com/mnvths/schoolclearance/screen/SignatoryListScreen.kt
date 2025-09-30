package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatoryListScreen(
    viewModel: SignatoryViewModel = viewModel(),
    navController: NavController
) {
    val signatoryList by viewModel.signatoryList
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var signatoryToDelete by remember { mutableStateOf<Signatory?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSignatoryList()
    }

    val filteredSignatoryList = signatoryList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.lastName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Signatories") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addSignatory") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Signatory")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Signatories") },
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
                    items(filteredSignatoryList) { signatory ->
                        SignatoryListItem(
                            signatory = signatory,
                            onViewDetails = {
                                val middleName = signatory.middleName ?: "null"
                                val username = signatory.username ?: "null"
                                navController.navigate("signatoryDetails/${signatory.id}/${signatory.name}/${signatory.firstName}/${signatory.lastName}/${middleName}/${username}")
                            },
                            onEdit = {
                                val middleName = signatory.middleName ?: "null"
                                val username = signatory.username ?: "null"
                                navController.navigate("editSignatory/${signatory.id}/${signatory.name}/${signatory.firstName}/${signatory.lastName}/${middleName}/${username}")
                            },
                            onDelete = {
                                signatoryToDelete = signatory
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && signatoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                signatoryToDelete = null
            },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete ${signatoryToDelete?.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        signatoryToDelete?.let { signatory ->
                            viewModel.deleteSignatory(
                                id = signatory.id,
                                onSuccess = {
                                    Toast.makeText(context, "Signatory deleted successfully!", Toast.LENGTH_SHORT).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                    signatoryToDelete = null
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    isDeleting = false
                                    showDeleteDialog = false
                                    signatoryToDelete = null
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
                        signatoryToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SignatoryListItem(
    signatory: Signatory,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewDetails)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = signatory.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
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
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}