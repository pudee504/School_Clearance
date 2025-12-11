package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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
                CenterAlignedTopAppBar(
                    title = { Text("Workspace", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SchoolBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = BackgroundGray
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
        // --- Welcome Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SchoolBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.malasila),
                        contentDescription = "Logo",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Welcome Back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SchoolBlue)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: $error",
                    color = SchoolRed,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // --- Subjects ---
                if (assignedSubjects.isNotEmpty()) {
                    item { DashboardSectionHeader("Academic Duties") }
                    items(assignedSubjects) { subject ->
                        DashboardDutyCard(
                            name = subject.subjectName,
                            type = "Subject",
                            icon = Icons.Outlined.Book,
                            onClick = {
                                navController.navigate("assigned_sections_subject/${subject.subjectId}/${subject.subjectName}")
                            }
                        )
                    }
                }

                // --- Accounts ---
                if (assignedAccounts.isNotEmpty()) {
                    item { DashboardSectionHeader("Financial/Admin Duties") }
                    items(assignedAccounts) { account ->
                        DashboardDutyCard(
                            name = account.accountName,
                            type = "Account",
                            icon = Icons.Outlined.AccountBalance,
                            onClick = {
                                navController.navigate("assigned_sections_account/${account.accountId}/${account.accountName}")
                            }
                        )
                    }
                }

                // Empty State
                if (assignedSubjects.isEmpty() && assignedAccounts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No duties assigned yet.", color = Color.Gray)
                            Text("Contact the administrator.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

// --- Dashboard Components ---

@Composable
private fun DashboardSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun DashboardDutyCard(
    name: String,
    type: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SchoolBlue)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage $type Clearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
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
    ModalDrawerSheet(drawerContainerColor = Color.White) {
        // Custom Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SchoolBlue)
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.malasila),
                        contentDescription = "School Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome,",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Key, null, tint = SchoolBlue) },
            label = { Text("Change Password") },
            selected = false,
            onClick = onChangePasswordClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ReceiptLong, null, tint = SchoolBlue) },
            label = { Text("View Reports") },
            selected = false,
            onClick = onReportsClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, null, tint = SchoolRed) },
            label = { Text("Logout", color = SchoolRed) },
            selected = false,
            onClick = onSignOutClick,
            modifier = Modifier.padding(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}