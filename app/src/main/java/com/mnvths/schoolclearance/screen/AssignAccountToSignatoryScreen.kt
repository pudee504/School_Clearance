package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    // Filter Logic: Exclude already assigned accounts
    val unassignedAccounts = remember(allAccounts, assignedAccounts) {
        allAccounts.filter { allAcc ->
            assignedAccounts.none { it.accountId == allAcc.id }
        }.sortedBy { it.name }
    }

    // Search Filter
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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Assign Accounts", fontWeight = FontWeight.Bold)
                        Text(
                            text = "to $signatoryName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = SchoolBlue
                )
            )
        },
        containerColor = BackgroundGray,
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedAccounts.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.assignMultipleAccountsToSignatory(
                            signatoryId = signatoryId,
                            accounts = selectedAccounts.toList(),
                            onSuccess = {
                                Toast.makeText(context, "${selectedAccounts.size} accounts assigned!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    containerColor = SchoolBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Check, null) },
                    text = { Text("Assign (${selectedAccounts.size})") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar Area
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search accounts (e.g. Tuition, Library)") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = SchoolRed)
                }
            } else if (filteredAccounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AccountBalance, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "All accounts are already assigned." else "No accounts found.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Available Accounts",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(filteredAccounts, key = { it.id }) { account ->
                        val isSelected = selectedAccounts.contains(account)
                        SelectableAccountCard(
                            account = account,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedAccounts.remove(account)
                                else selectedAccounts.add(account)
                            }
                        )
                    }
                    // Space for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// Helper Component for Account Selection
@Composable
fun SelectableAccountCard(
    account: Account,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) SchoolBlue.copy(alpha = 0.05f) else Color.White)
    val borderColor by animateColorAsState(if (isSelected) SchoolBlue else Color.Transparent)

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Icon
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = if (isSelected) SchoolBlue else Color.Gray
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) SchoolBlue else Color.Black
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SchoolBlue,
                    uncheckedColor = Color.Gray
                )
            )
        }
    }
}