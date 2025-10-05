// file: screen/AccountListScreen.kt

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Account


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    navController: NavController,
    viewModel: AccountViewModel = viewModel()
) {
    val accounts by viewModel.accounts
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    // ✅ ADDED: State for managing dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // ✅ ADDED: Scaffold for FAB and TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Accounts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(accounts, key = { it.id }) { account ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                // ✅ ADDED: Edit/Delete options menu
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
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
                                                accountToEdit = account
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                accountToDelete = account
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
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

    // ✅ ADDED: Dialog for adding a new account
    if (showAddDialog) {
        var newAccountName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("Add New Account") },
            text = {
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.addAccount(
                            name = newAccountName,
                            onSuccess = {
                                Toast.makeText(context, "Account added!", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                showAddDialog = false
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = !isSubmitting && newAccountName.isNotBlank()
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ ADDED: Dialog for editing an existing account
    accountToEdit?.let { account ->
        var updatedName by remember { mutableStateOf(account.name) }
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) accountToEdit = null },
            title = { Text("Edit Account") },
            text = {
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.updateAccount(
                            id = account.id,
                            newName = updatedName,
                            onSuccess = {
                                Toast.makeText(context, "Account updated!", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                accountToEdit = null
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = !isSubmitting && updatedName.isNotBlank() && updatedName != account.name
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) accountToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ ADDED: Dialog for confirming deletion
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) accountToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete the account '${account.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.deleteAccount(
                            id = account.id,
                            onSuccess = {
                                Toast.makeText(context, "Account deleted.", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                accountToDelete = null
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}