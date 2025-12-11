package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

// Theme Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    val filteredSignatoryList = remember(signatoryList, searchQuery) {
        signatoryList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.firstName.contains(searchQuery, ignoreCase = true) ||
                    it.lastName.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.name }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Signatories", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addSignatory") },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Signatory")
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Control Panel (Search) ---
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Name") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            // --- List Content ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = SchoolRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unable to load data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredSignatoryList.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (signatoryList.isEmpty()) Icons.Outlined.SupervisorAccount else Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (signatoryList.isEmpty()) "No signatories added yet" else "No matches found",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSignatoryList, key = { it.id }) { signatory ->
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
                    item { Spacer(modifier = Modifier.height(64.dp)) } // FAB spacing
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
            icon = { Icon(Icons.Outlined.Delete, null, tint = SchoolRed) },
            title = { Text(text = "Delete Signatory?") },
            text = { Text(text = "Are you sure you want to delete ${signatoryToDelete?.name}? This account will no longer be able to sign clearances.") },
            confirmButton = {
                Button(
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
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Delete")
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
            },
            containerColor = Color.White
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val initials = if (signatory.firstName.isNotEmpty() && signatory.lastName.isNotEmpty()) {
                    "${signatory.firstName.first()}${signatory.lastName.first()}"
                } else {
                    signatory.name.take(2).uppercase()
                }

                Text(
                    text = initials,
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = signatory.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Display username or a placeholder if available
                Text(
                    text = signatory.username ?: "Staff Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = SchoolRed) },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = SchoolRed) }
                    )
                }
            }
        }
    }
}