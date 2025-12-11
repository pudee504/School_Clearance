// file: screen/AccountListScreen.kt
package com.mnvths.schoolclearance.screen

import AccountViewModel
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Account


// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Initial Fetch
    LaunchedEffect(Unit) {
        viewModel.fetchAccounts()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Accounts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Account") }
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SchoolBlue)
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error loading accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolRed
                    )
                    Text(
                        text = error ?: "Unknown Error",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (accounts.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.AccountBalance, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No accounts created yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // Space for FAB
                ) {
                    items(accounts, key = { it.id }) { account ->
                        AccountItemCard(
                            account = account,
                            onEdit = { accountToEdit = account },
                            onDelete = { accountToDelete = account }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

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
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.addAccount(
                            name = newAccountName.trim(),
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
                    enabled = !isSubmitting && newAccountName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }

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
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.updateAccount(
                            id = account.id,
                            newName = updatedName.trim(),
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
                    enabled = !isSubmitting && updatedName.isNotBlank() && updatedName != account.name,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) accountToEdit = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) accountToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, null, tint = SchoolRed) },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to delete '${account.name}'? This may affect student clearances linked to this account.") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) accountToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }
}

// Helper Component
@Composable
fun AccountItemCard(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountBalance, null, tint = SchoolBlue)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

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
                        text = { Text("Edit") },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = SchoolRed) },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = SchoolRed) }
                    )
                }
            }
        }
    }
}