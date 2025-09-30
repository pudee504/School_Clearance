package com.mnvths.schoolclearance.screen


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert // ✅ Import for the "..." icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.AssignedItem
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatoryDetailsScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    username: String,
    viewModel: SignatoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val assignedItems by viewModel.assignedItems
    val isLoading by viewModel.isLoading

    var showDialog by remember { mutableStateOf(false) }
    var itemToUnassign by remember { mutableStateOf<AssignedItem?>(null) }

    // ✅ State to manage which item's menu is expanded
    var expandedMenuId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(key1 = signatoryId) {
        viewModel.fetchAssignedItems(signatoryId)
    }

    if (showDialog && itemToUnassign != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Unassignment") },
            text = { Text("Are you sure you want to unassign '${itemToUnassign!!.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unassignItem(
                            assignmentId = itemToUnassign!!.assignmentId,
                            onSuccess = {
                                Toast.makeText(context, "Item unassigned", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                        showDialog = false
                        itemToUnassign = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signatory Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("assignItemToSignatory/$signatoryId/$signatoryName")
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Assign New Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(signatoryName, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Assigned Items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (assignedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items have been assigned yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(assignedItems, key = { it.assignmentId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = item.type,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // ✅ IconButton is now wrapped in a Box to anchor the menu
                                Box {
                                    IconButton(onClick = { expandedMenuId = item.assignmentId }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options"
                                        )
                                    }

                                    // ✅ DropdownMenu that shows when the icon is clicked
                                    DropdownMenu(
                                        expanded = expandedMenuId == item.assignmentId,
                                        onDismissRequest = { expandedMenuId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Unassign") },
                                            onClick = {
                                                itemToUnassign = item
                                                showDialog = true
                                                expandedMenuId = null // Close the menu
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
}