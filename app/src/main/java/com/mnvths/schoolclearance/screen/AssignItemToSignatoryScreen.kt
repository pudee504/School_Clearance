package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AssignItemToSignatoryScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allSubjects by viewModel.subjects
    val allAccounts by viewModel.accounts

    val selectedItems = remember { mutableStateMapOf<String, Pair<Int, String>>() }

    // ✅ State for the search query
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchSubjects()
        viewModel.fetchAccounts()
    }

    // ✅ Filter lists based on the search query
    val filteredSubjects = remember(searchQuery, allSubjects) {
        if (searchQuery.isBlank()) {
            allSubjects
        } else {
            allSubjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val filteredAccounts = remember(searchQuery, allAccounts) {
        if (searchQuery.isBlank()) {
            allAccounts
        } else {
            allAccounts.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign to $signatoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedItems.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            var successCount = 0
                            val totalItems = selectedItems.size

                            selectedItems.forEach { (_, itemPair) ->
                                val (itemId, itemType) = itemPair
                                viewModel.assignItemToSignatory(
                                    signatoryId = signatoryId,
                                    itemId = itemId,
                                    itemType = itemType,
                                    onSuccess = {
                                        successCount++
                                        if (successCount == totalItems) {
                                            Toast.makeText(context, "All items assigned!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, "Error assigning $itemType: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Assign Selected Items")
                }
            }
        }
    ) { paddingValues ->
        // ✅ Wrap the LazyColumn in a Column to hold the search bar
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // ✅ Add the search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stickyHeader {
                    ListHeader("Subjects")
                }
                // ✅ Use the filtered list
                items(filteredSubjects, key = { "subject-${it.id}" }) { subject ->
                    val key = "subject-${subject.id}"
                    val isSelected = selectedItems.containsKey(key)
                    ItemCard(
                        name = subject.name,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) selectedItems.remove(key)
                            else selectedItems[key] = Pair(subject.id, "Subject")
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                stickyHeader {
                    ListHeader("Accounts")
                }
                // ✅ Use the filtered list
                items(filteredAccounts, key = { "account-${it.id}" }) { account ->
                    val key = "account-${account.id}"
                    val isSelected = selectedItems.containsKey(key)
                    ItemCard(
                        name = account.name,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) selectedItems.remove(key)
                            else selectedItems[key] = Pair(account.id, "Account")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun ItemCard(name: String, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(checked = isSelected, onCheckedChange = null)
        }
    }
}