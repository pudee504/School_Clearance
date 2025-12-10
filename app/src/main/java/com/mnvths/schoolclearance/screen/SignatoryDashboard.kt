package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.viewmodel.AuthViewModel
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatoryDashboard(
    user: OtherUser,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val innerNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showPasswordDialog by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                // ✅ CALL THE FUNCTION FROM AUTHVIEWMODEL
                authViewModel.changePassword(
                    userId = user.id,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    onSuccess = {
                        Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                        showPasswordDialog = false
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SignatoryDrawerContent(
                user = user,
                onChangePasswordClick = {
                    scope.launch { drawerState.close() }
                    showPasswordDialog = true
                },
                onSignOutClick = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                },
                onReportsClick = {
                    scope.launch { drawerState.close() }
                    innerNavController.navigate("reports")
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Signatory Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                SignatoryNavHost(
                    innerNavController = innerNavController,
                    user = user,
                )
            }
        }
    }
}

@Composable
private fun SignatoryNavHost(
    innerNavController: NavHostController,
    user: OtherUser
) {
    NavHost(
        navController = innerNavController,
        startDestination = "assignments_overview"
    ) {
        composable("assignments_overview") {
            AssignmentsOverviewScreen(
                navController = innerNavController,
                user = user
            )
        }

        composable(
            route = "assigned_sections_subject/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignedSectionsScreen(
                navController = innerNavController,
                signatoryId = user.id,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: "",
                destinationRoute = "clearance_subject",
                showFab = false
            )
        }

        composable(
            route = "assigned_sections_account/{accountId}/{accountName}",
            arguments = listOf(
                navArgument("accountId") { type = NavType.IntType },
                navArgument("accountName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignedSectionsForAccountScreen(
                navController = innerNavController,
                signatoryId = user.id,
                accountId = backStackEntry.arguments?.getInt("accountId") ?: 0,
                accountName = backStackEntry.arguments?.getString("accountName") ?: "",
                destinationRoute = "clearance_account",
                showFab = false
            )
        }

        composable(
            route = "clearance_subject/{sectionId}/{subjectId}/{gradeLevel}/{sectionName}/{subjectName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ClearanceScreen(
                navController = innerNavController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0,
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                subjectName = backStackEntry.arguments?.getString("subjectName") ?: "",
                isAccountClearance = false,
                showFab = false
            )
        }

        composable(
            route = "clearance_account/{sectionId}/{accountId}/{gradeLevel}/{sectionName}/{accountName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.IntType },
                navArgument("accountId") { type = NavType.IntType },
                navArgument("gradeLevel") { type = NavType.StringType },
                navArgument("sectionName") { type = NavType.StringType },
                navArgument("accountName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ClearanceScreen(
                navController = innerNavController,
                sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0,
                subjectId = backStackEntry.arguments?.getInt("accountId") ?: 0,
                gradeLevel = backStackEntry.arguments?.getString("gradeLevel") ?: "",
                sectionName = backStackEntry.arguments?.getString("sectionName") ?: "",
                subjectName = backStackEntry.arguments?.getString("accountName") ?: "",
                isAccountClearance = true,
                showFab = false
            )
        }
        composable("reports") {
            ReportsScreen(navController = innerNavController)
        }
    }
}


@Composable
private fun AssignmentsOverviewScreen(
    navController: NavHostController,
    user: OtherUser,
    viewModel: SignatoryViewModel = viewModel()
) {
    val assignedSubjects by viewModel.assignedSubjects
    val assignedAccounts by viewModel.assignedAccounts
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(user.id) {
        viewModel.fetchAssignedSubjects(user.id)
        viewModel.fetchAssignedAccounts(user.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(user.name, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Username: ${user.username ?: "N/A"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        text = "Assigned Subjects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (assignedSubjects.isEmpty()) {
                    item { Text("No subjects assigned.", modifier = Modifier.padding(bottom = 16.dp)) }
                } else {
                    items(assignedSubjects) { subject ->
                        AssignmentListItem(
                            name = subject.subjectName,
                            onItemClicked = {
                                navController.navigate("assigned_sections_subject/${subject.subjectId}/${subject.subjectName}")
                            }
                        )
                    }
                }

                item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    Text(
                        text = "Assigned Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (assignedAccounts.isEmpty()) {
                    item { Text("No accounts assigned.") }
                } else {
                    items(assignedAccounts) { account ->
                        AssignmentListItem(
                            name = account.accountName,
                            onItemClicked = {
                                navController.navigate("assigned_sections_account/${account.accountId}/${account.accountName}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentListItem(
    name: String,
    onItemClicked: () -> Unit
) {
    Card(
        onClick = onItemClicked,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SignatoryDrawerContent(
    user: OtherUser,
    onChangePasswordClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onReportsClick: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.malasila),
                contentDescription = "School Logo",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp)
            )
            Text(text = "Welcome,", style = MaterialTheme.typography.titleMedium)
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Divider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Key, contentDescription = "Change Password") },
            label = { Text("Change Password") },
            selected = false,
            onClick = onChangePasswordClick
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Reports") },
            label = { Text("Reports") },
            selected = false,
            onClick = onReportsClick
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = onSignOutClick
        )
    }
}
