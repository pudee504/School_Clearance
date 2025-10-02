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
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel
import kotlin.collections.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignAccountToSignatoryScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val allAccounts by viewModel.accounts
    val assignedAccounts by viewModel.assignedAccounts
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    val selectedAccounts = remember { mutableStateListOf<Account>() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadAccountAssignmentData(signatoryId)
    }

    val unassignedAccounts = remember(allAccounts, assignedAccounts) {
        allAccounts.filter { allAcc ->
            assignedAccounts.none { it.accountId == allAcc.id }
        }.sortedBy { it.name }
    }

    val filteredAccounts = remember(searchQuery, unassignedAccounts) {
        if (searchQuery.isBlank()) {
            unassignedAccounts
        } else {
            unassignedAccounts.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Accounts to $signatoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedAccounts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.assignMultipleAccountsToSignatory(
                            signatoryId = signatoryId,
                            accounts = selectedAccounts.toList(),
                            onSuccess = {
                                Toast.makeText(context, "Accounts assigned!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Assign Selected Accounts")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Accounts") },
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredAccounts, key = { it.id }) { account ->
                            val isSelected = selectedAccounts.contains(account)
                            Card(
                                onClick = {
                                    if (isSelected) selectedAccounts.remove(account)
                                    else selectedAccounts.add(account)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = account.name,
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